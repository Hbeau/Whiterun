package org.tiny.whiterun.models;

import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.ByteArrayInputStream;

public class PackCell extends ListCell<AssetsPack> {
    private final HBox content;
    private final ImageView thumbnail;
    private final Text title;
    private final Text installed;
    private final Text subtitle;

    public PackCell() {
        thumbnail = new ImageView();
        thumbnail.setFitWidth(50);
        thumbnail.setFitHeight(50);

        title = new Text();
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        installed = new Text();
        installed.setStyle("-fx-font-size: 12px; -fx-font-style: italic;-fx-text-align: right;");

        subtitle = new Text();
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");

        VBox textContainer = new VBox(title, subtitle);
        textContainer.setSpacing(5);

        content = new HBox(thumbnail, textContainer, installed);
        HBox.setHgrow(textContainer, Priority.ALWAYS);
        content.setSpacing(10);
        content.setPadding(new Insets(5));

    }

    @Override
    protected void updateItem(AssetsPack item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("-fx-opacity : 0");
        } else {
            setStyle("-fx-opacity : 1");
            switch (item.checkInstallation()) {
                case INSTALLED -> {
                    content.setStyle("-fx-background-color: rgba(240, 240, 250, 0.40)");
                    installed.setText("installed");
                }
                case COVERED -> {
                    content.setStyle("-fx-background-color: rgba(240, 170, 110, 0.40)");
                    installed.setText("Installation error");
                }
            }
            Image img = new Image(new ByteArrayInputStream(item.image()));
            thumbnail.setImage(img);
            title.setText(item.packDescriptor().name());
            subtitle.setText(item.packDescriptor().description());
            setGraphic(content);
        }
    }


}
