package com.miki.dungeondifficultyaddition.mixin;

import com.miki.dungeondifficultyaddition.AccessoryItemScaling;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class FixedItemEntityScalingMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void dungeonDifficultyAddition$enforceDroppedItemLevel(CallbackInfo ci) {
        var itemEntity = (ItemEntity) (Object) this;
        if (!itemEntity.getWorld().isClient) {
            AccessoryItemScaling.enforceFixedLevel(itemEntity.getStack());
        }
    }
}
