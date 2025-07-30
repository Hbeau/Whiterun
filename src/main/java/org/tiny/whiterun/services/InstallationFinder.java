package org.tiny.whiterun.services;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;


public class InstallationFinder {

    private static final Logger log = LoggerFactory.getLogger(InstallationFinder.class);

    private static final String REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall";
    public static final String TINY_GLADE = "Tiny Glade";
    public static final String DISPLAY_NAME = "DisplayName";

    public Optional<Path> findGameFolder() {
        try {
            String[] subKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_PATH);

            for (String subKey : subKeys) {
                String fullPath = REGISTRY_PATH + "\\" + subKey;

                if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullPath, DISPLAY_NAME)) {
                    String displayName = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullPath, DISPLAY_NAME);

                    if (TINY_GLADE.equalsIgnoreCase(displayName)) {
                        if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullPath, "InstallLocation")) {
                            String pathText = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullPath, "InstallLocation");
                            return Optional.of(Path.of(pathText));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while reading registry", e);
        }
        return Optional.empty();

    }
}
