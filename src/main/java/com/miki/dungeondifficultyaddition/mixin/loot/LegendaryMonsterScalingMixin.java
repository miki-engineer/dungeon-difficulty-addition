package com.miki.dungeondifficultyaddition.mixin.loot;

import com.miki.dungeondifficultyaddition.compat.LegendaryMonsterAttributes;
import net.dungeon_difficulty.DungeonDifficulty;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/** Shared by DD loot scaling, commands, fixed levels, and anvil upgrades. */
@Mixin(value = ItemScaling.class, remap = false)
public abstract class LegendaryMonsterScalingMixin {
    @Inject(method = "isScalableItem", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void allowDynamicWeapons(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && LegendaryMonsterAttributes.hasWeaponAttributes(stack)) cir.setReturnValue(true);
    }

    @Inject(method = "scale(Lnet/minecraft/item/ItemStack;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/Identifier;Lnet/dungeon_difficulty/logic/PatternMatching$LocationData;)V",
            at = @At("TAIL"), remap = false)
    private static void scaleDynamicWeaponLoot(ItemStack stack, ServerWorld world, Identifier lootTable,
                                               PatternMatching.LocationData location, CallbackInfo ci) {
        if (ItemScaling.isScaled(stack) || !LegendaryMonsterAttributes.hasWeaponAttributes(stack)) return;
        var item = new PatternMatching.ItemData(PatternMatching.ItemKind.WEAPONS, lootTable,
                stack.getRegistryEntry(), stack.getRarity().toString());
        var result = PatternMatching.getModifiersForItem(location, item, world, DungeonDifficulty.config.value.loot_scaling);
        ItemScalingInvoker.dungeonDifficultyAddition$applyModifiersForItemStack(
                List.of(AttributeModifierSlot.MAINHAND), Registries.ITEM.getId(stack.getItem()).toString(),
                stack, result.modifiers(), result.level());
    }

    @Inject(method = "applyModifiersForItemStack", at = @At("HEAD"), remap = false)
    private static void prepare(List<AttributeModifierSlot> slots, String id, ItemStack stack,
                                List<Config.AttributeModifier> modifiers, int level, CallbackInfo ci) {
        if (level > 0 && !modifiers.isEmpty()) LegendaryMonsterAttributes.prepare(stack);
    }

    @Inject(method = "applyModifiersForItemStack", at = @At("TAIL"), remap = false)
    private static void mark(List<AttributeModifierSlot> slots, String id, ItemStack stack,
                             List<Config.AttributeModifier> modifiers, int level, CallbackInfo ci) {
        LegendaryMonsterAttributes.markRepaired(stack);
    }
}
