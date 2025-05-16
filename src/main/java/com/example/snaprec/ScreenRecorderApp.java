package com.example.snaprec;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ScreenRecorderApp extends Application {

    private GUIController guiController;

    @Override
    public void start(Stage primaryStage) {
        guiController = new GUIController();
        Scene scene = guiController.createScene();

        primaryStage.setScene(scene);
        primaryStage.setTitle("螢幕錄影工具");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
