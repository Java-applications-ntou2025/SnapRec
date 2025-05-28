package com.example.snaprec;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public abstract class GUIController {

    public Recorder recorder;
    public MediaView mediaView;
    public GlobalMouseListener globalMouseListener;

    public Scene createScene() {

        Label label = new Label("Java SnapRac：");
        HBox controlBox = new HBox(10, label);
        controlBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, controlBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root);

        mediaView.fitWidthProperty().bind(scene.widthProperty());
        mediaView.fitHeightProperty().bind(scene.heightProperty().subtract(100));
        return scene;
    }
}

