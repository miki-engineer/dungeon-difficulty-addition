package com.miki.dungeondifficultyaddition.compat;

/** Explicit RPG-series families; avoids treating unrelated mods named "relics" as compatible. */
public final class AccessoryFamilies {
    private AccessoryFamilies() {}
    public static boolean jewelry(String namespace) {
        return "jewelry".equals(namespace) || "additional_rpg_jewelry".equals(namespace);
    }
    public static boolean relics(String namespace) {
        return "relics_rpgs".equals(namespace) || "more_relics".equals(namespace);
    }
    public static boolean accessory(String namespace) { return jewelry(namespace) || relics(namespace); }
    public static boolean material(String id) {
        return "additional_rpg_jewelry:aquamarine".equals(id) || "additional_rpg_jewelry:malachite".equals(id);
    }
    public static String effectsDirectory(String namespace) {
        return switch (namespace) {
            case "relics_rpgs" -> "relics";
            case "more_relics" -> "more_relics";
            default -> null;
        };
    }
}
