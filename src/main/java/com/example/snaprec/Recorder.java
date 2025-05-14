package com.example.snaprec;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recorder extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    private final Robot robot;
    private final FFmpegFrameRecorder recorder;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    // 設定幀率
    private final int targetFPS = 15;  // 設定為30fps來避免快轉
    private final long frameIntervalNanos = 1_000_000_000L / targetFPS; // 每幀時間

    private volatile Point zoomCenter = null;
    private final int zoomSize = 300;  // 要截取的區域大小
    private final double zoomScale = 0.85;  // 放大倍率
    private volatile ZoomEffect zoomEffect = null;


    public Recorder(String filename) throws Exception {
        robot = new Robot();
        recorder = new FFmpegFrameRecorder(filename, screenRect.width, screenRect.height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(targetFPS);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        // 畫質設定
        recorder.setVideoBitrate(8000 * 1000); // 8 Mbps
        recorder.setVideoOption("preset", "slow");
        recorder.setVideoOption("crf", "18");
    }

    public void setZoomCenter(Point point) {
        this.zoomEffect = new ZoomEffect(point);

        // 3 秒後自動取消放大
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
            zoomEffect = null;
        }).start();
    }


    @Override
    public void run() {
        try {
            recorder.start();
            running.set(true);

            long nextFrameTime = System.nanoTime();

            while (running.get()) {
                long now = System.nanoTime();
                if (now >= nextFrameTime) {
                    captureAndRecord();
                    nextFrameTime += frameIntervalNanos;
                } else {
                    long sleepTime = (nextFrameTime - now) / 1_000_000;
                    Thread.sleep(Math.max(0, sleepTime));
                }
            }

            recorder.stop();
            recorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long videoTimestamp = 0;
    private final long timestampIncrementMicros = 1_000_000L / targetFPS;

    private void captureAndRecord() {
        try {
            BufferedImage screen = robot.createScreenCapture(screenRect);

            if (zoomEffect != null) {
                if (zoomEffect.isExpired()) {
                    zoomEffect = null;
                } else {
                    double currentScale = zoomEffect.getCurrentScale();
                    System.out.println("目前放大倍率: " + currentScale);

                    int size = (int)(zoomSize / currentScale);
                    int x = zoomEffect.center.x - size / 2;
                    int y = zoomEffect.center.y - size / 2;
                    x = Math.max(0, Math.min(x, screen.getWidth() - size));
                    y = Math.max(0, Math.min(y, screen.getHeight() - size));

                    BufferedImage subImage = screen.getSubimage(x, y, size, size);
                    BufferedImage zoomedImage = new BufferedImage(screenRect.width, screenRect.height, BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D g = zoomedImage.createGraphics();
                    g.drawImage(subImage, 0, 0, screenRect.width, screenRect.height, null);
                    g.dispose();
                    screen = zoomedImage;
                }
            }


            BufferedImage formatted = new BufferedImage(screen.getWidth(), screen.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = formatted.createGraphics();
            g.drawImage(screen, 0, 0, null);
            g.dispose();

            byte[] data = ((DataBufferByte) formatted.getRaster().getDataBuffer()).getData();
            Mat mat = new Mat(screenRect.height, screenRect.width, opencv_core.CV_8UC3);
            mat.data().put(data);

            Frame frame = converter.convert(mat);
            recorder.setTimestamp(videoTimestamp);
            recorder.record(frame);

            videoTimestamp += timestampIncrementMicros;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ZoomEffect {
        final Point center;
        final long startTime;
        final long duration = 500_000_000L; // 0.5秒（以 nanosecond 為單位）

        public ZoomEffect(Point center) {
            this.center = center;
            this.startTime = System.nanoTime();
        }

        public double getCurrentScale() {
            long elapsed = System.nanoTime() - startTime;
            if (elapsed >= duration) return zoomScale; // 到達最終倍率
            double progress = (double) elapsed / duration;
            return 1.0 + (zoomScale - 1.0) * progress;
        }

        public boolean isExpired() {
            return System.nanoTime() - startTime > 3_000_000_000L; // 3 秒後結束
        }
    }

    public void shutdownAndWait() {
        stopRecording();
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }




    public void stopRecording() {
        running.set(false);
    }
}
