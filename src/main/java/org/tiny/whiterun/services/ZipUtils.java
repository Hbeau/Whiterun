package org.tiny.whiterun.services;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static ZipUtils instance;

    private ZipUtils() {

    }

    /**
     * Retourne l'instance unique de ZipUtils.
     * Crée l'instance si elle n'existe pas encore.
     *
     * @return L'instance unique de ZipUtils.
     */
    public static synchronized ZipUtils getInstance() {
        if (instance == null) {
            instance = new ZipUtils();
        }
        return instance;
    }

    /**
     * Décompresse un fichier ZIP dans un dossier de destination.
     * Les fichiers existants seront écrasés.
     *
     * @param zipFilePath Chemin du fichier ZIP à décompresser.
     * @throws IOException Si une erreur survient lors de la décompression.
     */
    public void installPack(Path zipFilePath) throws IOException {

        Path destDirPath = Paths.get(GameDirManager.getInstance().getGameRootPath()).resolve("assets");
        Path assetPackFile = GameDirManager.getInstance().getAssetPackFolder().toPath().resolve(zipFilePath);
        if (!Files.exists(destDirPath)) {
            Files.createDirectories(destDirPath);
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(assetPackFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                if (!entry.getName().startsWith("assets/")) {
                    continue; // Ignore les fichiers en dehors du dossier `assets`
                }

                Path filePath = destDirPath.resolve(entry.getName().substring("assets/".length()));

                if (entry.isDirectory()) {
                    // Crée les sous-dossiers
                    if (!Files.exists(filePath)) {
                        Files.createDirectories(filePath);
                    }
                } else {
                    // Crée le dossier parent si nécessaire
                    Files.createDirectories(filePath.getParent());

                    // Écrit le fichier, en écrasant s'il existe
                    try (OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Exemple d'une autre méthode utile à ajouter.
     */
    public void compress(String sourceDirPath, String zipFilePath) throws IOException {
        Path sourceDir = Paths.get(sourceDirPath);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath)); Stream<Path> walk = Files.walk(sourceDir)) {
            walk.filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("Erreur lors de la compression : " + e.getMessage(), e);
                        }
                    });
        }
    }

    public String listZipContentsAsTree(Path zipFilePath) throws IOException {
        StringBuilder treeBuilder = new StringBuilder();
        try (ZipFile zipFile = new ZipFile(GameDirManager.getInstance().getAssetPackFolder().toPath().resolve(zipFilePath).toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith("assets/")) {
                    continue;
                }
                String path = entry.getName().replace("\\", "/");
                treeBuilder.append(path).append("\n");
            }
        }

        return treeBuilder.toString();
    }

    public String extractManifest(Path zipFilePath) throws IOException {
        File zipFile = GameDirManager.getInstance().getAssetPackFolder().toPath().resolve(zipFilePath).toFile();

        if (!zipFile.exists() || !zipFile.isFile()) {
            throw new IllegalArgumentException("The provided ZIP file path is invalid: " + zipFilePath);
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry manifestEntry = zip.getEntry("manifest.json");

            if (manifestEntry == null) {
                throw new IllegalArgumentException("The manifest.json file was not found in the ZIP file.");
            }

            try (InputStream inputStream = zip.getInputStream(manifestEntry)) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    public byte[] extractThumbnail(Path zipFilePath) throws IOException {
        File zipFile = GameDirManager.getInstance().getAssetPackFolder().toPath().resolve(zipFilePath).toFile();

        if (!zipFile.exists() || !zipFile.isFile()) {
            throw new IllegalArgumentException("The provided ZIP file path is invalid: " + zipFilePath);
        }

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry thumbnailImage = zip.getEntry("thumbnail.jpg");

            if (thumbnailImage == null) {
                try (FileInputStream fileInputStream = new FileInputStream(Objects.requireNonNull(getClass()
                        .getResource("no_thumbnail.jpg")).getFile())) {
                    return fileInputStream.readAllBytes();
                }
            }

            try (InputStream inputStream = zip.getInputStream(thumbnailImage)) {
                return inputStream.readAllBytes();
            }
        }
    }
}
