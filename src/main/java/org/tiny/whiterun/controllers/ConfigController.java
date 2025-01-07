package org.tiny.whiterun.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.tiny.whiterun.models.PatchedEvent;
import org.tiny.whiterun.services.GameDirManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ConfigController {

    public TextField pathField;
    public Pane config;
    public static EventType<PatchedEvent> OPTIONS_ALL = new EventType<>("OPTIONS_ALL");
    @FXML
    protected void onBrowseForRoot() {
        DirectoryChooser fileChooser = new DirectoryChooser();
        File file = fileChooser.showDialog(null);
        GameDirManager instance = GameDirManager.getInstance();
        instance.setGameRootPath(file);
        pathField.setText(instance.getGameRootPath());
    }

    @FXML
    protected void onPatchGameClicked(){
        Task<Void> longRunningTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                GameDirManager instance = GameDirManager.getInstance();
                updateMessage("coucou");
                instance.patchGame();
                updateMessage("finished");
                return null;
            }
        };
        Stage progressDialog = createProgressDialog(longRunningTask);

        // Step 3: Run the Task in the Background
        new Thread(longRunningTask).start();

        // Step 4: Handle Task Completion
        longRunningTask.setOnSucceeded(event -> {
            progressDialog.close();

            showAlert("Success", "Game Patched successfully!");
            config.fireEvent(new PatchedEvent(OPTIONS_ALL));
        });

        longRunningTask.setOnFailed(event -> {
            progressDialog.close();
            showAlert("Error", "An error occurred: " + longRunningTask.getException().getMessage());
        });

        longRunningTask.setOnCancelled(event -> progressDialog.close());

    }
    @FXML
    protected void onAddAssetsPackClicked() throws IOException {
        System.out.println("clicked");
        GameDirManager instance = GameDirManager.getInstance();
        Desktop.getDesktop().open(instance.getAssetPackFolder());
    }

    private Stage createProgressDialog(Task<?> task) {

        Stage stage = new Stage(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Processing");

        // Create the UI
        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(task.progressProperty());

        Text progressText = new Text("Processing...");
        progressText.textProperty().bind(task.messageProperty());

        VBox layout = new VBox(10, progressText, progressBar);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #f4f4f4; -fx-border-color: #ccc;");
        Scene scene = new Scene(layout);

        stage.setScene(scene);
        stage.setWidth(300);
        stage.setHeight(150);

        stage.show();
        return stage;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

}