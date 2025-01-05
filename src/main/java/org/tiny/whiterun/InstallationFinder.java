package org.tiny.whiterun;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.util.Optional;


public class InstallationFinder {

    private static final String REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall";

    public Optional<String> findGameFolder(){
        try {
            // Parcourt toutes les sous-clés dans la clé de désinstallation
            String[] subKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_PATH);

            for (String subKey : subKeys) {
                String fullPath = REGISTRY_PATH + "\\" + subKey;

                // Vérifie si la sous-clé contient un DisplayName égal à "tiny glade"
                if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullPath, "DisplayName")) {
                    String displayName = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullPath, "DisplayName");

                    if ("Tiny Glade".equalsIgnoreCase(displayName)) {
                        // Retourne le chemin d'installation
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
