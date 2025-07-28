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
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tiny.whiterun.models.AssetsPack;
import org.tiny.whiterun.models.InstalledPack;
import org.tiny.whiterun.models.PackCell;
import org.tiny.whiterun.models.PackState;
import org.tiny.whiterun.services.DirectoryWatcherService;
import org.tiny.whiterun.services.GameDirManager;
import org.tiny.whiterun.services.ZipUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.tiny.whiterun.services.ZipUtils.ASSETS_FOLDER;


public class AssetPacksController {

    private static final Logger log = LoggerFactory.getLogger(AssetPacksController.class);


    public ListView<AssetsPack> assetsList;
    DirectoryWatcherService watcherService;

    @FXML
    void initialize() {
        assetsList.setCellFactory(param -> {
            PackCell packCell = new PackCell();
            packCell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() == MouseButton.PRIMARY && (!packCell.isEmpty())) {
                    AssetsPack selectedAsset = packCell.getItem();
                    if (selectedAsset != null) {
                        try {
                            showDialog(selectedAsset);
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
        if (watcherService == null) {
            try {
                watcherService = new DirectoryWatcherService(GameDirManager.getInstance().getOrCreateAssetPackFolder().getPath());
                ObservableList<AssetsPack> fileList = watcherService.getFileList();
                assetsList.setItems(fileList);
                watcherService.start();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    private void showDialog(AssetsPack selectedAsset) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        PackState packState = selectedAsset.checkInstallation();
        switch (packState) {
            case PackState.NOT_INSTALLED -> {
                alert.setTitle("Install an new assets pack");
                alert.setHeaderText("Are you sure to want to install this pack ?");
                alert.setContentText("Install Assets Pack : " + selectedAsset.packDescriptor().name());
                alert.getDialogPane().getButtonTypes().setAll(
                        ButtonType.OK,
                        ButtonType.CANCEL
                );
                ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Install");
            }
            case COVERED -> {
                alert.setTitle("Re-install the assets pack");
                alert.setHeaderText("Some of asset were overwritten, do you want to re-install them?");
                alert.setContentText("Re-install Assets Pack : " + selectedAsset.packDescriptor().name());
                alert.getDialogPane().getButtonTypes().setAll(
                        ButtonType.NO,
                        ButtonType.OK,
                        ButtonType.CANCEL
                );
                ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("Re-install");
                Button unInstallButton = ((Button) alert.getDialogPane().lookupButton(ButtonType.NO));
                unInstallButton.setText("Uninstall");
                unInstallButton.setStyle("-fx-background-color: #bb2124; -fx-text-fill : #EFEFEF");
            }
            case INSTALLED -> {
                alert.setTitle("Remove the assets pack");
                alert.setHeaderText("Do you want to bring back original assets?");
                alert.setContentText("Remove Assets Pack : " + selectedAsset.packDescriptor().name());
                alert.getDialogPane().getButtonTypes().setAll(
                        ButtonType.NO,
                        ButtonType.CANCEL
                );
                Button unInstallButton = ((Button) alert.getDialogPane().lookupButton(ButtonType.NO));
                unInstallButton.setText("Uninstall");
                unInstallButton.setStyle("-fx-background-color: #bb2124; -fx-text-fill : #EFEFEF");
            }

        }
        TextFlow textFlow = new TextFlow();
        String zipContents = ZipUtils.getInstance().listZipContentsAsTree(selectedAsset.archivePath());
        Map<String, Boolean> installationDetails = GameDirManager.getInstance().getInstallationDetails(selectedAsset);
        for (String line : zipContents.split("\\r?\\n")) {
            String customAssetPath = line.substring((ASSETS_FOLDER + "/").length());
            Text e = new Text(customAssetPath + "\n");
            if (!installationDetails.getOrDefault(customAssetPath, true)) {
                e.setStyle("-fx-fill: #f0ad4e;");
            }
            textFlow.getChildren().add(e);

        }

        textFlow.setMaxWidth(Double.MAX_VALUE);
        textFlow.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textFlow, Priority.ALWAYS);
        GridPane.setHgrow(textFlow, Priority.ALWAYS);
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);

        expContent.add(textFlow, 0, 1);
        alert.getDialogPane().setExpandableContent(expContent);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == ButtonType.OK) {
                log.info("install the pack {}", selectedAsset);
                install(selectedAsset.archivePath());
            }
            if (result.get() == ButtonType.NO) {
                log.info("uninstall the pack {}", selectedAsset);
                uninstall(selectedAsset);
            }
        }
    }

    private void install(Path selectedAsset) {
        Task<InstalledPack> installedPackTask = ZipUtils.getInstance().installPack(selectedAsset);
        Stage progressDialog = createProgressDialog(installedPackTask);

        new Thread(installedPackTask).start();

        installedPackTask.setOnSucceeded(event -> {
            progressDialog.close();
            log.info("Asset pack installed successfully!");
            showAlert("Success", "Asset pack installed successfully!");
            try {
                GameDirManager.getInstance().registerInstallation(installedPackTask.get());
            } catch (InterruptedException | ExecutionException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        installedPackTask.setOnFailed(event -> {
            log.error("An error occurred while installing the pack", installedPackTask.getException());
            progressDialog.close();
            showAlert("Error", "An error occurred while installing the pack: " + installedPackTask.getException().getMessage());
        });

        installedPackTask.setOnCancelled(event -> progressDialog.close());
    }

    private void uninstall(AssetsPack assetsPack) {
        Task<Void> uninstallTask = ZipUtils.getInstance().uninstallPack(assetsPack);
        Stage progressDialog = createProgressDialog(uninstallTask);

        new Thread(uninstallTask).start();

        uninstallTask.setOnSucceeded(event -> {
            progressDialog.close();
            log.info("Asset pack uninstalled successfully!");
            showAlert("Success", "Asset pack uninstalled successfully!");
        });

        uninstallTask.setOnFailed(event -> {
            log.error("An error occurred while uninstalling the pack", uninstallTask.getException());
            progressDialog.close();
            showAlert("Error", "An error occurred while uninstalling the pack: " + uninstallTask.getException().getMessage());
        });

        uninstallTask.setOnCancelled(event -> progressDialog.close());

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
