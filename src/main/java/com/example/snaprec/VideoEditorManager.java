package com.example.snaprec;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class VideoEditorManager {
    private final String sourceFilePath;
    private VideoEditState currentState;
    private Stack<EditAction> undoStack = new Stack<>();
    private Stack<EditAction> redoStack = new Stack<>();

    public VideoEditorManager(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
        this.currentState = new VideoEditState();
        this.currentState.getSegments().add(new ClipSegment(0, getVideoDurationInSeconds())); // 初始整段影片
    }

    public void applyAction(EditAction action) {
        undoStack.push(action);
        action.apply(currentState);
        redoStack.clear(); // 清除 redo 階段
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            EditAction action = undoStack.pop();
            action.undo(currentState);
            redoStack.push(action);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            EditAction action = redoStack.pop();
            action.apply(currentState);
            undoStack.push(action);
        }
    }

    public void preview() throws IOException, InterruptedException {
        List<String> partFiles = new ArrayList<>();
        int index = 0;

        for (ClipSegment seg : currentState.getSegments()) {
            String partFile = "part" + index + ".mp4";
            partFiles.add(partFile);

            System.out.println("Segment: " + seg.startTime + " to " + seg.endTime);

            double duration = seg.endTime - seg.startTime;

            // 使用重新編碼方式裁切，避免時間戳錯誤
            List<String> command = List.of(
                    "ffmpeg", "-y",
                    "-ss", String.valueOf(seg.startTime),
                    "-t", String.valueOf(duration),
                    "-i", sourceFilePath,
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-crf", "23",
                    "-an", // 如果你不需要音訊，可以加這行
                    partFile
            );
            runFFmpegCommand(command);
            index++;
        }

        // 產生 concat.txt
        File concatFile = new File("concat.txt");
        try (PrintWriter writer = new PrintWriter(concatFile)) {
            for (String part : partFiles) {
                writer.println("file '" + part + "'");
            }
        }

        // 合併成 preview.mp4
        List<String> concatCommand = List.of(
                "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                "-i", "concat.txt",
                "-c", "copy", "preview.mp4"
        );
        runFFmpegCommand(concatCommand);

        // 預覽後可以選擇是否要刪除中繼檔
        // for (String part : partFiles) new File(part).delete();
        // concatFile.delete();
    }


    private void runFFmpegCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO(); // 讓 FFmpeg 輸出顯示到控制台
        Process process = pb.start();
        process.waitFor();
    }


    public void exportToFile(String outputFileName) {
        try {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < currentState.getSegments().size(); i++) {
                ClipSegment seg = currentState.getSegments().get(i);
                String partName = "part" + i + ".mp4";
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y",
                        "-i", sourceFilePath,
                        "-ss", String.valueOf(seg.startTime),
                        "-to", String.valueOf(seg.endTime),
                        "-c", "copy", partName);
                pb.inheritIO().start().waitFor();
                parts.add(partName);
            }

            // 建立 concat.txt
            File concatFile = new File("concat.txt");
            PrintWriter writer = new PrintWriter(concatFile);
            for (String part : parts) {
                writer.println("file '" + part + "'");
            }
            writer.close();

            // 合併
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y",
                    "-f", "concat", "-safe", "0", "-i", "concat.txt",
                    "-c", "copy", outputFileName);
            pb.inheritIO().start().waitFor();

            // 清理中繼檔
            for (String part : parts) new File(part).delete();
            concatFile.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VideoEditState getCurrentState() {
        return currentState.copy();
    }

    private double getVideoDurationInSeconds() {
        // TODO: 你可以用 FFprobe 取得影片長度或用 JavaFX Media 簡單估算
        return 60.0; // 假設一開始影片是 60 秒
    }
}
