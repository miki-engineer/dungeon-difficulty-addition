package com.miki.dungeondifficultyaddition.readiness;

import java.util.Collection;

/** Pure calculations, shared by combat and diagnostics. */
public final class ReadinessMath {
    private ReadinessMath() {}

    /** Average slot level, unrounded, so every upgraded piece lowers the penalty a little. */
    public static double gearScore(Collection<Integer> levels, int maxLevel) {
        return levels.stream().mapToInt(level -> clamp(level, maxLevel)).average().orElse(0);
    }

    public static int clamp(int level, int maxLevel) {
        return Math.max(0, Math.min(level, maxLevel));
    }

    public static double incoming(int encounter, double gearScore, double rate) {
        return 1 + Math.max(0, encounter - gearScore) * rate;
    }

    public static double outgoing(int encounter, int weapon, double rate, double maxReduction) {
        return 1 - Math.min(maxReduction, Math.max(0, encounter - weapon) * rate);
    }
}
