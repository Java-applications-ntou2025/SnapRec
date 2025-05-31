package com.example.snaprec;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class HomeGUI extends GUIController {

    @Override
    public Scene createScene() {
        Button startBtn = new Button("開始錄影");
        Button stopBtn = new Button("停止錄影");
        Label statusLabel = new Label("狀態：未錄影");

        // 🔽 新增下拉式選單（滑鼠樣式選擇）
        ComboBox<String> cursorComboBox = new ComboBox<>();
        cursorComboBox.getItems().addAll("default", "Green Cursor", "Blue Cursor", "Yellow Cursor", "Rainbow Cursor", "Rainbow Halo Cursor");
        cursorComboBox.setValue("default");

        // 🖱️ 新增 ImageView 顯示目前滑鼠圖片
        ImageView cursorPreview = new ImageView();
        cursorPreview.setFitWidth(32);
        cursorPreview.setFitHeight(32);
        cursorPreview.setPreserveRatio(true);
        AtomicReference<String> CursorimagePath = new AtomicReference<>("src\\ImageRepository\\normal.png");

        try {
            // 預設顯示圖片
            Image defaultCursorImage = new Image(new FileInputStream("src\\ImageRepository\\normal.png"));
            cursorPreview.setImage(defaultCursorImage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 💡 下拉選單選擇時變更圖片
        cursorComboBox.setOnAction(e -> {
            String selected = cursorComboBox.getValue();
            CursorimagePath.set(switch (selected) {
                case "Green Cursor" -> "src\\ImageRepository\\green.png";
                case "Blue Cursor" -> "src\\ImageRepository\\blue.png";
                case "Yellow Cursor" -> "src\\ImageRepository\\yellow.png";
                case "Rainbow Cursor" -> "src\\ImageRepository\\rainbow.png";
                case "Rainbow Halo Cursor" -> "src\\ImageRepository\\rainbow_halo.png";
                default -> "src\\ImageRepository\\normal.png";
            });

            try {
                BufferedImage cursorImage = ImageIO.read(new File(CursorimagePath.get()));
                cursorPreview.setImage(new Image(new FileInputStream(CursorimagePath.get())));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        ComboBox<String> backgroundComboBox = new ComboBox<>();
        backgroundComboBox.getItems().addAll("Background1", "Background2", "Background3", "Background4", "Background5", "star", "dark", "grey", "grey-leaf");
        backgroundComboBox.setValue("Background1");

        AtomicReference<String> backgroundImagePath = new AtomicReference<>("src\\BackgroundRepository\\Background1.jpg");

        ImageView backgroundPreview = new ImageView();
        backgroundPreview.setFitWidth(64);
        backgroundPreview.setFitHeight(36);
        backgroundPreview.setPreserveRatio(true);
        try {
            backgroundPreview.setImage(new Image(new FileInputStream(backgroundImagePath.get())));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

// 背景選擇邏輯
        backgroundComboBox.setOnAction(e -> {
            String selected = backgroundComboBox.getValue();
            backgroundImagePath.set(switch (selected) {
                case "Background2" -> "src\\BackgroundRepository\\Background2.jpg";
                case "Background3" -> "src\\BackgroundRepository\\BackgroundGold.png";
                case "Background4" -> "src\\BackgroundRepository\\Background4.jpg";
                case "Background5" -> "src\\BackgroundRepository\\Background5.jpg";
                case "Background6" -> "src\\BackgroundRepository\\Background6.png";
                case "star" -> "src\\BackgroundRepository\\star.jpg";
                case "dark" -> "src\\BackgroundRepository\\dark.jpg";
                case "grey" -> "src\\BackgroundRepository\\grey.jpg";
                case "grey-leaf" -> "src\\BackgroundRepository\\grey-leaf-pattern.jpg";
                default -> "src\\BackgroundRepository\\Background1.jpg";
            });

            try {
                backgroundPreview.setImage(new Image(new FileInputStream(backgroundImagePath.get())));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });


        // 錄影按鈕動作
        startBtn.setOnAction(e -> {
            if (recorder == null || !recorder.isAlive()) {
                try {
                    System.setProperty("sun.java2d.uiScale", "1.0");
                    boolean useGpuEncoding = true;
                    recorder = new Recorder("output.mp4", useGpuEncoding, CursorimagePath.get(), backgroundImagePath.get());
                    recorder.start();
                    statusLabel.setText("狀態：錄影中...");
                    globalMouseListener = new GlobalMouseListener(recorder);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // 停止錄影
        stopBtn.setOnAction(e -> {
            if (recorder != null) {
                recorder.shutdownAndWait();
                recorder = null;
                if (globalMouseListener != null) {
                    globalMouseListener.stop();
                    globalMouseListener = null;
                }

                Stage currentStage = (Stage) ((Button) e.getSource()).getScene().getWindow();
                currentStage.close();

                EditPreviewGUI editPreviewGUI = new EditPreviewGUI("output.mp4");
                try {
                    editPreviewGUI.showPreviewWindow();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        // 🔧 排版
        HBox controlBox = new HBox(10, startBtn, stopBtn, statusLabel);
        controlBox.setAlignment(Pos.CENTER);

        HBox cursorBox = new HBox(10, new Label("滑鼠樣式："), cursorComboBox, cursorPreview);
        cursorBox.setAlignment(Pos.CENTER);

        HBox backgroundBox = new HBox(10, new Label("背景樣式："), backgroundComboBox, backgroundPreview);
        backgroundBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(15, controlBox, cursorBox, backgroundBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 500, 150);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());

        return scene;
    }
}
