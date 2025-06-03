package com.example.snaprec;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class ScreenRecorderApp extends Application {

    private GUIController guiController;
    private Recorder recorder = null;
    private GlobalMouseListener globalMouseListener;

    @Override
    public void start(Stage primaryStage) {
        guiController = new HomeGUI();
        Scene scene = guiController.createScene();

        primaryStage.setScene(scene);
        primaryStage.setTitle("螢幕錄影工具");
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (globalMouseListener != null) {
            globalMouseListener.stop();
        }
        if (recorder != null) {
            recorder.stopRecording();
        }

        try {
            Thread.sleep(1000); // 等待 OS 釋放檔案鎖
        } catch (InterruptedException ignored) {}

        deleteAllMp4Files(new File("."));

        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteAllMp4Files(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                deleteAllMp4Files(file);
            } else if (file.getName().toLowerCase().endsWith(".mp4")) {
                try {
                    // 嘗試刪除檔案
                    boolean deleted = false;
                    for (int i = 0; i < 10; i++) {
                        deleted = file.delete();
                        if (deleted) break;
                        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    }
                    System.out.println("刪除 " + file.getAbsolutePath() + ": " + deleted);
                } catch (Exception e) {
                    System.err.println("刪除檔案失敗: " + file.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

