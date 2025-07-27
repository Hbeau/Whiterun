package org.tiny.whiterun.models;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.List;

public record PackDescriptor(String name, String description, int version, String[] authors,
                             @JsonSetter(nulls = Nulls.AS_EMPTY)
                             List<AssetFileEntry> entries) {
}
