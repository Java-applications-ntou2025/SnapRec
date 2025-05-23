package com.example.snaprec;


import java.io.File;

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
}
