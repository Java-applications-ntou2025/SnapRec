package com.example.snaprec;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recorder extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    private final Robot robot;
    private final FFmpegFrameRecorder recorder;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    private final int targetFPS = 15;
    private final long frameIntervalNanos = 1_000_000_000L / targetFPS;
    private final long timestampIncrementMicros = 1_000_000L / targetFPS;

    private final BufferedImage backgroundImage;
    private final int outputWidth;
    private final int outputHeight;

    private long videoTimestamp = 0;

    private final BufferedImage cursorImage;

    private volatile ZoomEffect zoomEffect = null;
    private final List<ClickEffect> clickEffects = new ArrayList<>();

    // 影格佇列
    private final BlockingQueue<BufferedImage> frameQueue = new ArrayBlockingQueue<>(10);

    private Thread captureThread;

    public Recorder(String filename, boolean useGpuEncoding, String CursorimagePath, String backgroundImagePath) throws Exception {
        robot = new Robot();
        backgroundImage = ImageIO.read(new File(backgroundImagePath));
        outputWidth = screenRect.width;
        outputHeight = screenRect.height;

        recorder = new FFmpegFrameRecorder(filename, outputWidth, outputHeight);

        if (useGpuEncoding) {
            // 使用 NVIDIA NVENC GPU 編碼
            recorder.setVideoCodecName("h264_nvenc");
            recorder.setVideoOption("preset", "p4");   // p1 = 最快, p7 = 最佳畫質（取決於硬體）
            recorder.setVideoOption("rc", "vbr");      // 可改成 cbr, cq, 等依需求調整
            recorder.setVideoBitrate(12_000_000);      // 設定適中位元率
            System.out.println("⚙️ 使用 GPU 編碼器：h264_nvenc");
        } else {
            // 傳統 CPU 編碼
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // libx264
            recorder.setVideoOption("preset", "slow");         // 影像較清晰但編碼較慢
            recorder.setVideoOption("crf", "18");              // 畫質與壓縮比平衡
            recorder.setVideoBitrate(12_000_000);
            System.out.println("⚙️ 使用 CPU 編碼器：libx264");
        }

        recorder.setFormat("mp4");
        recorder.setFrameRate(targetFPS);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        // 音訊設定（可略過或移除）
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setAudioChannels(1);

        cursorImage = ImageIO.read(new File(CursorimagePath));

    }


    public void setZoomCenter(Point point) {
        this.zoomEffect = new ZoomEffect(point);
    }

    public void addClickEffect(Point point) {
        synchronized (clickEffects) {
            clickEffects.add(new ClickEffect(point));
        }
    }

    @Override
    public void run() {
        try {
            System.out.println("Recorder: 開始錄影...");
            recorder.start();
            running.set(true);

            // 擷取畫面執行緒
            captureThread = new Thread(() -> {
                long nextCaptureTime = System.nanoTime();
                while (running.get()) {
                    try {
                        long now = System.nanoTime();
                        if (now >= nextCaptureTime) {
                            BufferedImage screen = robot.createScreenCapture(screenRect);
                            frameQueue.put(screen);
                            nextCaptureTime += frameIntervalNanos;
                        } else {
                            long sleepMs = (nextCaptureTime - now) / 1_000_000;
                            if (sleepMs > 0) Thread.sleep(sleepMs);
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            captureThread.start();

            // 主執行緒做後續合成錄製
            while (running.get()) {
                BufferedImage screen = frameQueue.take(); // 取得最新畫面

                // 合成特效、背景與滑鼠游標
                BufferedImage combinedImage = composeFrame(screen);

                recordFrame(combinedImage);
            }

            // 結束時清理
            captureThread.interrupt();
            captureThread.join();

            recorder.stop();
            recorder.release();

            System.out.println("Recorder: 錄影結束。");

        } catch (Exception e) {
            System.err.println("Recorder: 錄影過程發生錯誤！");
            e.printStackTrace();
        }
    }

    private BufferedImage composeFrame(BufferedImage screen) {
        ZoomEffect effect = null;
        double scale = 1.0;
        int cropX = 0, cropY = 0;

        if (zoomEffect != null && !zoomEffect.isExpired()) {
            effect = zoomEffect;

            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            Point mouseLocation = pointerInfo.getLocation();
            effect.center = mouseLocation;

            scale = effect.getCurrentScale();
            int centerX = (int) (effect.center.x * scale);
            int centerY = (int) (effect.center.y * scale);
            cropX = centerX - screenRect.width / 2 + effect.offsetX;
            cropY = centerY - screenRect.height / 2 + effect.offsetY;

            int zoomW = (int) (screen.getWidth() * scale);
            int zoomH = (int) (screen.getHeight() * scale);
            BufferedImage zoomed = new BufferedImage(zoomW, zoomH, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D gZoom = zoomed.createGraphics();
            gZoom.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gZoom.drawImage(screen, 0, 0, zoomW, zoomH, null);
            gZoom.dispose();

            int borderMargin = 80;
            int scrollSpeed = 5;
            if (effect.center.x < borderMargin) effect.offsetX -= scrollSpeed;
            if (effect.center.x > screenRect.width - borderMargin) effect.offsetX += scrollSpeed;
            if (effect.center.y < borderMargin) effect.offsetY -= scrollSpeed;
            if (effect.center.y > screenRect.height - borderMargin) effect.offsetY += scrollSpeed;

            cropX = Math.max(0, Math.min(cropX, zoomed.getWidth() - screenRect.width));
            cropY = Math.max(0, Math.min(cropY, zoomed.getHeight() - screenRect.height));
            screen = zoomed.getSubimage(cropX, cropY, screenRect.width, screenRect.height);
        } else {
            zoomEffect = null;
        }

        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        Point mouseLocation = pointerInfo.getLocation();

        BufferedImage combinedImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = combinedImage.createGraphics();

        // 背景
        g.drawImage(backgroundImage, 0, 0, outputWidth, outputHeight, null);

        int scaledWidth = (int) (outputWidth * 0.8);
        int scaledHeight = (int) (outputHeight * 0.8);
        int offsetX = (outputWidth - scaledWidth) / 2;
        int offsetY = (outputHeight - scaledHeight) / 2;

        // 繪製截取螢幕畫面
        g.drawImage(screen, offsetX, offsetY, scaledWidth, scaledHeight, null);

        // 點擊特效
        synchronized (clickEffects) {
            clickEffects.removeIf(ClickEffect::isExpired);
            for (ClickEffect clickeffect : clickEffects) {
                double progress = clickeffect.getProgress();
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

        // 游標繪製（含縮放）
        int cursorWidth = cursorImage.getWidth();
        int cursorHeight = cursorImage.getHeight();
        double cursorScale = 0.09;
        int scaledCursorWidth = (int)(cursorWidth * cursorScale);
        int scaledCursorHeight = (int)(cursorHeight * cursorScale);

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

        int drawX = cursorX - scaledCursorWidth / 2;
        int drawY = cursorY - scaledCursorHeight / 2;
        g.drawImage(cursorImage, drawX, drawY, scaledCursorWidth, scaledCursorHeight, null);

        g.dispose();

        return combinedImage;
    }

    private void recordFrame(BufferedImage combinedImage) throws Exception {
        byte[] data = ((DataBufferByte) combinedImage.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(outputHeight, outputWidth, opencv_core.CV_8UC3);
        mat.data().put(data);

        Frame frame = converter.convert(mat);
        recorder.setTimestamp(videoTimestamp);
        recorder.record(frame);
        videoTimestamp += timestampIncrementMicros;
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