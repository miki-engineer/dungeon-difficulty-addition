package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import net.dungeon_difficulty.config.Config;

/**
 * With scaling.minimum_equipment_level_enabled on (the default), level 1 is base stats for equipment
 * and mobs alike, and bonuses count from level 2. Off restores Dungeon Difficulty's original math,
 * where level 1 already has one level of bonus. One switch, so items and mobs always agree.
 */
public final class LevelBonus {
    private LevelBonus() {}

    /** Like the level-1 floor itself, this needs the scaling section enabled. */
    public static boolean levelOneIsBase() {
        var config = AccessoryScalingConfig.get();
        return config.enabled && config.minimum_equipment_level_enabled;
    }

    public static int bonusLevels(int level) {
        return bonusLevels(level, levelOneIsBase());
    }

    static int bonusLevels(int level, boolean levelOneIsBase) {
        return Math.max(0, levelOneIsBase ? level - 1 : level);
    }

    /** Level 1 is exact (no offset or randomness) when it is base; otherwise Dungeon Difficulty's own roll. */
    public static float randomizedValue(Config.AttributeModifier modifier, int level) {
        if (!levelOneIsBase()) return modifier.randomizedValue(level);
        var bonusLevels = bonusLevels(level, true);
        return bonusLevels > 0 ? modifier.randomizedValue(bonusLevels) : 0F;
    }
}
