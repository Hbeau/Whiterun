package org.tiny.whiterun.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(ZipUtils.class);

    public static final String ASSETS_FOLDER = "assets";
    private static ZipUtils instance;

    private ZipUtils() {

    }


    public static synchronized ZipUtils getInstance() {
        if (instance == null) {
            instance = new ZipUtils();
        }
        return instance;
    }


    public void installPack(Path zipFilePath) throws IOException {

        Path destDirPath = Paths.get(GameDirManager.getInstance().getGameRootPath()).resolve(ASSETS_FOLDER);
        Path assetPackFile = GameDirManager.getInstance().getAssetPackFolder().toPath().resolve(zipFilePath);
        if (!Files.exists(destDirPath)) {
            Files.createDirectories(destDirPath);
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(assetPackFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                if (!entry.getName().startsWith(ASSETS_FOLDER)) {
                    continue;
                }

                Path filePath = destDirPath.resolve(entry.getName().substring((ASSETS_FOLDER + "/").length()));

                if (entry.isDirectory()) {
                    if (!Files.exists(filePath)) {
                        Files.createDirectories(filePath);
                    }
                } else {
                    Files.createDirectories(filePath.getParent());

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

    public String listZipContentsAsTree(Path zipFilePath) throws IOException {
        StringBuilder treeBuilder = new StringBuilder();
        try (ZipFile zipFile = new ZipFile(GameDirManager.getInstance().getAssetPackFolder().toPath().resolve(zipFilePath).toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(ASSETS_FOLDER)) {
                    continue;
                }
                String path = entry.getName().replace("\\", "/");
                treeBuilder.append(path).append("\n");
            }
        }

        return treeBuilder.toString();
    }

    public void createAssetsPack(Path assetsPath) {
        try {
            Path assetsPackPath = GameDirManager.getInstance().getAssetPackFolder().toPath();

            if (!Files.isDirectory(assetsPath)) {
                throw new FileNotFoundException("The 'assets' folder was not found in" + assetsPackPath + ".");
            }

            if (!Files.exists(assetsPackPath)) {
                Files.createDirectories(assetsPackPath);
                log.info("Folder 'assets-pack' has bean created in {}", assetsPackPath);
            }

            Path zipFilePath = assetsPackPath.resolve("default.zip");

            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipFilePath));
                 Stream<Path> walk = Files.walk(assetsPath)) {
                //Audio folder is removed because it's too large
                walk
                        .filter(path -> !path.toString().contains("audio"))
                        .forEach(path -> {
                            try {
                                if (Files.isRegularFile(path)) {
                                    String zipEntryName = "assets\\" + assetsPath.relativize(path);
                                    zipOut.putNextEntry(new ZipEntry(zipEntryName));
                                    Files.copy(path, zipOut);
                                    zipOut.closeEntry();
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Error while compressing the " + path + ": " + e.getMessage(), e);
                            }
                        });

                try (InputStream thumbnailInput = getClass().getResourceAsStream("default_thumbnail.jpg");
                     InputStream manifestInput = getClass().getResourceAsStream("manifest.json")) {
                    if (Objects.nonNull(thumbnailInput) && Objects.nonNull(manifestInput)) {
                        zipOut.putNextEntry(new ZipEntry("thumbnail.jpg"));
                        zipOut.write(thumbnailInput.readAllBytes());
                        zipOut.closeEntry();
                        zipOut.putNextEntry(new ZipEntry("manifest.json"));
                        zipOut.write(manifestInput.readAllBytes());
                        zipOut.closeEntry();
                    }
                }
            }
            log.info("'assets' folder compressed and saved in '{}'", zipFilePath);
        } catch (Exception e) {
            log.error("Error creating asset pack:", e);
        }
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
