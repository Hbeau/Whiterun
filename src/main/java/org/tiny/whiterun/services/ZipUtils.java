package org.tiny.whiterun.services;

import javafx.concurrent.Task;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tiny.whiterun.exceptions.CorruptedPackageException;
import org.tiny.whiterun.models.AssetsPack;
import org.tiny.whiterun.models.InstalledPack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.tiny.whiterun.services.GameDirectories.ASSETS;
import static org.tiny.whiterun.services.GameDirectories.COMPILED_ASSETS;

public class ZipUtils {

    private static final Logger log = LoggerFactory.getLogger(ZipUtils.class);

    private static ZipUtils instance;
    private final GameDirectories gameDirectories;
    private final InstalledPacksService installedPacksService;
    private final UserPreferencesService userPreferencesService = UserPreferencesService.getInstance();

    private ZipUtils(GameDirectories gameDirectories, InstalledPacksService installedPacksService) {
        this.gameDirectories = gameDirectories;
        this.installedPacksService = installedPacksService;
    }


    public static synchronized ZipUtils getInstance() {
        if (instance == null) {
            instance = new ZipUtils(GameDirManager.getInstance().getGameDirectories(),
                    InstalledPacksService.getInstance());
        }
        return instance;
    }


    public Task<InstalledPack> installPack(Path zipFilePath) {
        return new Task<>() {
            @Override
            protected InstalledPack call() {
                log.info("Installing pack {} ", zipFilePath);
                Path destDirPath = Path.of(gameDirectories.getGameRootPath());
                Map<String, String> installationVerification = new HashMap<>();
                try (ZipFile assetPackFile = new ZipFile(gameDirectories.getOrCreateAssetPackFolder().toPath().resolve(zipFilePath).toFile())) {
                    if (!Files.exists(destDirPath)) {
                        Files.createDirectories(destDirPath);
                    }
                    updateMessage("installing pack");
                    Enumeration<? extends ZipEntry> zipEntries = assetPackFile.entries();
                    int i = 0;
                    updateProgress(i, assetPackFile.size());
                    while (zipEntries.hasMoreElements()) {
                        if (extractFile(destDirPath, installationVerification, assetPackFile, zipEntries)) continue;
                        updateProgress(++i, assetPackFile.size());
                    }

                    log.info("Installation complete");
                    updateMessage("Installation complete");
                    return new InstalledPack(zipFilePath.toString(), installationVerification);
                } catch (IOException e) {
                    log.error("Error while decompressing the assets pack {} ", zipFilePath, e);
                    throw new RuntimeException("Error while decompressing the assets pack : " + zipFilePath, e);
                }
            }
        };
    }

    private boolean extractFile(Path destDirPath, Map<String, String> installationVerification, ZipFile assetPackFile, Enumeration<? extends ZipEntry> zipEntries) throws IOException {
        ZipEntry entry = zipEntries.nextElement();
        if (!(entry.getName().startsWith(ASSETS) || entry.getName().startsWith(COMPILED_ASSETS))) {
            return true;
        }
        String customAssetPath = entry.getName();
        Path filePath = destDirPath.resolve(customAssetPath);

        if (entry.isDirectory()) {
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath);
            }
        } else {
            Files.createDirectories(filePath.getParent());
            replaceFileInAssets(assetPackFile, entry, filePath);
            String fileChecksum = getFileChecksum(filePath.toFile());
            installationVerification.put(customAssetPath, fileChecksum);
        }
        return false;
    }

    private static void replaceFileInAssets(ZipFile assetPackFile, ZipEntry entry, Path filePath) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(assetPackFile.getInputStream(entry))) {

            try (OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public String listZipContentsAsTree(Path zipFilePath) throws IOException {
        StringBuilder treeBuilder = new StringBuilder();
        try (ZipFile zipFile = new ZipFile(gameDirectories.getOrCreateAssetPackFolder().toPath().resolve(zipFilePath).toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !(entry.getName().startsWith(ASSETS) || entry.getName().startsWith(COMPILED_ASSETS))) {
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
            Path assetsPackPath = gameDirectories.getOrCreateAssetPackFolder().toPath();

            if (!Files.isDirectory(assetsPath)) {
                throw new FileNotFoundException("The 'assets' folder was not found in" + assetsPackPath + ".");
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

    public String extractManifest(Path zipFilePath) throws CorruptedPackageException {
        try {
            File zipFile = gameDirectories.getOrCreateAssetPackFolder().toPath().resolve(zipFilePath).toFile();

            if (!zipFile.exists() || !zipFile.isFile()) {
                throw new CorruptedPackageException("The provided ZIP file path is invalid: " + zipFilePath);
            }

            try (ZipFile zip = new ZipFile(zipFile)) {
                ZipEntry manifestEntry = zip.getEntry("manifest.json");

                if (manifestEntry == null) {
                    throw new CorruptedPackageException("The manifest.json file was not found in the ZIP file.");
                }

                try (InputStream inputStream = zip.getInputStream(manifestEntry)) {
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new CorruptedPackageException(e.getMessage());
        }
    }

    public byte[] extractThumbnail(Path zipFilePath) throws CorruptedPackageException {
        try {
            File zipFile = gameDirectories.getOrCreateAssetPackFolder().toPath().resolve(zipFilePath).toFile();

            if (!zipFile.exists() || !zipFile.isFile()) {
                throw new CorruptedPackageException("The provided ZIP file path is invalid: " + zipFilePath);
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
        } catch (IOException e) {
            throw new CorruptedPackageException(e.getMessage());
        }
    }

    public Map<String, Boolean> checkInstallation(Map<String, String> filesWithChecksum) {
        return filesWithChecksum.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, fileWithChecksum -> {
                    Path path = Path.of(gameDirectories.getGameRootPath()).resolve(fileWithChecksum.getKey());
                    return Files.exists(path) &&
                            fileWithChecksum.getValue().equals(getFileChecksum(path.toFile()));
                }));

    }

    private String getFileChecksum(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return DigestUtils.sha256Hex(is);
        } catch (IOException e) {
            log.warn("cannot read file checksum", e);
            return "";
        }
    }

    public Task<Void> uninstallPack(AssetsPack assetsPack) {
        return new Task<>() {
            @Override
            protected Void call() {
                log.info("Uninstalling pack {}", assetsPack.archivePath());
                Path sourceDirPath = userPreferencesService.getGameBackupFolder();
                Path destDirPath = Path.of(gameDirectories.getGameRootPath());
                try {
                    updateMessage("Uninstalling pack");
                    Map<String, Boolean> fileWithChecksum = installedPacksService.getInstallationDetails(assetsPack);
                    for (Map.Entry<String, Boolean> entry : fileWithChecksum.entrySet()) {
                        if (entry.getValue()) {
                            File srcFile = sourceDirPath.resolve(entry.getKey()).toFile();
                            File destFile = destDirPath.resolve(entry.getKey()).toFile();
                            log.info("Replace file {} by {}",destFile,srcFile);
                            FileUtils.copyFile(srcFile, destFile, REPLACE_EXISTING);
                        }
                    }
                    installedPacksService.unregisterInstallation(assetsPack);
                    log.info("Uninstallation complete");
                    updateMessage("Uninstallation complete");
                } catch (IOException e) {
                    log.error("Error while uninstalling the assets pack {}", assetsPack.archivePath(), e);
                    throw new RuntimeException("Error while uninstalling the assets pack: " + assetsPack.archivePath(), e);
                }
                return null;
            }
        };
    }
}
