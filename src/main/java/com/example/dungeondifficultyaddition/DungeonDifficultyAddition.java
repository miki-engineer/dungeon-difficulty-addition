package com.example.dungeondifficultyaddition;

import net.neoforged.fml.common.Mod;

@Mod(DungeonDifficultyAddition.MOD_ID)
public final class DungeonDifficultyAddition {
    public static final String MOD_ID = "dungeon_difficulty_addition";
    public static final String LEGACY_MOD_ID = "dd_jewelry_compat";

    public DungeonDifficultyAddition() {
        AccessoryScalingConfig.get();
    }
}
