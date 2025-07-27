module org.tiny.whiterun {
    requires com.fasterxml.jackson.core;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires javafx.fxml;
    requires org.apache.commons.io;
    requires org.slf4j;
    requires atlantafx.base;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires org.apache.commons.codec;


    opens org.tiny.whiterun to javafx.fxml;
    exports org.tiny.whiterun;
    exports org.tiny.whiterun.controllers;
    opens org.tiny.whiterun.controllers to javafx.fxml;
    exports org.tiny.whiterun.services;
    opens org.tiny.whiterun.services to javafx.fxml;
    exports org.tiny.whiterun.models;
    opens org.tiny.whiterun.models to javafx.fxml;
    exports org.tiny.whiterun.exceptions;
}