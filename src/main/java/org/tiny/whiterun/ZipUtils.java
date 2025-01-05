package org.tiny.whiterun;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

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
     * @param zipFilePath    Chemin du fichier ZIP à décompresser.
     * @throws IOException Si une erreur survient lors de la décompression.
     */
    public void unzip(String zipFilePath) throws IOException {

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
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
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

    public  String listZipContentsAsTree(String zipFilePath) throws IOException {
        StringBuilder treeBuilder = new StringBuilder();
        try (ZipFile zipFile = new ZipFile(GameDirManager.getInstance().getAssetPackFolder().toPath().resolve(zipFilePath).toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if(entry.isDirectory()) {
                    continue;
                }
                String path = entry.getName();
                treeBuilder.append(path).append("\n");
            }
        }

        return treeBuilder.toString();
    }

}
