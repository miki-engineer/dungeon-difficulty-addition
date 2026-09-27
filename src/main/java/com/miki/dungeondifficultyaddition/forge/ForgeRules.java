package com.miki.dungeondifficultyaddition.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Pure transaction planning, shared by preview and execution. */
public final class ForgeRules {
    public static final int MAX_LEVEL = 100_000;
    public record GemStack(int level, int count) {}
    private ForgeRules() {}

    public static boolean validLevel(int level) { return level > 0 && level <= MAX_LEVEL; }

    public static List<GemStack> salvage(List<Integer> itemLevels, int yield) {
        if (itemLevels.size() > 9 || yield < 1 || yield > 64) return List.of();
        var totals = new TreeMap<Integer, Integer>();
        for (int level : itemLevels) {
            if (!validLevel(level)) return List.of();
            totals.merge(level, yield, Integer::sum);
        }
        var result = new ArrayList<GemStack>();
        totals.forEach((level, count) -> {
            for (int remaining = count; remaining > 0; remaining -= 64) {
                result.add(new GemStack(level, Math.min(64, remaining)));
            }
        });
        return List.copyOf(result);
    }

    public static boolean canUpgrade(int currentLevel, int targetLevel, int available, int cost,
                                     boolean hasFixedLevel) {
        return currentLevel >= 0 && validLevel(targetLevel) && targetLevel > currentLevel
                && cost >= 2 && cost <= 64 && available >= cost && !hasFixedLevel;
    }
}
