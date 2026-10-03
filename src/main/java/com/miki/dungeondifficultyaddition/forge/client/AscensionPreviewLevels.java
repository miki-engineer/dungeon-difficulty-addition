package com.miki.dungeondifficultyaddition.forge.client;

import com.miki.dungeondifficultyaddition.forge.ForgeRules;
import net.dungeon_difficulty.config.Config;

/** Highest configured loot level, including DD's explicit reward-level overrides. */
public final class AscensionPreviewLevels {
    private AscensionPreviewLevels() {}

    public static int maximum(Config config) {
        int maximum = 1;
        if (config == null || config.dimensions == null) return maximum;
        for (var dimension : config.dimensions) {
            if (dimension == null) continue;
            maximum = Math.max(maximum, rewardLevel(dimension.difficulty));
            if (dimension.zones != null) for (var zone : dimension.zones) {
                if (zone != null) maximum = Math.max(maximum, rewardLevel(zone.difficulty));
            }
            if (dimension.entities != null) for (var entity : dimension.entities) {
                if (entity != null) maximum = Math.max(maximum, rewardLevel(entity.difficulty));
            }
        }
        return Math.min(maximum, ForgeRules.MAX_LEVEL);
    }

    private static int rewardLevel(Config.DifficultyReference difficulty) {
        return difficulty == null ? 0 : difficulty.reward_level != null
                ? difficulty.reward_level : difficulty.level;
    }
}
