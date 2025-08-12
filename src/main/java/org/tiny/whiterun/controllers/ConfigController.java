package org.tiny.whiterun.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tiny.whiterun.models.NewPackForm;
import org.tiny.whiterun.models.PatchedEvent;
import org.tiny.whiterun.services.GameAssetsService;
import org.tiny.whiterun.services.GameDirManager;
import org.tiny.whiterun.services.ZipUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

public class ConfigController {

    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    public TextField pathField;
    public Pane config;
    public static EventType<PatchedEvent> OPTIONS_ALL = new EventType<>("OPTIONS_ALL");

    @FXML
    void initialize() {
        pathField.setText(GameDirManager.getInstance().getGameDirectories().getGameRootPath());
    }

    @FXML
    protected void onBrowseForRoot() {
        DirectoryChooser fileChooser = new DirectoryChooser();
        File file = fileChooser.showDialog(null);
        if (file != null) {
            GameDirManager instance = GameDirManager.getInstance();
            instance.setGameRootPath(file.toPath());
            pathField.setText(instance.getGameDirectories().getGameRootPath());
        }
    }

    @FXML
    protected void onPatchGameClicked() {

        GameAssetsService instance = GameAssetsService.getInstance();

        Task<Void> longRunningTask = instance.patchGame();

        Stage progressDialog = createProgressDialog(longRunningTask);

        new Thread(longRunningTask).start();

        longRunningTask.setOnSucceeded(event -> {
            progressDialog.close();
            log.info("Game Patched successfully!");
            showAlert("Success", "Game Patched successfully!");
            config.fireEvent(new PatchedEvent(OPTIONS_ALL));
        });

        longRunningTask.setOnFailed(event -> {
            log.error("An error occurred while patching the game", longRunningTask.getException());
            progressDialog.close();
            showAlert("Error", "An error occurred: " + longRunningTask.getException().getMessage());
        });

        longRunningTask.setOnCancelled(event -> progressDialog.close());

    }

    @FXML
    protected void onAddAssetsPackClicked() throws IOException {
        GameDirManager instance = GameDirManager.getInstance();
        Desktop.getDesktop().open(instance.getGameDirectories().getOrCreateAssetPackFolder());
    }

    private Stage createProgressDialog(Task<?> task) {

        Stage stage = new Stage(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Processing");

        // Create the UI
        ProgressBar progressBar = new ProgressBar();
        progressBar.autosize();
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.setMaxWidth(350);
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
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @FXML
    protected void onCreatePack() {
        Optional<NewPackForm> assetsPack = showCreatePackDialog();
        if (assetsPack.isPresent()) {
            Task<Void> createPackTask = new Task<>() {
                @Override
                protected Void call() {
                    updateMessage("Creating assets pack");
                    ZipUtils.getInstance().createAssetsPack(assetsPack.get());
                    return null;
                }
            };
            Stage progressDialog = createProgressDialog(createPackTask);
            new Thread(createPackTask).start();
            createPackTask.setOnSucceeded(event -> {
                progressDialog.close();
                log.info("Assets Pack created successfully!");
                showAlert("Success", "Assets Pack created successfully!");
                config.fireEvent(new PatchedEvent(OPTIONS_ALL));
            });

            createPackTask.setOnFailed(event -> {
                log.error("An error occurred while creating an Assets Pack", createPackTask.getException());
                progressDialog.close();
                showAlert("Error", "An error occurred: " + createPackTask.getException().getMessage());
            });

            createPackTask.setOnCancelled(event -> progressDialog.close());
        }
    }


    private Optional<NewPackForm> showCreatePackDialog() {
        Dialog<NewPackForm> dialog = new Dialog<>();
        dialog.setTitle("Create new Assets Pack");
        dialog.setHeaderText("Fill in the form below:");

        ButtonType submitButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(20);
        grid.getColumnConstraints().addAll(col1);

        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        // 1. Folder input
        TextField folderField = new TextField();

        folderField.setPromptText("Select a folder...");
        folderField.setMaxWidth(Double.MAX_VALUE);
        folderField.setEditable(false);
        Button folderBtn = new Button("Browse...");
        folderBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File folder = dc.showDialog(dialog.getOwner());
            if (folder != null) {
                folderField.setText(folder.getAbsolutePath());
            }
        });
        HBox folderBox = new HBox(5, folderField, folderBtn);
        folderBox.setHgrow(folderField,Priority.ALWAYS);
        // 2. Image input with preview
        TextField imageField = new TextField();
        imageField.setEditable(false);
        imageField.setPromptText("Select an image...");
        Button imageBtn = new Button("Browse...");
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(64);
        imagePreview.setFitHeight(64);
        imagePreview.setPreserveRatio(true);
        InputStream defaultThumbnail = getClass().getResourceAsStream("/org/tiny/whiterun/services/default_thumbnail.jpg");
        imagePreview.setImage(new Image(defaultThumbnail));
        imageBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File file = fc.showOpenDialog(dialog.getOwner());
            if (file != null) {
                imageField.setText(file.getAbsolutePath());
                imagePreview.setImage(new Image(file.toURI().toString()));
            }
        });
        HBox imageBox = new HBox(5,imagePreview, imageField, imageBtn );
        imageBox.setAlignment(Pos.CENTER_LEFT);
        imageBox.setHgrow(imageField,Priority.ALWAYS);

        // 3. Title and description
        TextField titleField = new TextField();
        titleField.setPromptText("Enter title...");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Enter description...");
        descriptionArea.setPrefRowCount(2);

        // 4. Authors list input
        ListView<String> authorsList = new ListView<>();
        authorsList.setPrefHeight(100);
        TextField authorInput = new TextField();
        authorInput.setPromptText("Author name");
        Button addAuthorBtn = new Button("Add");
        addAuthorBtn.setOnAction(e -> {
            String name = authorInput.getText().trim();
            if (!name.isEmpty()) {
                authorsList.getItems().add(name);
                authorInput.clear();
            }
        });
        HBox authorInputBox = new HBox(5, authorInput, addAuthorBtn);

        // Add everything to the grid
        grid.add(new Label("Folder:"), 0, 0);
        grid.add(folderBox, 1, 0);

        grid.add(new Label("Image:"), 0, 1);
        grid.add(imageBox, 1, 1);

        grid.add(new Label("Title:"), 0, 2);
        grid.add(titleField, 1, 2);

        grid.add(new Label("Description:"), 0, 3);
        grid.add(descriptionArea, 1, 3);

        grid.add(new Label("Authors:"), 0, 4);
        grid.add(authorsList, 1, 4);

        grid.add(authorInputBox, 1, 5);

        Text errorMessage = new Text();
        errorMessage.setStyle("-fx-fill: #ff0000;");

        grid.add(errorMessage,1,6);


        dialog.getDialogPane().setContent(grid);

        final Button okButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        okButton.addEventFilter(ActionEvent.ACTION, ae -> {

            File image = imageField.getText().isBlank() ? null : new File(imageField.getText());
            NewPackForm newPackForm = new NewPackForm(Path.of(folderField.getText()), titleField.getText(), descriptionArea.getText(),
                    image, authorsList.getItems().toArray(new String[0]));
            if (!newPackForm.isValid()) {
                errorMessage.setText(newPackForm.validate());
                ae.consume(); //not valid
            }
        });

        dialog.setResultConverter(buttonType -> {
            if(buttonType.getButtonData().isCancelButton()){
                return null;
            }
            return new NewPackForm(Path.of(folderField.getText()), titleField.getText(), descriptionArea.getText(),
                new File(imageField.getText()), authorsList.getItems().toArray(new String[0]));});
        return dialog.showAndWait();
    }

}