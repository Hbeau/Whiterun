package org.tiny.whiterun;

import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ConfigController {

    public TextField pathField;
    public Pane config;
    public static EventType<MyEvent> OPTIONS_ALL = new EventType<>("OPTIONS_ALL");
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
        GameDirManager instance = GameDirManager.getInstance();
        instance.patchGame();
        config.fireEvent(new MyEvent(OPTIONS_ALL));

    }
    @FXML
    protected void onAddAssetsPackClicked() throws IOException {
        System.out.println("clicked");
        GameDirManager instance = GameDirManager.getInstance();
        Desktop.getDesktop().open(instance.getAssetPackFolder());
    }

}