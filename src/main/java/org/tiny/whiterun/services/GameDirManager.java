package org.tiny.whiterun.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tiny.whiterun.models.AssetsPack;
import org.tiny.whiterun.models.InstalledPack;
import org.tiny.whiterun.models.PackState;
import org.tiny.whiterun.models.PacksWrapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;


public class GameDirManager {

    private static final Logger log = LoggerFactory.getLogger(GameDirManager.class);
    public static final String ASSETS_PACK = "assets-pack";
    public static final String ASSETS = "assets";
    private static GameDirManager instance;

    private final Properties properties;

    public Path getUserPrefsFolder() {
        return userPrefsFolder;
    }

    private final Path userPrefsFolder;
    private final Path settings;

    private File gameRootPath;
    private final ObjectMapper objectMapper;

    private GameDirManager() {
        properties = new Properties();
        objectMapper = new ObjectMapper();

        String homeDir = System.getProperty("user.home");
        userPrefsFolder = Path.of(homeDir).resolve(".whiterun");

        try {
            if (!Files.exists(userPrefsFolder)) {
                Files.createDirectories(userPrefsFolder);
            }
            this.settings = userPrefsFolder.resolve("settings");
            if (Files.exists(settings)) {
                properties.load(new FileInputStream(settings.toFile()));
                String gameFolder = (String) properties.get("gameFolder");
                this.gameRootPath = Path.of(gameFolder).toFile();

            }
            Optional<String> gameFolder = new InstallationFinder().findGameFolder();
            gameFolder.ifPresent(path -> setGameRootPath(Path.of(path).toFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static GameDirManager getInstance() {
        if (instance == null) {
            instance = new GameDirManager();
        }
        return instance;
    }

    public String getGameRootPath() {
        if (gameRootPath != null) {
            return gameRootPath.getAbsolutePath();
        }
        return "";
    }

    public File getOrCreateAssetPackFolder() throws IOException {
        if (gameRootPath != null) {
            Path path = getAssetPackFolderPath();
            log.info("Get asset pack at Location {}", path);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Folder 'assets-pack' has bean created in {}", path);
            }
            return path.toFile();
        }
        throw new FileNotFoundException("Game root path is null");
    }

    public Path getAssetPackFolderPath() {
        return Paths.get(gameRootPath.getPath(), ASSETS_PACK);
    }

    public Path getAssetFolderPath() {
        return Paths.get(gameRootPath.getPath(), ASSETS);
    }

    public void setGameRootPath(File gameRootPath) {
        this.gameRootPath = gameRootPath;
        properties.setProperty("gameFolder", this.gameRootPath.getPath());
        storeProperties();
    }


    public Task<Void> patchGame() {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    log.info("Create assets pack folder");
                    updateMessage("Create assets pack folder");
                    getOrCreateAssetPackFolder();
                    Thread.sleep(500);
                    Path assetsBackup = userPrefsFolder.resolve("assets_backup");
                    log.info("Backup game assets folder");
                    updateMessage("Backup game assets folder (this might take a while)");
                    if (Files.exists(assetsBackup)) {
                        FileUtils.deleteDirectory(assetsBackup.toFile());
                    }
                    FileUtils.copyDirectory(getAssetFolderPath().toFile(), assetsBackup.toFile());
                    log.info("patch complete");
                    updateMessage("Finished");
                    Thread.sleep(500);
                    succeeded();
                } catch (IOException | InterruptedException e) {
                    log.error("Error while patching the game", e);
                    updateMessage("Error while patching the game" + e.getMessage());
                    failed();
                }
                return null;
            }
        };
    }

    public PackState checkPackInstallation(AssetsPack pack) {
        Optional<InstalledPack> packOptional = getInstalledPack(pack);
        if (packOptional.isEmpty()) {
            return PackState.NOT_INSTALLED;
        }
        Map<String, Boolean> fileWithChecksum = ZipUtils.getInstance().checkInstallation(packOptional.get().entries());
        log.info("some file are moved {}", fileWithChecksum.entrySet().stream().filter(stringBooleanEntry -> !stringBooleanEntry.getValue()).map(Map.Entry::getKey).toList());
        if (fileWithChecksum.containsValue(false)) {
            return PackState.COVERED;
        }
        return PackState.INSTALLED;
    }

    public Map<String, Boolean> getInstallationDetails(AssetsPack pack) {
        Optional<InstalledPack> packOptional = getInstalledPack(pack);
        if (packOptional.isEmpty()) {
            return Map.of();
        }
        return ZipUtils.getInstance().checkInstallation(packOptional.get().entries());
    }

    private Optional<InstalledPack> getInstalledPack(AssetsPack pack) {
        PacksWrapper packsWrapper;
        try {
            packsWrapper = getOrCreatePacks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return packsWrapper.packs().stream()
                .filter(installedPack -> installedPack.name().equals(pack.archivePath().getFileName().toString())).findFirst();
    }

    public void registerInstallation(InstalledPack pack) throws IOException {
        PacksWrapper packsWrapper = getOrCreatePacks();
        Optional<InstalledPack> first = packsWrapper.packs().stream()
                .filter(installedPack -> installedPack.name().equals(pack.name())).findFirst();
        if (first.isEmpty()) {
            packsWrapper.packs().add(pack);
        } else {
            first.get().entries().replaceAll((s, s2) -> pack.entries().get(s));
        }
        Path path = userPrefsFolder.resolve("installation.json");
        objectMapper.writeValue(path.toFile(), packsWrapper);
    }

    public void unregisterInstallation(AssetsPack pack) throws IOException {
        PacksWrapper packsWrapper = getOrCreatePacks();
        packsWrapper.packs().removeIf(installedPack -> installedPack.name().equals(pack.archivePath().getFileName().toString()));
        Path path = userPrefsFolder.resolve("installation.json");
        objectMapper.writeValue(path.toFile(), packsWrapper);
    }

    public PacksWrapper getOrCreatePacks() throws IOException {
        Path path = userPrefsFolder.resolve("installation.json");
        if (!Files.exists(path)) {
            Files.createFile(path);
            objectMapper.writeValue(path.toFile(), new PacksWrapper(List.of()));
        }
        return objectMapper.readValue(path.toFile(), PacksWrapper.class);
    }

    private void storeProperties() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(settings.toFile());
            properties.store(fileOutputStream, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
