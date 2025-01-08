package org.tiny.whiterun.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import org.tiny.whiterun.services.GameDirManager;

public class MainController {

    public Pane configPane;
    @FXML
    public AssetPacksController assetPaneController;
    @FXML
    public ConfigController configPaneController;

    @FXML
    void initialize(){
        if(GameDirManager.getInstance().isPatched()) {
            assetPaneController.watch();
        }
        configPane.addEventHandler(ConfigController.OPTIONS_ALL, patchedEvent -> {
            if(GameDirManager.getInstance().isPatched()) {
                assetPaneController.watch();
            }
        });
    }
}
