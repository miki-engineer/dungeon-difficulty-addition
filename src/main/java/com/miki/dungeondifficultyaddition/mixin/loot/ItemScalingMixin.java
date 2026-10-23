package com.miki.dungeondifficultyaddition.mixin.loot;

import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import com.miki.dungeondifficultyaddition.scaling.LevelBonus;
import net.dungeon_difficulty.DungeonDifficulty;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    // Every item roll (loot, fixed, manual, anvil, Legendary Monsters) passes through here; mob
    // scaling uses EntityScaling and keeps Dungeon Difficulty's own level formula.
    @Redirect(
            method = "applyModifiersForItemStack",
            at = @At(value = "INVOKE",
                    target = "Lnet/dungeon_difficulty/config/Config$AttributeModifier;randomizedValue(I)F"),
            remap = false
    )
    private static float dungeonDifficultyAddition$levelOneIsBase(Config.AttributeModifier modifier, int level) {
        return LevelBonus.randomizedValue(modifier, level);
    }

    // Dungeon Difficulty marks a stack only right after rolling it (rescale removes, then re-rolls).
    @Inject(method = "markAsScaled", at = @At("TAIL"), remap = false)
    private static void dungeonDifficultyAddition$markCurrentFormula(ItemStack stack, int level, CallbackInfo ci) {
        AccessoryItemScaling.markCurrentFormula(stack);
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
