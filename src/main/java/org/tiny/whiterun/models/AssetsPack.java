package org.tiny.whiterun.models;

import org.tiny.whiterun.services.InstalledPacksService;

import java.nio.file.Path;

public record AssetsPack(PackDescriptor packDescriptor, Path archivePath, byte[] image) {

    public PackState checkInstallation() {
        return InstalledPacksService.getInstance().checkPackInstallation(this);
    }
}
