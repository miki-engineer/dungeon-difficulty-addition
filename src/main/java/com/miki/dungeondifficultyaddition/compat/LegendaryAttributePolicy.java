package com.miki.dungeondifficultyaddition.compat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/** Narrow correction: never reinterpret arbitrary negative attack-damage modifiers as speed. */
final class LegendaryAttributePolicy {
    private LegendaryAttributePolicy() {}

    static boolean misplacedSpeed(String attribute, String modifier) {
        return "minecraft:generic.attack_damage".equals(attribute)
                && "minecraft:base_attack_speed".equals(modifier);
    }

    /** Latest config-backed entry wins; values are never added together for the same modifier. */
    static <T, K> List<T> uniqueByKey(List<T> entries, Function<T, K> key) {
        var unique = new LinkedHashMap<K, T>();
        for (var entry : entries) unique.put(key.apply(entry), entry);
        return List.copyOf(unique.values());
    }
}
