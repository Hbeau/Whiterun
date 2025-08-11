package org.tiny.whiterun.services;

import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.tiny.whiterun.services.GameDirectories.ASSETS;
import static org.tiny.whiterun.services.GameDirectories.COMPILED_ASSETS;

public class GameAssetsService {
    private static final Logger log = LoggerFactory.getLogger(GameAssetsService.class);

    private final UserPreferencesService userPreferencesService;
    private final GameDirManager gameDirectories;

    private GameAssetsService(UserPreferencesService userPreferencesService, GameDirManager gameDirectories) {
        this.userPreferencesService = userPreferencesService;
        this.gameDirectories = gameDirectories;
    }

    private static GameAssetsService INSTANCE;

    public static synchronized GameAssetsService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GameAssetsService(UserPreferencesService.getInstance(), GameDirManager.getInstance());
        }
        return INSTANCE;
    }

    public Task<Void> patchGame() {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    log.info("Create assets pack folder");
                    updateMessage("Create assets pack folder");
                    gameDirectories.getGameDirectories().getOrCreateAssetPackFolder();
                    Thread.sleep(500);
                    log.info("Backup game assets folder");
                    Path assetsBackup = userPreferencesService.getGameBackupFolder();
                    if (Files.exists(assetsBackup)) {
                        FileUtils.deleteDirectory(assetsBackup.toFile());
                    }
                    backupAssetsFolder(assetsBackup);
                    backupCompliedAssetsFolder(assetsBackup);

                    log.info("patch complete");
                    updateMessage("Finished");
                    Thread.sleep(500);
                    succeeded();
                } catch (IOException | InterruptedException e) {
                    log.error("Error while patching the game", e);
                    updateMessage("Error while patching the game" + e.getMessage());
                    throw new RuntimeException("Error while patching the game", e);
                }
                return null;
            }

            private void backupAssetsFolder(Path assetsBackup) throws IOException {
                updateMessage("Backup game assets folder (this might take a while)");
                Path sourceAssets = gameDirectories.getGameDirectories().getAssetFolderPath();
                if (!Files.exists(sourceAssets)) {
                    updateMessage("Game assets folder does not exist: " + sourceAssets);
                    throw new RuntimeException("Game assets folder does not exist: " + sourceAssets);
                }
                //Remove audio because folder is too heavy and not editable
                FileFilter filter = file -> !file.getPath().contains("audio");
                FileUtils.copyDirectory(sourceAssets.toFile(), assetsBackup.resolve(ASSETS).toFile(), filter);
            }
            private void backupCompliedAssetsFolder(Path assetsBackup) throws IOException {
                updateMessage("Backup game compiled assets folder (this might take a while)");
                Path sourceCompiledAssets = gameDirectories.getGameDirectories().getCompiledAssetFolderPath();
                if (!Files.exists(sourceCompiledAssets)) {
                    updateMessage("Game compiled assets folder does not exist: " + sourceCompiledAssets);
                    throw new RuntimeException("Game compiled assets folder does not exist: " + sourceCompiledAssets);
                }
                FileUtils.copyDirectory(sourceCompiledAssets.toFile(), assetsBackup.resolve(COMPILED_ASSETS).toFile());

            }
        };
    }
}