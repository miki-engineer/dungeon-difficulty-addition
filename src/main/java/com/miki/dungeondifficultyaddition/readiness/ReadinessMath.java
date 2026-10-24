package com.miki.dungeondifficultyaddition.readiness;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Pure calculations, shared by combat and diagnostics. */
public final class ReadinessMath {
    private ReadinessMath() {}

    /** Average slot level, unrounded, so every upgraded piece lowers the penalty a little. */
    public static double gearScore(Collection<Integer> levels, int maxLevel) {
        return gearScore(levels, List.of(), maxLevel);
    }

    /**
     * Optional slots (Curios) only ever raise the score: highest first, each is averaged in while
     * it is above the running score. That gives the best possible average, and a stronger player
     * never loses score for wearing an extra low-level accessory.
     */
    public static double gearScore(Collection<Integer> required, Collection<Integer> optional, int maxLevel) {
        long sum = 0;
        int count = 0;
        for (int level : required) {
            sum += clamp(level, maxLevel);
            count++;
        }
        var bonus = optional.stream().map(level -> clamp(level, maxLevel)).sorted(Comparator.reverseOrder()).toList();
        for (int level : bonus) {
            if (count > 0 && level * (long) count <= sum) break;
            sum += level;
            count++;
        }
        return count == 0 ? 0 : (double) sum / count;
    }

    public static int clamp(int level, int maxLevel) {
        return Math.max(0, Math.min(level, maxLevel));
    }

    public static double incoming(int encounter, double gearScore, double rate) {
        return 1 + Math.max(0, encounter - gearScore) * rate;
    }

    public static double outgoing(int encounter, double gearScore, double rate, double maxReduction) {
        return 1 - Math.min(maxReduction, Math.max(0, encounter - gearScore) * rate);
    }
}
