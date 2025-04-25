package com.example.snaprec;

import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

import java.util.concurrent.atomic.AtomicBoolean;

public class Recorder extends Thread {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final FFmpegFrameGrabber grabber;
    private final FFmpegFrameRecorder recorder;

    private final int targetFPS = 60;
    private final long startTime = System.nanoTime();

    public Recorder(String filename) throws Exception {
        // Windows 專用：使用 FFmpeg gdigrab 擷取整個桌面
        grabber = new FFmpegFrameGrabber("desktop");
        grabber.setFormat("gdigrab"); // Windows 平台
        grabber.setFrameRate(targetFPS);
        grabber.setOption("draw_mouse", "1"); // 抓滑鼠游標
        grabber.start();

        int width = grabber.getImageWidth();
        int height = grabber.getImageHeight();

        recorder = new FFmpegFrameRecorder(filename, width, height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setFrameRate(targetFPS);
        recorder.setVideoBitrate(10_000_000); // 10 Mbps
        recorder.setVideoOption("preset", "slow"); // 可換成 medium 或 slow 根據需求
        recorder.setVideoOption("crf", "23"); // CRF 越低畫質越高
    }

    @Override
    public void run() {
        try {
            recorder.start();
            running.set(true);

            long frameIntervalNanos = 1_000_000_000L / targetFPS;
            long nextFrameTime = System.nanoTime();

            while (running.get()) {
                long now = System.nanoTime();
                if (now >= nextFrameTime) {
                    Frame frame = grabber.grab();
                    if (frame != null) {
                        long timestamp = (System.nanoTime() - startTime) / 1000;
                        recorder.setTimestamp(timestamp);
                        recorder.record(frame);
                    }
                    nextFrameTime += frameIntervalNanos;
                } else {
                    long sleepTime = (nextFrameTime - now) / 1_000_000;
                    Thread.sleep(Math.max(0, sleepTime));
                }
            }

            recorder.stop();
            recorder.release();
            grabber.stop();
            grabber.release();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        running.set(false);
    }
}
