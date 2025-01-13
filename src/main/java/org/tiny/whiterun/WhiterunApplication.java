package org.tiny.whiterun;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.tiny.whiterun.controllers.MainController;
import org.tiny.whiterun.services.GameDirManager;

import java.io.IOException;
import java.util.Objects;

public class WhiterunApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 960, 540);
        scene.getStylesheets().addAll(Objects.requireNonNull(this.getClass().getResource("style.css")).toExternalForm());
        stage.setTitle("Whiterun");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        MainController controller = fxmlLoader.getController();

        controller.configPaneController.pathField.setText(GameDirManager.getInstance().getGameRootPath());
    }

    public static void main(String[] args) {
        launch();
    }
}