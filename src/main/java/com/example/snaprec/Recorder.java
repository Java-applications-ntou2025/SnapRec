package com.example.snaprec;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Recorder extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    private final FFmpegFrameRecorder recorder;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

    private final NativeScreenCapture nativeCapture; // ✅ 只初始化一次
    private final int screenWidth = 2880;
    private final int screenHeight = 1800;

    // 設定幀率
    private final int targetFPS = 30;
    private final long frameIntervalNanos = 1_000_000_000L / targetFPS;
    private final long startTime = System.nanoTime();

    public Recorder(String filename) throws Exception {
        nativeCapture = new NativeScreenCapture(); // ✅ 初始化移到建構子中

        recorder = new FFmpegFrameRecorder(filename, screenWidth, screenHeight);

        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(targetFPS);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setVideoBitrate(10_000_000); // 10 Mbp
        recorder.setVideoOption("preset", "ultrafast");
//        recorder.setVideoOption("profile", "baseline");
        recorder.setVideoOption("crf", "18");
//        recorder.setVideoOption("skip_frame", "1");

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

    private void captureAndRecord() {
        try {
            byte[] frameData = nativeCapture.captureFrame();

            int expectedSize = screenWidth * screenHeight * 3;
            if (frameData.length != expectedSize) {
                System.out.printf("錯誤：frameData 長度不符: %d, expectedSize: %d%n", frameData.length, expectedSize);
                return;
            }

            Mat mat = new Mat(screenHeight, screenWidth, opencv_core.CV_8UC3);
            mat.data().put(frameData, 0, frameData.length);

            org.bytedeco.javacv.Frame frame = converter.convert(mat);

            long timestamp = (System.nanoTime() - startTime) / 1000;
            recorder.setTimestamp(timestamp);
            recorder.record(frame);

            mat.release(); // 釋放 native 資源
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        running.set(false);
    }
}
