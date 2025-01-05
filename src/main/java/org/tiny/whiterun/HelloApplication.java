package org.tiny.whiterun;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 580);
        stage.setTitle("Hello!");
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