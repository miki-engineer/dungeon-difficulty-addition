package com.miki.dungeondifficultyaddition.forge;

import java.util.Locale;

public enum GemKind {
    WEAPON, ARMOR, ACCESSORY, NEBULA;

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public static GemKind read(String value) {
        for (var kind : values()) if (kind.id().equals(value)) return kind;
        return null;
    }
}
