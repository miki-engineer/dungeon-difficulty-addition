package com.miki.dungeondifficultyaddition.mixin;

import com.miki.dungeondifficultyaddition.FixedLevelPreview;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CraftingResultInventory.class)
public abstract class CraftingResultInventoryMixin {
    @ModifyVariable(method = "setStack", at = @At("HEAD"), argsOnly = true)
    private ItemStack dungeonDifficultyAddition$scaleCraftingResultPreview(ItemStack stack) {
        return FixedLevelPreview.scaledCopy(stack);
    }
}
