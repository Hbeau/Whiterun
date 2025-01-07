module org.tiny.whiterun {
    requires org.json;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires javafx.fxml;
    requires javafx.controls;
    requires java.desktop;
    requires org.apache.commons.io;


    opens org.tiny.whiterun to javafx.fxml;
    exports org.tiny.whiterun;
    exports org.tiny.whiterun.controllers;
    opens org.tiny.whiterun.controllers to javafx.fxml;
    exports org.tiny.whiterun.services;
    opens org.tiny.whiterun.services to javafx.fxml;
    exports org.tiny.whiterun.models;
    opens org.tiny.whiterun.models to javafx.fxml;
}