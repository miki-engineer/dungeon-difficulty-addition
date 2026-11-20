package com.miki.dungeondifficultyaddition.compat;

import java.util.List;
import java.util.Set;

/** Explicit RPG-series families; avoids treating unrelated mods named "relics" as compatible. */
public final class AccessoryFamilies {
    /**
     * The mods listed in scaling.extra_relic_namespaces, set by ModSettings each time settings load (pushed there, so asking a family
     * never needs the config loaded). Until then the config's default: Realmbound Content.
     */
    private static volatile Set<String> extraRelics = Set.of("realmboundcontent");

    private AccessoryFamilies() {}
    public static boolean jewelry(String namespace) {
        return "jewelry".equals(namespace) || "additional_rpg_jewelry".equals(namespace);
    }
    public static boolean relics(String namespace) {
        return builtInRelics(namespace) || extraRelics(namespace);
    }
    /** Relics RPG and More Relics, whose curio class DDA hooks directly. */
    public static boolean builtInRelics(String namespace) {
        return "relics_rpgs".equals(namespace) || "more_relics".equals(namespace);
    }
    /** The extra relic mods (Realmbound Content by default): their curios scale through Curios' attribute event. */
    public static boolean extraRelics(String namespace) {
        return namespace != null && !jewelry(namespace) && !builtInRelics(namespace) && extraRelics.contains(namespace);
    }
    public static void setExtraRelics(List<String> namespaces) {
        extraRelics = namespaces == null ? Set.of() : Set.copyOf(namespaces);
    }
    public static boolean accessory(String namespace) { return jewelry(namespace) || relics(namespace); }
    public static boolean material(String id) {
        return "additional_rpg_jewelry:aquamarine".equals(id) || "additional_rpg_jewelry:malachite".equals(id);
    }
}
