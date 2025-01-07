package org.tiny.whiterun.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class GameDirManager {
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
        System.out.println(gameRootPath);
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
            return Paths.get(gameRootPath.getPath(), ASSETS_PACK).toFile();
        }
        throw new FileNotFoundException("Le fichier 'manifest.json' est introuvable dans le dossier.");
    }

    public void setGameRootPath(File gameRootPath) {
        this.gameRootPath = gameRootPath;
        properties.setProperty("gameFolder", this.gameRootPath.getPath());
        storeProperties();
    }


    public void patchGame() {
        if (patched) {
            return;
        }
        try {
            cleanManifest();
            createAssetsPack();
            this.patched = true;
            properties.setProperty("patched", String.valueOf(true));
            storeProperties();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void cleanManifest() throws Exception {

        Path manifestPath = Paths.get(gameRootPath.getPath(), "manifest.json");

        if (!Files.isRegularFile(manifestPath)) {
            throw new FileNotFoundException("Le fichier 'manifest.json' est introuvable dans le dossier " + gameRootPath + ".");
        }

        // Lire le fichier manifest.json
        String manifestContent = new String(Files.readAllBytes(manifestPath));
        JSONObject manifestData = getJsonObject(manifestContent);

        // Réécrire le fichier manifest.json
        try (BufferedWriter writer = Files.newBufferedWriter(manifestPath)) {
            writer.write(manifestData.toString(4));
        }

        System.out.println("Le fichier manifest.json a été nettoyé et mis à jour dans " + gameRootPath + ".");
    }

    private static JSONObject getJsonObject(String manifestContent) {
        JSONObject manifestData = new JSONObject(manifestContent);

        if (!manifestData.has("files")) {
            throw new IllegalArgumentException("Le fichier manifest.json ne contient pas de clé 'files'.");
        }

        // Filtrer les fichiers pour exclure ceux contenant '/assets/'
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

    private void createAssetsPack() {
        try {
            Path assetsPath = Paths.get(gameRootPath.getPath(), "assets");
            Path assetsPackPath = Paths.get(gameRootPath.getPath(), ASSETS_PACK);

            if (!Files.isDirectory(assetsPath)) {
                throw new FileNotFoundException("Le dossier 'assets' est introuvable dans " + gameRootPath + ".");
            }

            if (!Files.exists(assetsPackPath)) {
                Files.createDirectories(assetsPackPath);
                System.out.println("Dossier 'assets-pack' créé à l'emplacement " + assetsPackPath + ".");
            }

            Path zipFilePath = assetsPackPath.resolve("default.zip");

            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFilePath));
                 Stream<Path> walk = Files.walk(assetsPath)) {
                walk
                        .filter(path -> !path.toString().contains("audio")) // Exclure le dossier 'audio'
                        .forEach(path -> {
                            try {
                                if (Files.isRegularFile(path)) {
                                    String zipEntryName = "assets\\" + assetsPath.relativize(path);
                                    zipOut.putNextEntry(new ZipEntry(zipEntryName));
                                    Files.copy(path, zipOut);
                                    zipOut.closeEntry();
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Erreur lors de la compression du fichier " + path + ": " + e.getMessage(), e);
                            }
                        });
                Path thumbnailPath = Path.of(Objects.requireNonNull(getClass().getResource("default_thumbnail.jpg")).toURI());
                zipOut.putNextEntry(new ZipEntry("thumbnail.jpg"));
                zipOut.write(new FileInputStream(thumbnailPath.toFile()).readAllBytes());
                zipOut.closeEntry();
                Path manifestPath = Path.of(Objects.requireNonNull(getClass().getResource("manifest.json")).toURI());
                zipOut.putNextEntry(new ZipEntry("manifest.json"));
                zipOut.write(new FileInputStream(manifestPath.toFile()).readAllBytes());
                zipOut.closeEntry();
            }
            System.out.println("Dossier 'assets' compressé et enregistré dans '" + zipFilePath + "'.");

        } catch (Exception e) {
            System.err.println("Erreur lors de la création du pack d'assets : " + e.getMessage());
        }
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
