package org.tiny.whiterun.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.tiny.whiterun.models.AssetsPack;
import org.tiny.whiterun.models.InstalledPack;
import org.tiny.whiterun.models.PackState;
import org.tiny.whiterun.models.PacksWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InstalledPacksService {


    private final Path installationFile;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static InstalledPacksService instance;

    private InstalledPacksService(Path userPrefsFolder) {
        this.installationFile = userPrefsFolder.resolve("installation.json");
    }

    public static synchronized InstalledPacksService getInstance() {
        if (instance == null) {
            instance = new InstalledPacksService(UserPreferencesService.getInstance().getUserPrefsFolder());
        }
        return instance;
    }

    public Map<String, Boolean> getInstallationDetails(AssetsPack pack) {
        Optional<InstalledPack> packOptional = getInstalledPack(pack);
        if (packOptional.isEmpty()) {
            return Map.of();
        }
        return ZipUtils.getInstance().checkInstallation(packOptional.get().entries());
    }

    private Optional<InstalledPack> getInstalledPack(AssetsPack pack) {
        PacksWrapper packsWrapper;
        try {
            packsWrapper = getOrCreatePacks();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return packsWrapper.packs().stream()
                .filter(installedPack -> installedPack.name().equals(pack.archivePath().getFileName().toString())).findFirst();
    }

    public void registerInstallation(InstalledPack pack) throws IOException {
        PacksWrapper packsWrapper = getOrCreatePacks();
        Optional<InstalledPack> first = packsWrapper.packs().stream()
                .filter(installedPack -> installedPack.name().equals(pack.name())).findFirst();
        if (first.isEmpty()) {
            packsWrapper.packs().add(pack);
        } else {
            first.get().entries().replaceAll((s, s2) -> pack.entries().get(s));
        }

        objectMapper.writeValue(installationFile.toFile(), packsWrapper);
    }

    public void unregisterInstallation(AssetsPack pack) throws IOException {
        PacksWrapper packsWrapper = getOrCreatePacks();
        packsWrapper.packs().removeIf(installedPack -> installedPack.name().equals(pack.archivePath().getFileName().toString()));

        objectMapper.writeValue(installationFile.toFile(), packsWrapper);
    }

    public PacksWrapper getOrCreatePacks() throws IOException {
        if (!Files.exists(installationFile)) {
            Files.createFile(installationFile);
            objectMapper.writeValue(installationFile.toFile(), new PacksWrapper(List.of()));
        }
        return objectMapper.readValue(installationFile.toFile(), PacksWrapper.class);
    }

    public PackState checkPackInstallation(AssetsPack pack) {
        Optional<InstalledPack> packOptional = getInstalledPack(pack);
        if (packOptional.isEmpty()) {
            return PackState.NOT_INSTALLED;
        }
        Map<String, Boolean> fileWithChecksum = ZipUtils.getInstance().checkInstallation(packOptional.get().entries());
        if (fileWithChecksum.containsValue(false)) {
            return PackState.COVERED;
        }
        return PackState.INSTALLED;
    }
}