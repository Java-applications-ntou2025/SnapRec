package com.example.snaprec;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

import static java.lang.Thread.sleep;

import java.util.Objects;
import java.util.Stack;

import org.bytedeco.libfreenect._freenect_context;
import org.controlsfx.control.RangeSlider;


public class EditPreviewGUI extends GUIController {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Slider progressSlider;
    private boolean isInitializingVideo = false;
    private boolean previewMode = false;
    private boolean excludeMode = false;
    private RangeSlider rangeSlider;
    private boolean isSeeking = false;
    private final String videoPath;
    private boolean videoEnded = false;
    private String currentVideoPath;
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private Button undoButton = new Button("undo");
    private Button redoButton = new Button("redo");
    private Button closeButton = new Button("匯出影片");
    final double MAX_FONT_SIZE = 25.2;
    final double MAX_BNT_SIZE = 20.2;
    private ToggleButton playPauseButton;
    private Image playIcon = new Image("file:src/ImageRepository/play.png");
    private Image pauseIcon = new Image("file:src/ImageRepository/pause.png");
    private ImageView playPauseImageView = new ImageView(playIcon);




    public EditPreviewGUI(String videoPath) {
        this.videoPath = videoPath;
        Image play = new Image("file:src/ImageRepository/play.png");
        this.playPauseImageView.setFitWidth(16);
        this.playPauseImageView.setFitHeight(16);
        this.playPauseButton = new ToggleButton("", playPauseImageView);
    }


    public void showPreviewWindow() throws InterruptedException {
        this.currentVideoPath = this.videoPath;
        Stage previewStage = new Stage();
        previewStage.setTitle("錄影預覽編輯");

        // 左側樣式控制欄
        Label styleLabel = new Label("影片樣式");

        VBox stylePanel = new VBox(15,
                styleLabel
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
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        mediaContainer.setPrefSize(bounds.getWidth()/5*4, bounds.getHeight()/4*3); // 影片最大顯示範圍
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
                    mediaPlayer.play();
                    playPauseImageView.setImage(pauseIcon);
                } else {
                    mediaPlayer.pause();
                    playPauseImageView.setImage(playIcon);
                }
            }
        });

        playPauseButton.setFont(new Font(MAX_BNT_SIZE));
        //影片控制滑竿
        progressSlider = new Slider();
        progressSlider.setPrefWidth(600);
        progressSlider.setDisable(true); // 尚未載入影片時先禁用
        // 剪輯起訖滑桿
        rangeSlider = new RangeSlider(0, 100, 0, 100);
        rangeSlider.setPrefWidth(600);
        rangeSlider.setShowTickMarks(false);
        rangeSlider.setShowTickLabels(false);
        rangeSlider.setMajorTickUnit(10);




        Button previewEditButton = new Button("預覽剪輯區段");
        previewEditButton.setOnAction(e -> {
            double startMs = rangeSlider.getLowValue();
            double endMs = rangeSlider.getHighValue();
            if (startMs >= endMs) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "起始時間必須小於結束時間！");
                alert.showAndWait();
                return;
            }
            if (mediaPlayer != null) {
                previewMode = true; // ← 進入預覽模式
                mediaPlayer.seek(Duration.millis(startMs));
                mediaPlayer.play();
                playPauseButton.setSelected(true);
            }
        });

        ToggleButton modeToggleButton = new ToggleButton("模式：保留範圍");
        modeToggleButton.setOnAction(e -> {
            excludeMode = !excludeMode;
            modeToggleButton.setText(excludeMode ? "模式：排除範圍" : "模式：保留範圍");

            if (excludeMode) {
                rangeSlider.getStyleClass().add("exclude");
                previewEditButton.setDisable(true);
            } else {
                rangeSlider.getStyleClass().remove("exclude");
                previewEditButton.setDisable(false);
            }
        });


        Button exportButton = new Button("剪輯區段");
        exportButton.setOnAction(e -> {
            double startMs = rangeSlider.getLowValue();
            double endMs = rangeSlider.getHighValue();

            if (startMs >= endMs) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "起始時間必須小於結束時間！");
                alert.showAndWait();
                return;
            }

            String trimmedPath;
            if (excludeMode) {
                trimmedPath = VideoEditor.trimExcludingSegment(currentVideoPath, startMs, endMs);
//                System.out.println("排除中");
            } else {
                trimmedPath = VideoEditor.trimVideoSegment(currentVideoPath, startMs, endMs);
            }

            if (trimmedPath != null) {
                undoStack.push(currentVideoPath);
                currentVideoPath = trimmedPath;
                redoStack.clear();

                try {
                    loadVideo(currentVideoPath);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                updateUndoRedoButtons(undoButton, redoButton);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "剪輯失敗！");
                alert.showAndWait();
            }
        });

        // EditPreviewGUI.java

        closeButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("my_video.mp4");
            dialog.setTitle("匯出影片");
            dialog.setHeaderText("請輸入影片檔名（含 .mp4）：");
            dialog.setContentText("檔名：");

            dialog.showAndWait().ifPresent(filename -> {
                if (filename.trim().isEmpty() || !filename.endsWith(".mp4")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "請輸入有效的 .mp4 檔名！");
                    alert.showAndWait();
                    return;
                }
                // 先關閉視窗與 MediaPlayer
                if (mediaPlayer != null) {
                    mediaPlayer.dispose();
                }
                // 關閉視窗
                Stage currentStage = (Stage) closeButton.getScene().getWindow();
                currentStage.close();

                // 再執行檔案重新命名
                File oldFile = new File(currentVideoPath);
                File newFile = new File(filename);
                try {
                    if (oldFile.exists()) {
                        if (oldFile.renameTo(newFile)) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "影片已匯出為：" + filename);
                            alert.showAndWait();
                            File dir = new File(".");
                            File[] files = dir.listFiles((d, name) -> name.endsWith(".mp4") && !name.equals(newFile.getName()));
                            if (files != null) {
                                for (File f : files) {
                                    f.delete();
                                }
                            }
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "檔案重新命名失敗！");
                            alert.showAndWait();
                            currentStage.show();
                        }
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "找不到原始影片 output.mp4！");
                        alert.showAndWait();
                    }
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "無法建立新檔案：" + ex.getMessage());
                    alert.showAndWait();
                }
            });
        });


        undoButton.setDisable(true);
        redoButton.setDisable(true);
        undoButton.setFont(new Font(MAX_BNT_SIZE));
        redoButton.setFont(new Font(MAX_BNT_SIZE));
        previewEditButton.setFont(new Font(MAX_BNT_SIZE));
        exportButton.setFont(new Font(MAX_BNT_SIZE));

        undoButton.setOnAction(e -> {
            if (!undoStack.isEmpty()) {
                redoStack.push(currentVideoPath);
                currentVideoPath = undoStack.pop();
                try {
                    loadVideo(currentVideoPath);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                updateUndoRedoButtons(undoButton, redoButton); // <--- 新增
            }
        });

        redoButton.setOnAction(e -> {
            if (!redoStack.isEmpty()) {
                undoStack.push(currentVideoPath);
                currentVideoPath = redoStack.pop();
                try {
                    loadVideo(currentVideoPath);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                updateUndoRedoButtons(undoButton, redoButton); // <--- 新增
            }
        });

        HBox functionButtons = new HBox(10, undoButton, redoButton, previewEditButton, exportButton, closeButton);
        functionButtons.setAlignment(Pos.CENTER);


        VBox clipControl = new VBox(10, modeToggleButton, rangeSlider, functionButtons);
        clipControl.setAlignment(Pos.CENTER);
        clipControl.setPadding(new Insets(10));

        VBox videoPanel = new VBox(15, mediaContainer, this.playPauseButton, progressSlider, clipControl);
        videoPanel.setAlignment(Pos.TOP_CENTER);
        videoPanel.setPadding(new Insets(20));

        HBox root = new HBox(stylePanel, videoPanel);

        Scene scene = new Scene(root); //EditPreviewGUI 解析度
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm()
        );

        previewStage.setScene(scene);




//        previewStage.setFullScreen(true); // 設定為全螢幕
        previewStage.setMaximized(true); // 最大化，但保留邊框
        previewStage.show();

        loadVideo(videoPath);
    }

    private void updateUndoRedoButtons(Button undoButton, Button redoButton) {
        undoButton.setDisable(undoStack.isEmpty());
        redoButton.setDisable(redoStack.isEmpty());
    }

    private void loadVideo(String path) throws InterruptedException {
        sleep(700); // 給FFmpeg處裡時間輸出mp4
        if (mediaPlayer != null) mediaPlayer.dispose();
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("影片不存在：" + file.getAbsolutePath());
            return;
        }

        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        progressSlider.setDisable(true);

        progressSlider.setOnMousePressed(e -> isSeeking = true);
        progressSlider.setOnMouseReleased(e -> {
            isSeeking = false;
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.millis(progressSlider.getValue()));
            }
        });

        progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            isSeeking = isChanging;
            if (!isChanging && mediaPlayer != null) {
                mediaPlayer.seek(Duration.millis(progressSlider.getValue()));
            }
        });

        rangeSlider.lowValueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                if (isInitializingVideo) return;
                else {
                    mediaPlayer.pause();
                    playPauseButton.setSelected(false);
                }
            }
            mediaPlayer.seek(Duration.millis(newVal.doubleValue()));
        });

        rangeSlider.highValueProperty().addListener((obs, oldVal, newVal) -> {
            if (isInitializingVideo) return; // ← 加這行防止初始化時觸發 seek

            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPauseButton.setSelected(false);
            }
            mediaPlayer.seek(Duration.millis(newVal.doubleValue()));
        });

        mediaPlayer.setOnReady(() -> {
            Duration total = mediaPlayer.getTotalDuration();
            progressSlider.setMax(total.toMillis());
            progressSlider.setValue(0);
            progressSlider.setDisable(false);
            rangeSlider.setMin(0);
            rangeSlider.setMax(total.toMillis());

            // 預設整段區間
            isInitializingVideo = true;
            rangeSlider.setLowValue(0);
            rangeSlider.setHighValue(total.toMillis());
            isInitializingVideo = false; // ← 必須放在 setHighValue 之後！
        });


        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isSeeking && mediaPlayer.getTotalDuration() != null) {
                double millis = newTime.toMillis();
                progressSlider.setValue(millis);
                if (previewMode && millis >= rangeSlider.getHighValue()) {
                    mediaPlayer.pause();
                    mediaPlayer.seek(Duration.millis(rangeSlider.getLowValue()));
                    playPauseButton.setSelected(false);
                    previewMode = false; // ← 結束預覽模式
                }
            }
        });


        mediaPlayer.setOnEndOfMedia(() -> {
            videoEnded = true;
            mediaPlayer.pause();
            playPauseButton.setSelected(false);
            playPauseImageView.setImage(playIcon); // 回復播放圖示
        });


        mediaPlayer.setOnError(() -> {
            System.err.println("播放錯誤：" + mediaPlayer.getError());
            mediaPlayer.getError().printStackTrace();
        });
    }
}
