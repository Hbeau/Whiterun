package org.tiny.whiterun.models;

import java.nio.file.Path;
import java.util.Objects;

public class AssetsPack {
    private final String name;
    private final String description;
    private final byte[] image;
    private final Path archivePath;

    public AssetsPack(String name, String description, byte[] image, Path archivePath) {
        this.name = name;
        this.description = description;
        this.image = image;
        this.archivePath = archivePath;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public byte[] getImage() {
        return image;
    }

    public Path getArchivePath() {
        return archivePath;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        AssetsPack that = (AssetsPack) o;
        return Objects.equals(archivePath, that.archivePath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(archivePath);
    }
}
