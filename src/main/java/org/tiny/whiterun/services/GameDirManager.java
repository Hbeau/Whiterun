package org.tiny.whiterun.services;

import javafx.concurrent.Task;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class GameDirManager {

    private static final Logger log = LoggerFactory.getLogger(GameDirManager.class);
    public static final String ASSETS_PACK = "assets-pack";
    private static GameDirManager instance;

    private final Properties properties;

    private final Path settings;

    private boolean patched;

    private File gameRootPath;

    private GameDirManager() {
        properties = new Properties();

        String homeDir = System.getProperty("user.home");
        Path userPrefs = Path.of(homeDir).resolve(".whiterun");

        try {
            if (!Files.exists(userPrefs)) {
                Files.createDirectories(userPrefs);
            }
            this.settings = userPrefs.resolve("settings");
            if (Files.exists(settings)) {
                properties.load(new FileInputStream(settings.toFile()));
                String gameFolder = (String) properties.get("gameFolder");
                this.gameRootPath = Path.of(gameFolder).toFile();
                this.patched = Boolean.parseBoolean(properties.getProperty("patched", "false"));
            } else {
                this.patched = false;
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

    public boolean isPatched() {
        return patched;
    }

    public File getAssetPackFolder() throws FileNotFoundException {
        if (gameRootPath != null) {
            Path path = Paths.get(gameRootPath.getPath(), ASSETS_PACK);
            log.info("Get asset pack at Location {}", path);
            return path.toFile();
        }
        throw new FileNotFoundException("Game root path is null");
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
                if (patched) {
                    return null;
                }
                try {
                    log.info("Clean Manifest file");
                    updateMessage("Clean Manifest file");
                    cleanManifest();
                    Thread.sleep(500);
                    log.info("Create default assets pack");
                    updateMessage("Create default assets pack (this might take a while)");
                    Path assetPath = Paths.get(gameRootPath.getPath(), "assets");
                    ZipUtils.getInstance().createAssetsPack(assetPath);
                    patched = true;
                    properties.setProperty("patched", String.valueOf(true));
                    storeProperties();
                    log.info("patch complete");
                    updateMessage("Finished");
                    Thread.sleep(500);

                    return null;
                } catch (IOException | InterruptedException e) {
                    log.error("Error while patching the game", e);
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void cleanManifest() throws IOException {

        Path manifestPath = Paths.get(gameRootPath.getPath(), "build-info/manifest.json");

        if (!Files.isRegularFile(manifestPath)) {
            throw new FileNotFoundException("The file 'manifest.json' was not found in the folder" + gameRootPath + ".");
        }

        String manifestContent = new String(Files.readAllBytes(manifestPath));
        JSONObject manifestData = getJsonObject(manifestContent);

        try (BufferedWriter writer = Files.newBufferedWriter(manifestPath)) {
            writer.write(manifestData.toString(4));
        }

        log.info("Manifest file has bean patched in {}.", gameRootPath);
    }

    private static JSONObject getJsonObject(String manifestContent) {
        JSONObject manifestData = new JSONObject(manifestContent);

        if (!manifestData.has("files")) {
            throw new IllegalArgumentException("The manifest.json file does not contain a 'files' key.");
        }

        JSONArray files = manifestData.getJSONArray("files");
        JSONArray filteredFiles = new JSONArray();

        for (int i = 0; i < files.length(); i++) {
            Object entry = files.get(i);
            if (entry instanceof String && !((String) entry).contains("assets/")) {
                filteredFiles.put(entry);
            }
        }

        manifestData.put("files", filteredFiles);
        return manifestData;
    }

    public List<String> getInstalledPack() {
        String property = properties.getProperty("installed", "");
        return Arrays.asList(property.split(","));
    }

    public void registerInstallation(String pack) {
        String property = properties.getProperty("installed", "");
        List<String> installedPacks = new ArrayList<>(List.of(property.split(",")));
        if (!installedPacks.contains(pack)) {
            installedPacks.add(pack);
        }
        String joined = String.join(",", installedPacks);
        properties.put("installed", joined);
        storeProperties();
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
