package com.example.snaprec;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import javax.imageio.ImageIO;
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

    public Recorder(String filename) throws Exception {
        robot = new Robot();
        // 提前載入背景圖片
        backgroundImage = ImageIO.read(new File("src\\picture\\背景01.jpg"));
        this.outputWidth = 1920;
        this.outputHeight = 1080;

        // 這邊我們以背景圖片的尺寸作為輸出尺寸
        recorder = new FFmpegFrameRecorder(filename, outputWidth, outputHeight);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.setFrameRate(targetFPS);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        // 畫質設定
        recorder.setVideoBitrate(8000 * 1000); // 8 Mbps
        recorder.setVideoOption("preset", "slow");
        recorder.setVideoOption("crf", "18");

        // 音訊設定
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setAudioChannels(1);
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

    private void captureAndRecord() {
        try {
            // 擷取螢幕截圖
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);

            // 計算縮放後的寬高（80%）
            int scaledWidth = (int) (outputWidth * 0.8);
            int scaledHeight = (int) (outputHeight * 0.8);

            // 建立合成圖
            BufferedImage combinedImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = combinedImage.createGraphics();

            // 畫背景圖
            g.drawImage(backgroundImage, 0, 0, outputWidth, outputHeight, null);

            // 將螢幕截圖縮放後置中
            int x = (outputWidth - scaledWidth) / 2;
            int y = (outputHeight - scaledHeight) / 2;
            g.drawImage(screenCapture, x, y, scaledWidth, scaledHeight, null);
            g.dispose();

            // 將合成後的圖轉成 Mat 物件
            byte[] data = ((DataBufferByte) combinedImage.getRaster().getDataBuffer()).getData();
            Mat mat = new Mat(outputHeight, outputWidth, opencv_core.CV_8UC3);
            mat.data().put(data);

            // 轉換成 Frame 並錄製
            org.bytedeco.javacv.Frame frame = converter.convert(mat);
            recorder.setTimestamp(videoTimestamp);  // 設置時間戳
            recorder.record(frame);

            videoTimestamp += timestampIncrementMicros;  // 增加時間戳
        } catch (Exception e) {
            System.err.println("Recorder: 擷取或錄製影格時發生錯誤！");
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        running.set(false);
        System.out.println("Recorder: 收到停止錄影指令。");
    }
}
