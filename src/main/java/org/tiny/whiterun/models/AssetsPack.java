package org.tiny.whiterun.models;

import org.tiny.whiterun.services.GameDirManager;

import java.nio.file.Path;

public record AssetsPack(PackDescriptor packDescriptor, Path archivePath, byte[] image) {

    public PackState checkInstallation() {
        return GameDirManager.getInstance().checkPackInstallation(this);
    }
}
