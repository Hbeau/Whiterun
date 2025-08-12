package org.tiny.whiterun.models;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public record NewPackForm(Path assetsPath, String title, String description, File image, String[] authors) {
    /**
     * Checks if the InstallPack is valid.
     * Returns true only if all required fields are set and point to valid resources.
     */
    public boolean isValid() {
        return validate() == null;
    }

    public String validate() {
        System.out.println(assetsPath);
        if (assetsPath == null || assetsPath.toString().isBlank() || !Files.isDirectory(assetsPath)) {
            return "Assets path is missing or is not a valid directory.";
        }
        if (title == null || title.isBlank() || title.length() > 60) {
            return "Title is missing or exceed 60 characters.";
        }
        if (description == null || description.isBlank()|| description.length() > 200) {
            return "Description is missing or exceed 200 characters.";
        }
        if (image != null && ( !image.isFile() || !image.canRead())) {
            return "Invalid or unreadable image.";
        }
        if (authors == null || authors.length == 0 ||
                !java.util.Arrays.stream(authors).allMatch(a -> a != null && !a.isBlank() && a.length()<30)) {
            return "Authors list is empty or contains invalid names.";
        }
        return null; // valid
    }
}
