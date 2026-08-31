package com.miki.dungeondifficultyaddition.mixin;

import com.miki.dungeondifficultyaddition.AccessoryItemScaling;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class FixedItemStackScalingMixin {
    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void dungeonDifficultyAddition$enforceInventoryLevel(
            World world,
            Entity entity,
            int slot,
            boolean selected,
            CallbackInfo ci
    ) {
        dungeonDifficultyAddition$enforceOnServer(world);
    }

    @Inject(method = "onCraftByPlayer", at = @At("TAIL"))
    private void dungeonDifficultyAddition$enforcePlayerCraftedLevel(
            World world,
            PlayerEntity player,
            int amount,
            CallbackInfo ci
    ) {
        dungeonDifficultyAddition$enforceOnServer(world);
    }

    @Inject(method = "onCraftByCrafter", at = @At("TAIL"))
    private void dungeonDifficultyAddition$enforceAutomatedCraftedLevel(World world, CallbackInfo ci) {
        dungeonDifficultyAddition$enforceOnServer(world);
    }

    private void dungeonDifficultyAddition$enforceOnServer(World world) {
        if (!world.isClient) {
            AccessoryItemScaling.enforceFixedLevel((ItemStack) (Object) this);
        }
    }
}
