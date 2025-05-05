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
            BufferedImage formatted = new BufferedImage(screen.getWidth(), screen.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = formatted.createGraphics();
            g.drawImage(screen, 0, 0, null);
            g.dispose();

            byte[] data = ((DataBufferByte) formatted.getRaster().getDataBuffer()).getData();
            Mat mat = new Mat(screenRect.height, screenRect.width, opencv_core.CV_8UC3);
            mat.data().put(data);

            Frame frame = converter.convert(mat);
            recorder.setTimestamp(videoTimestamp);  // 設置時間戳
            recorder.record(frame);

            videoTimestamp += timestampIncrementMicros;  // 增加時間戳
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        running.set(false);
    }
}
