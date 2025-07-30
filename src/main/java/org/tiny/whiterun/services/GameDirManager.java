package org.tiny.whiterun.services;

import java.nio.file.Path;
import java.util.Optional;


public class GameDirManager {


    private static GameDirManager instance;

    private GameDirectories gameDirectories;

    private GameDirManager() {

        Optional<String> gameFolder = UserPreferencesService.getInstance().getGameFolder();
        if (gameFolder.isPresent()) {
            this.gameDirectories = new GameDirectories(Path.of(gameFolder.get()).toFile());
        } else {
            Optional<Path> gameFolderOptional = new InstallationFinder().findGameFolder();
            if (gameFolderOptional.isPresent()) {
                Path installationFolderPath = gameFolderOptional.get();
                this.gameDirectories = new GameDirectories(installationFolderPath.toFile());
                setGameRootPath(installationFolderPath);
            }
        }
    }

    public GameDirectories getGameDirectories() {
        return gameDirectories;
    }

    public static synchronized GameDirManager getInstance() {
        if (instance == null) {
            instance = new GameDirManager();
        }
        return instance;
    }


    public void setGameRootPath(Path gameRootPath) {
        this.gameDirectories = new GameDirectories(gameRootPath.toFile());
        UserPreferencesService.getInstance().setGameFolder(gameRootPath.toString());
    }




}
