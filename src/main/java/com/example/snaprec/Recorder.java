package com.example.snaprec;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.ImageIO;


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
    private final double zoomScale = 1.2;  // 放大倍率
    private volatile ZoomEffect zoomEffect = null;
    private final List<ClickEffect> clickEffects = new ArrayList<>();


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

    public void addClickEffect(Point point) {
        synchronized (clickEffects) {
            clickEffects.add(new ClickEffect(point));
        }
    }


    private long videoTimestamp = 0;
    private final long timestampIncrementMicros = 1_000_000L / targetFPS;

    private void captureAndRecord() {
        try {
            BufferedImage screen = robot.createScreenCapture(screenRect);
            ZoomEffect effect = zoomEffect; // 用區域變數保留，避免在中間變成 null
            if (effect != null) {
                if (effect.isExpired()) {
                    zoomEffect = null;
                } else {
                    double scale = effect.getCurrentScale();
                    int zoomW = (int)(screen.getWidth() * scale);
                    int zoomH = (int)(screen.getHeight() * scale);

                    BufferedImage zoomed = new BufferedImage(zoomW, zoomH, BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D g = zoomed.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(screen, 0, 0, zoomW, zoomH, null);
                    g.dispose();

                    // 計算滑鼠在原圖的位置，對應到放大圖的位置
                    int mouseX = effect.center.x;
                    int mouseY = effect.center.y;

                    // 放大後，滑鼠的點會對應到 zoomEffect.center * scale
                    int centerX = (int)(mouseX * scale);
                    int centerY = (int)(mouseY * scale);

                    // 我們要讓這個點在畫面中間，所以裁切畫面：
                    int cropX = centerX - screenRect.width / 2;
                    int cropY = centerY - screenRect.height / 2;

                    // 防止超出邊界
                    cropX = Math.max(0, Math.min(cropX, zoomed.getWidth() - screenRect.width));
                    cropY = Math.max(0, Math.min(cropY, zoomed.getHeight() - screenRect.height));

                    screen = zoomed.getSubimage(cropX, cropY, screenRect.width, screenRect.height);
                }
            }

            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            Point mouseLocation = pointerInfo.getLocation();

            Graphics2D g = screen.createGraphics();
            BufferedImage cursorImage = ImageIO.read(new File("src/cursorImageRepository/cursor-mouse-svg-icon-free-download-windows-10-cursor-icon-triangle-symbol-transparent-png-1038697.png"));
            g.drawImage(cursorImage, mouseLocation.x, mouseLocation.y, null);

            // 顯示點擊特效
            synchronized (clickEffects) {
                clickEffects.removeIf(ClickEffect::isExpired);
                for (ClickEffect clickeffect : clickEffects) {
                    double progress = clickeffect.getProgress();  // 0 ~ 1
                    float alpha = (float) (1.0 - progress);
                    int radius = (int) (30 + 40 * progress);

                    g.setColor(new Color(1.0f, 0f, 0f, alpha)); // 紅色，逐漸淡出
                    g.setStroke(new BasicStroke(3));
                    g.drawOval(clickeffect.location.x - radius / 2, clickeffect.location.y - radius / 2, radius, radius);
                }
            }
            g.dispose();


            BufferedImage formatted = new BufferedImage(screen.getWidth(), screen.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            g = formatted.createGraphics();
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

    private class ClickEffect {
        final Point location;
        final long timestamp; // 開始時間

        ClickEffect(Point location) {
            this.location = location;
            this.timestamp = System.nanoTime();
        }

        boolean isExpired() {
            return System.nanoTime() - timestamp > 500_000_000L; // 0.5 秒
        }

        double getProgress() {
            return (System.nanoTime() - timestamp) / 500_000_000.0;
        }
    }

    private class ZoomEffect {
        final Point center;
        final long startTime;
        final long durationExpand = 600_000_000L; // 放大動畫 0.5 秒
        final long durationHold = 3_000_000_000L;  // 停留 3 秒
        final long durationShrink = 600_000_000L; // 縮回動畫 0.5 秒
        final long totalDuration = durationExpand + durationHold + durationShrink;
        final double zoomScale = 1.5;  // 假設這裡設置放大倍率為 1.5

        public ZoomEffect(Point center) {
            this.center = center;
            this.startTime = System.nanoTime();
        }

        // 平滑插值（Ease In and Out）
        private double easeInOut(double t) {
            return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }

        public double getCurrentScale() {
            long elapsed = System.nanoTime() - startTime;

            if (elapsed <= durationExpand) {
                // 放大中：1.0 → zoomScale
                double progress = (double) elapsed / durationExpand;
                progress = easeInOut(progress);  // 加入平滑插值
                return 1.0 + (zoomScale - 1.0) * progress;
            } else if (elapsed <= durationExpand + durationHold) {
                // 保持最大倍率
                return zoomScale;
            } else if (elapsed <= totalDuration) {
                // 縮回中：zoomScale → 1.0
                double shrinkElapsed = elapsed - durationExpand - durationHold;
                double progress = shrinkElapsed / (double) durationShrink;
                progress = easeInOut(progress);  // 加入平滑插值
                return zoomScale - (zoomScale - 1.0) * progress;
            } else {
                // 已經結束
                return 1.0;
            }
        }

        public boolean isExpired() {
            return System.nanoTime() - startTime > totalDuration;
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
