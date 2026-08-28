package com.example.dungeondifficultyaddition;

import net.minecraft.item.ItemStack;

public final class FixedLevelPreview {
    private FixedLevelPreview() {
    }

    public static ItemStack scaledCopy(ItemStack original) {
        if (original == null || original.isEmpty()
                || AccessoryScalingConfig.get().fixedLevel(original) <= 0) {
            return original;
        }

        var preview = original.copy();
        AccessoryItemScaling.enforceFixedLevel(preview);
        return preview;
    }
}
