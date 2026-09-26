package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import net.minecraft.item.ItemStack;

public final class FixedLevelPreview {
    private FixedLevelPreview() {
    }

    public static ItemStack scaledCopy(ItemStack original) {
        if (original == null || original.isEmpty()
                || (AccessoryScalingConfig.get().fixedLevel(original) <= 0
                && !AccessoryItemScaling.needsMinimumLevel(original))) {
            return original;
        }

        var preview = original.copy();
        AccessoryItemScaling.enforceFixedLevel(preview);
        return preview;
    }
}
