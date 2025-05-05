package com.example.snaprec;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class ScreenRecorderApp extends Application {

    private Recorder recorder;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Slider progressSlider;
    private boolean isSeeking = false;

    @Override
    public void start(Stage primaryStage) {
        Button startBtn = new Button("開始錄影");
        Button stopBtn = new Button("停止錄影");
        Button playBtn = new Button("播放");
        Button pauseBtn = new Button("暫停");

        startBtn.setOnAction(e -> {
            try {
                recorder = new Recorder("output.mp4");
                recorder.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        stopBtn.setOnAction(e -> {
            if (recorder != null) {
                recorder.stopRecording();
                recorder = null;
                playVideo(); // 停止錄影後播放影片
            }
        });

        playBtn.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.play();
            }
        });

        pauseBtn.setOnAction(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
            }
        });

        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);

        progressSlider = new Slider();
        progressSlider.setMin(0);
        progressSlider.setMax(100);
        progressSlider.setValue(0);
        progressSlider.setPrefWidth(600);

        // 監聽滑桿拖曳事件
        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                double seekTime = progressSlider.getValue() / 100 * mediaPlayer.getTotalDuration().toMillis();
                mediaPlayer.seek(Duration.millis(seekTime));
            }
            isSeeking = false;
        });

        HBox controlBox = new HBox(10, startBtn, stopBtn, playBtn, pauseBtn);
        controlBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, controlBox, mediaView, progressSlider);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 800, 600);

        mediaView.fitWidthProperty().bind(scene.widthProperty());
        mediaView.fitHeightProperty().bind(scene.heightProperty().subtract(100)); // 預留控制區域高度

        primaryStage.setScene(scene);
        primaryStage.setTitle("螢幕錄影工具");
        primaryStage.show();
    }

    private void playVideo() {
        File videoFile = new File("output.mp4");
        if (videoFile.exists()) {
            Media media = new Media(videoFile.toURI().toString());
            if (mediaPlayer != null) {
                mediaPlayer.dispose();
            }
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            // 監聽影片播放進度，更新進度條
            mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                if (!isSeeking && mediaPlayer.getTotalDuration() != null) {
                    double progress = newValue.toMillis() / mediaPlayer.getTotalDuration().toMillis() * 100;
                    progressSlider.setValue(progress);
                }
            });

            mediaPlayer.play();
        } else {
            System.out.println("影片檔案不存在！");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
