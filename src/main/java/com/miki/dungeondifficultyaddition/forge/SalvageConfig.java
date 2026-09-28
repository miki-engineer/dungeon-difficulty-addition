package com.miki.dungeondifficultyaddition.forge;

import com.miki.dungeondifficultyaddition.config.ModSettings;

/** Restart to reload. Netherite deliberately has no configurable level ceiling. */
public final class SalvageConfig {
    public boolean enabled = true;
    public int upgrade_xp_levels = 5;
    public int gold_max_level = 3, diamond_max_level = 5;
    public int gold_durability = 32, diamond_durability = 1561, netherite_durability = 2031;
    public static SalvageConfig get() { return ModSettings.get().anvil(); }
    public void validate() {
        if (upgrade_xp_levels < 1 || upgrade_xp_levels > 39 || gold_max_level < 1 || diamond_max_level < gold_max_level
                || gold_durability < 1 || diamond_durability <= gold_durability
                || netherite_durability <= diamond_durability)
            throw new IllegalArgumentException("Hammer limits must be positive and durability must increase by tier");
    }
    public int durability(HammerTier tier) {
        return switch (tier) { case GOLD -> gold_durability; case DIAMOND -> diamond_durability; case NETHERITE -> netherite_durability; };
    }
}
