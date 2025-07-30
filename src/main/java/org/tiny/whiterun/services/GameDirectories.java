package org.tiny.whiterun.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GameDirectories {

    private static final Logger log = LoggerFactory.getLogger(GameDirectories.class);
    public static final String ASSETS_PACK = "assets-pack";
    public static final String ASSETS = "assets";

    private File gameRootPath;

    public GameDirectories(File gameRootPath) {
        this.gameRootPath = gameRootPath;
    }

    public GameDirectories() {
    }

    public String getGameRootPath() {
        return gameRootPath != null ? gameRootPath.getAbsolutePath() : "";
    }

    public Path getAssetPackFolderPath() {
        return Paths.get(gameRootPath.getPath(), ASSETS_PACK);
    }

    public Path getAssetFolderPath() {
        return Paths.get(gameRootPath.getPath(), ASSETS);
    }

    public File getOrCreateAssetPackFolder() throws IOException {
        if (gameRootPath != null) {
            Path path = getAssetPackFolderPath();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Folder 'assets-pack' has bean created in {}", path);
            }
            return path.toFile();
        }
        throw new FileNotFoundException("Game root path is not found");
    }
}