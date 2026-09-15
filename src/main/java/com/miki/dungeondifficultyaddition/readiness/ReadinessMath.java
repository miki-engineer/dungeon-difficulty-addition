package com.miki.dungeondifficultyaddition.readiness;

import java.util.Collection;
import java.util.Comparator;

/** Pure calculations, shared by combat and diagnostics. */
public final class ReadinessMath {
    private ReadinessMath() {}

    public static int readiness(Collection<Integer> levels, int required, int maxLevel) {
        if (required < 1 || levels.size() < required) return 0;
        return levels.stream().map(level -> clamp(level, maxLevel))
                .sorted(Comparator.reverseOrder()).skip(required - 1L).findFirst().orElse(0);
    }

    public static int clamp(int level, int maxLevel) {
        return Math.max(0, Math.min(level, maxLevel));
    }

    public static double incoming(int encounter, int readiness, double rate) {
        return 1 + Math.max(0, encounter - readiness) * rate;
    }

    public static double outgoing(int encounter, int weapon, double rate, double maxReduction) {
        return 1 - Math.min(maxReduction, Math.max(0, encounter - weapon) * rate);
    }
}
