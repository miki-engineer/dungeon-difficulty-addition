package com.miki.dungeondifficultyaddition;

import com.miki.dungeondifficultyaddition.command.DungeonDifficultyCommands;
import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.readiness.EncounterConfig;
import com.miki.dungeondifficultyaddition.readiness.EncounterEvents;
import com.miki.dungeondifficultyaddition.forge.RunicForge;
import com.miki.dungeondifficultyaddition.scaling.ContainerLevelEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(DungeonDifficultyAddition.MOD_ID)
public final class DungeonDifficultyAddition {
    public static final String MOD_ID = "dungeon_difficulty_addition";
    public static final String LEGACY_MOD_ID = "dd_jewelry_compat";
    public static final String JEWELRY_MOD_ID = "jewelry";
    public static final String RELICS_MOD_ID = "relics_rpgs";
    public static final String SPELL_ENGINE_MOD_ID = "spell_engine";

    public DungeonDifficultyAddition(IEventBus modBus) {
        RunicForge.register(modBus);
        AccessoryScalingConfig.get();
        EncounterConfig.get();
        EncounterEvents.register();
        NeoForge.EVENT_BUS.addListener(DungeonDifficultyCommands::register);
        NeoForge.EVENT_BUS.addListener(ContainerLevelEvents::opened);
        NeoForge.EVENT_BUS.addListener(ContainerLevelEvents::tick);
        NeoForge.EVENT_BUS.addListener(com.miki.dungeondifficultyaddition.compat.legendary.LegendaryAbilityDamage::spawned);
    }
}
