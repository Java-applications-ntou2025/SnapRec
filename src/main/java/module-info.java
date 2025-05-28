module com.example.snaprec {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.fxml;

    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;
    requires org.bytedeco.ffmpeg;
    requires java.desktop;
    requires java.logging;
    requires jnativehook;
    requires org.controlsfx.controls;
    requires org.bytedeco.libfreenect;

    opens com.example.snaprec to javafx.fxml;
    exports com.example.snaprec;
}
