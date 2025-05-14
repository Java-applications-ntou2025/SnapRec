package com.example.snaprec;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScreenRecorderApp extends Application {

    private Recorder recorder;

    @Override
    public void start(Stage primaryStage) {
        Button startBtn = new Button("開始錄影");
        Button stopBtn = new Button("停止錄影");

        startBtn.setOnAction(e -> {
            try {
//                String basePath = System.getProperty("user.dir"); // 取得目前工作目錄
//                String outputPath = basePath + "/src/main/java/com/example/snaprec/output.mp4";
//                recorder = new Recorder(outputPath);
                recorder = new Recorder("C:/Users/t0910/OneDrive/codes/JavaFinal/SnapRec/src/main/java/com/example/snaprec/output.mp4");
                recorder.start();
                System.out.println("start record!");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        stopBtn.setOnAction(e -> {
            if (recorder != null) {
                recorder.stopRecording();
                System.out.println("record ended");
            }
        });

        VBox root = new VBox(10, startBtn, stopBtn);
        root.setStyle("-fx-padding: 20;");
        primaryStage.setScene(new Scene(root, 300, 150));
        primaryStage.setTitle("螢幕錄影工具");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
