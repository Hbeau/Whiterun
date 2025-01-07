package org.tiny.whiterun.services;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.json.JSONObject;
import org.tiny.whiterun.models.AssetsPack;

import java.io.IOException;
import java.nio.file.*;

public class DirectoryWatcherService extends Service<Void> {

    // Path to the directory being watched
    private final Path directoryToWatch;

    // Observable list to store the directory's files
    private final ObservableList<AssetsPack> fileList;

    /**
     * Constructor to initialize the service with the specified directory.
     *
     * @param directoryPath Path of the directory to watch.
     */
    public DirectoryWatcherService(String directoryPath) {
        this.directoryToWatch = Paths.get(directoryPath);

        if (!Files.isDirectory(this.directoryToWatch)) {
            throw new IllegalArgumentException("Invalid directory: " + directoryPath);
        }

        this.fileList = FXCollections.observableArrayList();
        loadInitialFiles();
    }

    /**
     * Loads the initial files from the directory into the observable list.
     */
    private void loadInitialFiles() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryToWatch)) {
            for (Path path : stream) {
                fileList.add(createAssetPack(path));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading initial files: " + e.getMessage(), e);
        }
    }

    private AssetsPack createAssetPack(Path path) {
        try {
            Path fileName = path.getFileName();
            String s = ZipUtils.getInstance().extractManifest(fileName);
            byte[] thumbnail = ZipUtils.getInstance().extractThumbnail(fileName);
            JSONObject jsonObject = new JSONObject(s);
            String name = jsonObject.getString("name");
            String description = jsonObject.getString("description");
            return new AssetsPack(name, description, thumbnail, fileName);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the observable list of files in the directory.
     *
     * @return ObservableList of file names.
     */
    public ObservableList<AssetsPack> getFileList() {
        return fileList;
    }

    /**
     * Creates a task to watch for changes in the directory.
     *
     * @return Task that monitors the directory for file events.
     */
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                monitorDirectory();
                return null;
            }
            /**
             * Monitors the directory for changes and updates the observable list accordingly.
             */
            private void monitorDirectory() {
                try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                    registerDirectory(watchService);

                    while (!isCancelled()) {
                        WatchKey key = waitForWatchKey(watchService);
                        if (key == null) break;

                        processWatchEvents(key);
                        if (!key.reset()) {
                            notifyDirectoryInaccessible();
                            break;
                        }
                    }
                } catch (IOException e) {
                    notifyError("Error monitoring directory: " + e.getMessage());
                }
            }

            /**
             * Registers the directory with the watch service for monitoring.
             *
             * @param watchService WatchService instance.
             * @throws IOException if an error occurs during registration.
             */
            private void registerDirectory(WatchService watchService) throws IOException {
                directoryToWatch.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
            }
            /**
             * Waits for the next watch key, handling interruptions.
             *
             * @param watchService WatchService instance.
             * @return WatchKey or null if interrupted or cancelled.
             */
            private WatchKey waitForWatchKey(WatchService watchService) {
                try {
                    return watchService.take();
                } catch (InterruptedException e) {
                    if (isCancelled()) {
                        return null;
                    }
                }
                return null;
            }

            /**
             * Processes the events for a given watch key.
             *
             * @param key  WatchKey to process.
             */
            private void processWatchEvents(WatchKey key) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // Ignore overflow events
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path fileName = pathEvent.context();

                    updateFileList(kind, fileName);
                }
            }

            /**
             * Updates the observable list based on the file event type.
             *
             * @param kind     The type of the event (create, delete, modify).
             * @param fileName The name of the file involved in the event.
             */
            private void updateFileList(WatchEvent.Kind<?> kind, Path fileName) {
                    Platform.runLater(() -> {
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            fileList.add(createAssetPack(fileName));
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            fileList.remove(createAssetPack(fileName));
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            updateMessage("File modified: " + fileName);
                        }
                    });
            }

            /**
             * Notifies that the directory is no longer accessible.
             */
            private void notifyDirectoryInaccessible() {
                Platform.runLater(() -> updateMessage("Directory is no longer accessible."));
            }

            /**
             * Notifies an error message.
             *
             * @param message Error message.
             */
            private void notifyError(String message) {
                Platform.runLater(() -> updateMessage(message));
            }
        };

    }
}
