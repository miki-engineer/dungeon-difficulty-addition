package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Apply the level floor after chest loot generation, while its storage slots are visible. */
public final class ContainerLevelEvents {
    private ContainerLevelEvents() {}

    public static void opened(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            update(player, event.getContainer());
        }
    }

    public static void tick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayerEntity player) {
            // Also handles unlevelled equipment inserted by another player or hopper while open.
            update(player, player.currentScreenHandler);
        }
    }

    private static void update(ServerPlayerEntity player, ScreenHandler handler) {
        var config = AccessoryScalingConfig.get();
        if (!config.enabled || !config.minimum_equipment_level_enabled
                || !(handler instanceof GenericContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler)) return;
        boolean changed = false;
        for (var slot : handler.slots) {
            if (slot.inventory == player.getInventory()) continue;
            var stack = slot.getStack();
            // Existing dungeon levels are authoritative; do not pre-empt location-aware loot generation.
            if (!AccessoryItemScaling.needsMinimumLevel(stack)) continue;
            AccessoryItemScaling.enforceFixedLevel(stack);
            slot.markDirty();
            changed = true;
        }
        if (changed) handler.sendContentUpdates();
    }
}
