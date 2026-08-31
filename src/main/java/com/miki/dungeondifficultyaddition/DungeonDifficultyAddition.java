package com.miki.dungeondifficultyaddition;

import net.neoforged.fml.common.Mod;

@Mod(DungeonDifficultyAddition.MOD_ID)
public final class DungeonDifficultyAddition {
    public static final String MOD_ID = "dungeon_difficulty_addition";
    public static final String LEGACY_MOD_ID = "dd_jewelry_compat";
    public static final String JEWELRY_MOD_ID = "jewelry";
    public static final String RELICS_MOD_ID = "relics_rpgs";
    public static final String SPELL_ENGINE_MOD_ID = "spell_engine";

    public DungeonDifficultyAddition() {
        AccessoryScalingConfig.get();
    }
}
