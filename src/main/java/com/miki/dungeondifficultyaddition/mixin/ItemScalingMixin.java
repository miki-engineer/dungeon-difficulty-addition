package com.miki.dungeondifficultyaddition.mixin;

import com.miki.dungeondifficultyaddition.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.AccessoryItemScaling;
import net.dungeon_difficulty.DungeonDifficulty;
import net.dungeon_difficulty.logic.ItemScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemScaling.class, remap = false)
public abstract class ItemScalingMixin {
    // Dungeon Difficulty 3.8.0 filters loot before resolving its location. Allow
    // our accessories through so the existing location-aware hook still runs.
    // Older supported versions have no such filter, hence require = 0.
    @Inject(method = "isScalableItem", at = @At("RETURN"), cancellable = true,
            remap = false, require = 0)
    private static void dungeonDifficultyAddition$allowAccessoryLoot(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ() || stack.isEmpty()) {
            return;
        }
        var config = AccessoryScalingConfig.get();
        if (config.enabled && AccessoryItemScaling.isSupportedAccessory(stack, config)
                && !AccessoryItemScaling.isBuiltInExcluded(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "scale(Lnet/minecraft/item/ItemStack;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/Identifier;Lnet/dungeon_difficulty/logic/PatternMatching$LocationData;)V",
            at = @At("TAIL"),
            remap = false
    )
    private static void dungeonDifficultyAddition$scaleJewelry(
            ItemStack itemStack,
            ServerWorld world,
            Identifier lootTableId,
            PatternMatching.LocationData locationData,
            CallbackInfo ci
    ) {
        var config = AccessoryScalingConfig.get();
        if (AccessoryItemScaling.isBuiltInExcluded(itemStack)) {
            if (ItemScaling.isScaled(itemStack)) {
                ItemScaling.removeScaling(itemStack);
            }
            return;
        }
        if (ItemScaling.isScaled(itemStack) || !config.enabled || !AccessoryItemScaling.isSupportedAccessory(itemStack, config)) {
            return;
        }

        var itemEntry = itemStack.getRegistryEntry();
        var rarity = itemStack.getRarity().toString();
        var scaling = DungeonDifficulty.config.value.loot_scaling;
        var itemData = new PatternMatching.ItemData(PatternMatching.ItemKind.WEAPONS, lootTableId, itemEntry, rarity);
        var result = PatternMatching.getModifiersForItem(locationData, itemData, world, scaling);
        var level = result.level();

        AccessoryItemScaling.applyLootLevel(itemStack, level);
    }
}
