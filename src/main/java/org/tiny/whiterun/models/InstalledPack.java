package org.tiny.whiterun.models;

import java.util.Map;

public record InstalledPack(String name, Map<String, String> entries) {
}
