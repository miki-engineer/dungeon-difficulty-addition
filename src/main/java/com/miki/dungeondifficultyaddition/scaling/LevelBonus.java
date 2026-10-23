package com.miki.dungeondifficultyaddition.scaling;

import net.dungeon_difficulty.config.Config;

/** Level 1 is the item's base stats; every scaling formula counts bonus levels from level 2. */
public final class LevelBonus {
    private LevelBonus() {}

    public static int bonusLevels(int level) {
        return Math.max(0, level - 1);
    }

    /** Dungeon Difficulty's roll, one level lower; level 1 is exact (no offset or randomness). */
    public static float randomizedValue(Config.AttributeModifier modifier, int level) {
        var bonusLevels = bonusLevels(level);
        return bonusLevels > 0 ? modifier.randomizedValue(bonusLevels) : 0F;
    }
}
