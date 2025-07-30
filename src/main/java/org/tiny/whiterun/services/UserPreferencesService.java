package org.tiny.whiterun.services;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class UserPreferencesService {
    private static UserPreferencesService instance;
    private final Path userPrefsFolder;
    private final Path settings;
    private final Properties properties = new Properties();

    private UserPreferencesService() {
        String homeDir = System.getProperty("user.home");
        userPrefsFolder = Path.of(homeDir).resolve(".whiterun");
        settings = userPrefsFolder.resolve("settings");
        try {
            if (!Files.exists(userPrefsFolder)) {
                Files.createDirectories(userPrefsFolder);
            }
            if (Files.exists(settings)) {
                properties.load(new FileInputStream(settings.toFile()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized UserPreferencesService getInstance() {
        if (instance == null) {
            instance = new UserPreferencesService();
        }
        return instance;
    }

    public Path getUserPrefsFolder() {
        return userPrefsFolder;
    }


    public Optional<String> getGameFolder() {

        return Optional.ofNullable(properties.getProperty("gameFolder"));
    }

    public Path getGameBackupFolder() {
        return userPrefsFolder.resolve("assets_backup");
    }

    public void setGameFolder(String path) {
        properties.setProperty("gameFolder", path);
        storeProperties();
    }

    private void storeProperties() {
        try (FileOutputStream out = new FileOutputStream(settings.toFile())) {
            properties.store(out, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}