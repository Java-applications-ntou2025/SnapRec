package com.example.snaprec;

import javafx.beans.InvalidationListener;
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
import javafx.util.Duration;

import java.io.File;

public class GUIController {

    private Recorder recorder;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Slider progressSlider;
    private boolean isSeeking = false;
    private GlobalMouseListener globalMouseListener;


    public Scene createScene() {
        Button startBtn = new Button("開始錄影");
        Button stopBtn = new Button("停止錄影");
        Button playBtn = new Button("播放");
        Button pauseBtn = new Button("暫停");

        startBtn.setOnAction(e -> {
            if (recorder == null || !recorder.isAlive()) {
                try {
                    recorder = new Recorder("output.mp4");
                    recorder.start();
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
                playVideo();
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

        progressSlider = new Slider(0, 100, 0);
        progressSlider.setPrefWidth(600);

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
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 800, 600);

        mediaView.fitWidthProperty().bind(scene.widthProperty());
        mediaView.fitHeightProperty().bind(scene.heightProperty().subtract(100));

        return scene;
    }

    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
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
}
