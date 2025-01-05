package org.tiny.whiterun;


import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.IOException;
import java.util.Optional;

public class AssetPacksController {


    public ListView<String> assetsList;

    public void watch() {
        try {
            DirectoryWatcherService watcherService = new DirectoryWatcherService(GameDirManager.getInstance().getAssetPackFolder().getPath());

            // Lier les messages de mise à jour au label
            ObservableList<String> fileList = watcherService.getFileList();
            assetsList.setItems(fileList);

            // Démarrer le service
            watcherService.start();
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
    @FXML
    public void onClicked(MouseEvent _mouseEvent) throws IOException {
        String selectedAsset = assetsList.getSelectionModel().getSelectedItem();
        showDialog(selectedAsset);
    }
    private void showDialog(String selectedAsset) throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("Look, a Confirmation Dialog");
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

// Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == ButtonType.OK) {
                System.out.println("ok");
                ZipUtils.getInstance().unzip(selectedAsset);
            } else {
                System.out.println("no");
            }
        }
    }
}
