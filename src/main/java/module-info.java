module org.tiny.whiterun {
    requires org.json;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires javafx.fxml;
    requires javafx.controls;
    requires java.desktop;


    opens org.tiny.whiterun to javafx.fxml;
    exports org.tiny.whiterun;
}