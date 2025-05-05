module com.example.snaprec {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.fxml;

    requires org.bytedeco.javacv;
    requires org.bytedeco.opencv;
    requires org.bytedeco.ffmpeg;
    requires java.desktop;

    opens com.example.snaprec to javafx.fxml;
    exports com.example.snaprec;
}
