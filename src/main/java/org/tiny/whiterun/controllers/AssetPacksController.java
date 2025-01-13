package org.tiny.whiterun.controllers;


import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tiny.whiterun.models.AssetsPack;
import org.tiny.whiterun.models.PackCell;
import org.tiny.whiterun.services.DirectoryWatcherService;
import org.tiny.whiterun.services.GameDirManager;
import org.tiny.whiterun.services.ZipUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class AssetPacksController {

    private static final Logger log = LoggerFactory.getLogger(AssetPacksController.class);


    public ListView<AssetsPack> assetsList;

    @FXML
    void initialize() {
        assetsList.setCellFactory(param -> {
            PackCell packCell = new PackCell();
            packCell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() == MouseButton.PRIMARY && (!packCell.isEmpty())) {
                    AssetsPack selectedAsset = packCell.getItem();
                    if (selectedAsset != null) {
                        try {
                            showDialog(selectedAsset.getArchivePath());
                        } catch (IOException e) {
                            log.error("error while installing assets", e);
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            return packCell;
        });
        assetsList.prefHeight(64);
    }
    public void watch() {
        try {
            DirectoryWatcherService watcherService = new DirectoryWatcherService(GameDirManager.getInstance().getAssetPackFolder().getPath());

            ObservableList<AssetsPack> fileList = watcherService.getFileList();
            assetsList.setItems(fileList);

            watcherService.start();
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    private void showDialog(Path selectedAsset) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Install an new assets pack");
        alert.setHeaderText("Are you sure to want to install this pack");
        alert.setContentText("Install asset :" + selectedAsset);
        TextArea textArea = new TextArea(ZipUtils.getInstance().listZipContentsAsTree(selectedAsset));
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);

        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == ButtonType.OK) {
                log.info("install the pack {}", selectedAsset);
                install(selectedAsset);
            }
        }
    }

    private void install(Path selectedAsset) {
        Task<Void> voidTask = ZipUtils.getInstance().installPack(selectedAsset);
        Stage progressDialog = createProgressDialog(voidTask);

        new Thread(voidTask).start();

        voidTask.setOnSucceeded(event -> {
            progressDialog.close();
            log.info("Asset pack installed successfully!");
            showAlert("Success", "Asset pack installed successfully!");
            GameDirManager.getInstance().registerInstallation(selectedAsset.toString());
        });

        voidTask.setOnFailed(event -> {
            log.error("An error occurred while installing the pack", voidTask.getException());
            progressDialog.close();
            showAlert("Error", "An error occurred while installing the pack: " + voidTask.getException().getMessage());
        });

        voidTask.setOnCancelled(event -> progressDialog.close());

        GameDirManager.getInstance().registerInstallation(selectedAsset.toString());
    }

    private Stage createProgressDialog(Task<?> task) {

        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Processing");

        // Create the UI
        ProgressBar progressBar = new ProgressBar();
        progressBar.autosize();
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.setMaxWidth(300);
        Text progressText = new Text("Processing...");
        progressText.textProperty().bind(task.messageProperty());

        VBox layout = new VBox(10, progressText, progressBar);
        layout.setFillWidth(true);
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
