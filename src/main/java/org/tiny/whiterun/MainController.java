package org.tiny.whiterun;

import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;

public class MainController {

    public Pane configPane;
    @FXML
    AssetPacksController assetPaneController;
    @FXML
    ConfigController configPaneController;

    @FXML
    void initialize(){
        if(GameDirManager.getInstance().isPatched()) {
            assetPaneController.watch();
        }
        configPane.addEventHandler(ConfigController.OPTIONS_ALL, myEvent -> {
            if(GameDirManager.getInstance().isPatched()) {
                assetPaneController.watch();
            }
        });
    }
}
