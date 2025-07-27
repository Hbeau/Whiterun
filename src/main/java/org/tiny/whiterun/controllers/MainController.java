package org.tiny.whiterun.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.tiny.whiterun.services.GameDirManager;

import java.nio.file.Files;

public class MainController {

    public Pane configPane;
    @FXML
    public AssetPacksController assetPaneController;
    @FXML
    public ConfigController configPaneController;
    public VBox boxTop;

    @FXML
    void initialize(){

        enableWatcher();
        configPane.addEventHandler(ConfigController.OPTIONS_ALL, patchedEvent -> {
            enableWatcher();
        });
    }

    private void enableWatcher() {
        if (Files.exists(GameDirManager.getInstance().getAssetPackFolderPath())) {
            assetPaneController.watch();
        }
    }
}
