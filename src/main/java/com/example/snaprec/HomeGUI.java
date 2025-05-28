package com.example.snaprec;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

public class HomeGUI extends GUIController {

    @Override
    public Scene createScene() {
        Button startBtn = new Button("開始錄影");
        Button stopBtn = new Button("停止錄影");
        Label statusLabel = new Label("狀態：未錄影");

        startBtn.setOnAction(e -> {
            if (recorder == null || !recorder.isAlive()) {
                try {
                    System.setProperty("sun.java2d.uiScale", "1.0");
                    recorder = new Recorder("output.mp4");
                    recorder.start();
                    statusLabel.setText("狀態：錄影中...");
                    globalMouseListener = new GlobalMouseListener(recorder); // 儲存 listener
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        stopBtn.setOnAction(e -> {
            if (recorder != null) {
                recorder.shutdownAndWait();
                recorder = null;
                if (globalMouseListener != null) {
                    globalMouseListener.stop();
                    globalMouseListener = null;
                }

                // 關閉當前視窗
                Stage currentStage = (Stage) ((Button) e.getSource()).getScene().getWindow();
                currentStage.close();

                // 顯示編輯與預覽畫面
                EditPreviewGUI EditPreviewGUI = new EditPreviewGUI("output.mp4");
                try {
                    EditPreviewGUI.showPreviewWindow();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);

        HBox controlBox = new HBox(10, startBtn, stopBtn, statusLabel);
        controlBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, controlBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 400, 100);

        mediaView.fitWidthProperty().bind(scene.widthProperty());
        mediaView.fitHeightProperty().bind(scene.heightProperty().subtract(100));
        return scene;
    }
}
