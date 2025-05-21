package com.example.snaprec;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

import static java.lang.Thread.sleep;

public class EditPreviewGUI extends GUIController {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Slider progressSlider;
    private Slider startSlider;
    private Slider endSlider;
    private boolean isSeeking = false;
    private final String videoPath;
    private ToggleButton playPauseButton = new ToggleButton("播放");;
    private boolean videoEnded = false;

    public EditPreviewGUI(String videoPath) {
        this.videoPath = videoPath;
    }

    public void showPreviewWindow() throws InterruptedException {
        Stage previewStage = new Stage();
        previewStage.setTitle("錄影預覽編輯");

        // 左側樣式控制欄
        Label styleLabel = new Label("影片樣式");
//        Slider roundnessSlider = new Slider(0, 50, 4);
//        roundnessSlider.setShowTickMarks(true);
//        roundnessSlider.setShowTickLabels(true);
//        roundnessSlider.setMajorTickUnit(10);
//        roundnessSlider.setBlockIncrement(1);
//
//        Slider shadowSlider = new Slider(0, 100, 60);
//        shadowSlider.setShowTickMarks(true);
//        shadowSlider.setShowTickLabels(true);
//
//        ColorPicker bgColorPicker = new ColorPicker(Color.BLACK);
//        ComboBox<String> aspectRatioBox = new ComboBox<>();
//        aspectRatioBox.getItems().addAll("原始尺寸", "16:9", "4:3", "1:1");
//        aspectRatioBox.getSelectionModel().selectFirst();

        VBox stylePanel = new VBox(15,
                styleLabel
//                ,
//                new Label("圓角"), roundnessSlider,
//                new Label("陰影"), shadowSlider,
//                new Label("背景色"), bgColorPicker,
//                new Label("比例"), aspectRatioBox
        );
        stylePanel.setPadding(new Insets(20));
        stylePanel.setPrefWidth(350);
        stylePanel.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: white;");
        stylePanel.getChildren().forEach(node -> {
            if (node instanceof Label) ((Label) node).setTextFill(Color.WHITE);
        });

        // 中間播放區域
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        StackPane mediaContainer = new StackPane();
        mediaContainer.setPrefSize(1600, 900); // 影片最大顯示範圍
        mediaContainer.setStyle("-fx-background-color: #000000;");

// 設定 MediaView 尺寸限制與縮放策略
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);

// 綁定 MediaView 大小不超過 mediaContainer
        mediaView.fitWidthProperty().bind(mediaContainer.widthProperty());
        mediaView.fitHeightProperty().bind(mediaContainer.heightProperty());

        mediaContainer.getChildren().add(mediaView);
        mediaContainer.setStyle("-fx-background-color: #000;");
//        mediaContainer.setPrefSize(800, 450);
        mediaContainer.setEffect(new DropShadow(60, Color.BLACK));

        // 播放控制
//        ToggleButton playPauseButton = new ToggleButton("播放");
        this.playPauseButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                if (this.playPauseButton.isSelected()) {
                    if (videoEnded) {
                        mediaPlayer.seek(Duration.ZERO);  // 重頭播放
                        videoEnded = false;
                    }
                    this.playPauseButton.setText("暫停");
                    mediaPlayer.play();
                } else {
                    this.playPauseButton.setText("播放");
                    mediaPlayer.pause();
                }
            }
        });
        //影片控制滑竿
        progressSlider = new Slider();
        progressSlider.setPrefWidth(600);
        progressSlider.setDisable(true); // 尚未載入影片時先禁用
        // 剪輯起訖滑桿
//        startSlider = new Slider(0, 100, 0);
//        endSlider = new Slider(0, 100, 100);
        startSlider = new Slider(0, 100, 0);
        endSlider = new Slider(0, 100, 100);
        startSlider.setPrefWidth(600);
        endSlider.setPrefWidth(600);

        Label clipLabel = new Label("剪輯範圍設定");
        clipLabel.setTextFill(Color.BLACK);

        Button exportButton = new Button("匯出剪輯區段");
        exportButton.setDisable(true); // 先關閉功能，待未來實作

        VBox clipControl = new VBox(10, clipLabel, startSlider, endSlider, exportButton);
        clipControl.setAlignment(Pos.CENTER);
        clipControl.setPadding(new Insets(10));

        VBox videoPanel = new VBox(15, mediaContainer, this.playPauseButton, progressSlider, clipControl);
        videoPanel.setAlignment(Pos.TOP_CENTER);
        videoPanel.setPadding(new Insets(20));

        HBox root = new HBox(stylePanel, videoPanel);
        Scene scene = new Scene(root, 1920, 1080); //EditPreviewGUI 解析度

        previewStage.setScene(scene);
        previewStage.show();

        loadAndPlayVideo();
    }

    private void loadAndPlayVideo() throws InterruptedException {
        sleep(500); // 給FFmpeg處裡時間輸出mp4
        File file = new File(videoPath);
        if (!file.exists()) {
            System.err.println("影片不存在：" + file.getAbsolutePath());
            return;
        }

        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        mediaPlayer.setOnReady(() -> {
            Duration total = mediaPlayer.getTotalDuration();
            progressSlider.setMax(total.toMillis());
            progressSlider.setDisable(false);
            startSlider.setMax(total.toMillis());
            endSlider.setMax(total.toMillis());
            endSlider.setValue(total.toMillis());
        });

        progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            isSeeking = isChanging;
            if (!isChanging) {
                mediaPlayer.seek(Duration.millis(progressSlider.getValue()));
            }
        });

        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            isSeeking = false;
            mediaPlayer.seek(Duration.millis(progressSlider.getValue()));
        });

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isSeeking && mediaPlayer.getTotalDuration() != null) {
                double millis = newTime.toMillis();
                progressSlider.setValue(millis);
                if (millis > endSlider.getValue()) {
                    mediaPlayer.pause(); // 自動停止於剪輯結尾
//                    this.playPauseButton.setSelected(false);
//                    this.playPauseButton.setText("播放");
                }
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            videoEnded = true;
            mediaPlayer.pause(); // 停在最後一幀
            this.playPauseButton.setSelected(false);
            this.playPauseButton.setText("播放");
        });

        mediaPlayer.setOnError(() -> {
            System.err.println("播放錯誤：" + mediaPlayer.getError());
            mediaPlayer.getError().printStackTrace();
        });
    }
}
