package org.tiny.whiterun.controllers;


import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
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
                ZipUtils.getInstance().installPack(selectedAsset);
                GameDirManager.getInstance().registerInstallation(selectedAsset.toString());
            }
        }
    }
}
