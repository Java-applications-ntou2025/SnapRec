package com.example.snaprec;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.Frame;

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
    private final int targetFPS = 15;  // 設定為15fps
    private final long frameIntervalNanos = 1_000_000_000L / targetFPS; // 每幀間隔

    // 將背景圖片提前載入（假設檔案路徑正確）
    private final BufferedImage backgroundImage;
    private final int outputWidth;
    private final int outputHeight;

    private long videoTimestamp = 0;
    private final long timestampIncrementMicros = 1_000_000L / targetFPS;
    private final BufferedImage cursorImage = ImageIO.read(new File("src\\cursorImageRepository\\cursor-mouse-svg-icon-free-download-windows-10-cursor-icon-triangle-symbol-transparent-png-1038697.png"));



    private volatile ZoomEffect zoomEffect = null;
    private final List<ClickEffect> clickEffects = new ArrayList<>();


    public Recorder(String filename) throws Exception {
        robot = new Robot();
        // 提前載入背景圖片
        backgroundImage = ImageIO.read(new File("src\\picture\\背景01.jpg"));
        this.outputWidth = screenRect.width;
        this.outputHeight = screenRect.height;

        // 這邊我們以背景圖片的尺寸作為輸出尺寸
        recorder = new FFmpegFrameRecorder(filename, outputWidth, outputHeight);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(targetFPS);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        // 畫質設定
        recorder.setVideoBitrate(12000 * 1000); // 12 Mbps
        recorder.setVideoOption("preset", "slow");
        recorder.setVideoOption("crf", "18");

        // 音訊設定
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setAudioChannels(1);
    }

    public void setZoomCenter(Point point) {
        this.zoomEffect = new ZoomEffect(point);
    }


    @Override
    public void run() {
        try {
            System.out.println("Recorder: 開始錄影...");
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
            System.out.println("Recorder: 錄影結束。");
            File f = new File("output.mp4");
            System.out.println("Recorder: 檔案存在？" + f.exists() + "，大小：" + f.length());
        } catch (Exception e) {
            System.err.println("Recorder: 錄影過程發生錯誤！");
            e.printStackTrace();
        }
    }

    public void addClickEffect(Point point) {
        synchronized (clickEffects) {
            clickEffects.add(new ClickEffect(point));
        }
    }

    private void captureAndRecord() {
        try {
            BufferedImage screen = robot.createScreenCapture(screenRect);

            ZoomEffect effect = null;
            double scale = 1.0;
            int cropX = 0, cropY = 0;

            if (zoomEffect != null && !zoomEffect.isExpired()) {
                effect = zoomEffect;

                // 每次更新中心點為目前滑鼠位置
                PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                Point mouseLocation = pointerInfo.getLocation();
                effect.center = mouseLocation;

                scale = effect.getCurrentScale();
                int centerX = (int) (effect.center.x * scale);
                int centerY = (int) (effect.center.y * scale);
                cropX = centerX - screenRect.width / 2 + effect.offsetX;
                cropY = centerY - screenRect.height / 2 + effect.offsetY;

                // 放大整個畫面
                int zoomW = (int) (screen.getWidth() * scale);
                int zoomH = (int) (screen.getHeight() * scale);
                BufferedImage zoomed = new BufferedImage(zoomW, zoomH, BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D gZoom = zoomed.createGraphics();
                gZoom.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                gZoom.drawImage(screen, 0, 0, zoomW, zoomH, null);
                gZoom.dispose();

                // 邊界捲動
                int borderMargin = 80;
                int scrollSpeed = 5;
                if (effect.center.x < borderMargin) effect.offsetX -= scrollSpeed;
                if (effect.center.x > screenRect.width - borderMargin) effect.offsetX += scrollSpeed;
                if (effect.center.y < borderMargin) effect.offsetY -= scrollSpeed;
                if (effect.center.y > screenRect.height - borderMargin) effect.offsetY += scrollSpeed;

                // 限制裁切範圍
                cropX = Math.max(0, Math.min(cropX, zoomed.getWidth() - screenRect.width));
                cropY = Math.max(0, Math.min(cropY, zoomed.getHeight() - screenRect.height));
                screen = zoomed.getSubimage(cropX, cropY, screenRect.width, screenRect.height);
            } else {
                zoomEffect = null; // 清除過期的放大效果
            }

            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            Point mouseLocation = pointerInfo.getLocation();

            // 建立合成圖（背景 + 畫面 + 特效）
            BufferedImage combinedImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = combinedImage.createGraphics();
            g.drawImage(backgroundImage, 0, 0, outputWidth, outputHeight, null);

            int scaledWidth = (int) (outputWidth * 0.8);
            int scaledHeight = (int) (outputHeight * 0.8);
            int offsetX = (outputWidth - scaledWidth) / 2;
            int offsetY = (outputHeight - scaledHeight) / 2;

            g.drawImage(screen, offsetX, offsetY, scaledWidth, scaledHeight, null);

            // 點擊特效
            synchronized (clickEffects) {
                clickEffects.removeIf(ClickEffect::isExpired);
                for (ClickEffect clickeffect : clickEffects) {
                    double progress = clickeffect.getProgress(); // 0 ~ 1
                    float alpha = (float) (1.0 - progress);
                    int radius = (int) (30 + 40 * progress);

                    int effectX, effectY;

                    if (effect != null) {
                        int relativeX = (int) ((clickeffect.location.x * scale) - cropX);
                        int relativeY = (int) ((clickeffect.location.y * scale) - cropY);
                        effectX = (int) (relativeX * 0.8) + offsetX;
                        effectY = (int) (relativeY * 0.8) + offsetY;
                    } else {
                        effectX = (int) (clickeffect.location.x * 0.8) + offsetX;
                        effectY = (int) (clickeffect.location.y * 0.8) + offsetY;
                    }

                    g.setColor(new Color(1.0f, 0f, 0f, alpha));
                    g.setStroke(new BasicStroke(3));
                    g.drawOval(effectX - radius / 2, effectY - radius / 2, radius, radius);
                }
            }

            // 畫滑鼠游標
            int cursorX, cursorY;
            if (effect != null) {
                int relativeX = (int) ((mouseLocation.x * scale) - cropX);
                int relativeY = (int) ((mouseLocation.y * scale) - cropY);
                cursorX = (int) (relativeX * 0.8) + offsetX;
                cursorY = (int) (relativeY * 0.8) + offsetY;
            } else {
                cursorX = (int) (mouseLocation.x * 0.8) + offsetX;
                cursorY = (int) (mouseLocation.y * 0.8) + offsetY;
            }

            g.drawImage(cursorImage, cursorX, cursorY, null);
            g.dispose();

            // 錄製畫面
            byte[] data = ((DataBufferByte) combinedImage.getRaster().getDataBuffer()).getData();
            Mat mat = new Mat(outputHeight, outputWidth, opencv_core.CV_8UC3);
            mat.data().put(data);

            Frame frame = converter.convert(mat);
            recorder.setTimestamp(videoTimestamp);
            recorder.record(frame);
            videoTimestamp += timestampIncrementMicros;

        } catch (Exception e) {
            System.err.println("Recorder: 擷取或錄製影格時發生錯誤！");
            e.printStackTrace();
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
        System.out.println("Recorder: 收到停止錄影指令。");
    }
}