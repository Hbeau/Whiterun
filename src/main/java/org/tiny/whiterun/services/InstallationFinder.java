package org.tiny.whiterun.services;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.util.Optional;


public class InstallationFinder {

    private static final String REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall";
    public static final String TINY_GLADE = "Tiny Glade";
    public static final String DISPLAY_NAME = "DisplayName";

    public Optional<String> findGameFolder(){
        try {
            String[] subKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_PATH);

            for (String subKey : subKeys) {
                String fullPath = REGISTRY_PATH + "\\" + subKey;

                if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullPath, DISPLAY_NAME)) {
                    String displayName = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullPath, DISPLAY_NAME);

                    if (TINY_GLADE.equalsIgnoreCase(displayName)) {
                        if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullPath, "InstallLocation")) {
                            return Optional.of(Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullPath, "InstallLocation"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du registre : " + e.getMessage());
        }
        return Optional.empty();

    }
}
