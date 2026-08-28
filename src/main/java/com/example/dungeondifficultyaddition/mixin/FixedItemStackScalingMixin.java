package com.example.dungeondifficultyaddition.mixin;

import com.example.dungeondifficultyaddition.AccessoryItemScaling;
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
    private void ddJewelryCompat$enforceInventoryLevel(
            World world,
            Entity entity,
            int slot,
            boolean selected,
            CallbackInfo ci
    ) {
        ddJewelryCompat$enforceOnServer(world);
    }

    @Inject(method = "onCraftByPlayer", at = @At("TAIL"))
    private void ddJewelryCompat$enforcePlayerCraftedLevel(
            World world,
            PlayerEntity player,
            int amount,
            CallbackInfo ci
    ) {
        ddJewelryCompat$enforceOnServer(world);
    }

    @Inject(method = "onCraftByCrafter", at = @At("TAIL"))
    private void ddJewelryCompat$enforceAutomatedCraftedLevel(World world, CallbackInfo ci) {
        ddJewelryCompat$enforceOnServer(world);
    }

    private void ddJewelryCompat$enforceOnServer(World world) {
        if (!world.isClient) {
            AccessoryItemScaling.enforceFixedLevel((ItemStack) (Object) this);
        }
    }
}
