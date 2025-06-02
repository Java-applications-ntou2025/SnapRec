package com.example.snaprec;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

public class VideoEditor {

    public static String trimVideoSegment(String inputPath, double startMs, double endMs) {
        String outputPath = "temp_trimmed_" + System.currentTimeMillis() + ".mp4";
        double startSec = startMs / 1000;
        double durationSec = (endMs - startMs) / 1000;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", inputPath,
                    "-ss", String.valueOf(startSec),
                    "-t", String.valueOf(durationSec),
                    "-c:v", "libx264", "-preset", "ultrafast", "-crf", "23",
                    outputPath
            );
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0 && new File(outputPath).exists()) {
                return outputPath;
            } else {
                System.err.println("FFmpeg 剪輯失敗，exit code: " + exitCode);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String trimExcludingSegment(String inputPath, double startMs, double endMs) {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String baseName = UUID.randomUUID().toString();
            String frontPart = tempDir + baseName + "_part1.mp4";
            String backPart = tempDir + baseName + "_part2.mp4";
            String concatList = tempDir + baseName + "_list.txt";
            String outputPath = tempDir + baseName + "_excluded_trim.mp4";

            double startSec = startMs / 1000.0;
            double endSec = endMs / 1000.0;

            // 前段（0 ~ startSec）
            Process p1 = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", inputPath,
                    "-t", String.valueOf(startSec),
                    "-c:v", "libx264", "-preset", "ultrafast", "-crf", "23",
                    frontPart
            ).inheritIO().start();
            p1.waitFor();

            // 後段（endSec ~ end）
            Process p2 = new ProcessBuilder(
                    "ffmpeg", "-y", "-ss", String.valueOf(endSec), "-i", inputPath,
                    "-c:v", "libx264", "-preset", "ultrafast", "-crf", "23",
                    backPart
            ).inheritIO().start();
            p2.waitFor();

            // 建立 concat list 檔案
            Files.write(Paths.get(concatList), (
                    "file '" + frontPart.replace("\\", "/") + "'\n" +
                            "file '" + backPart.replace("\\", "/") + "'\n"
            ).getBytes());

            // 合併兩段（需轉為同 codec 格式）
            Process p3 = new ProcessBuilder(
                    "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                    "-i", concatList, "-c", "copy", outputPath
            ).inheritIO().start();
            p3.waitFor();

            // 清理暫存檔
            new File(frontPart).delete();
            new File(backPart).delete();
            new File(concatList).delete();

            return outputPath;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
