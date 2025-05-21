package com.example.snaprec;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
    }

    public static void main(String[] args) {
        launch(args);
    }
}

