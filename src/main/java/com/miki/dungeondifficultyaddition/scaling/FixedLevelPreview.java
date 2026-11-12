package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.config.ModSettings;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class FixedLevelPreview {
    private static final int MAX_PENDING_ROLLS = 64;
    // Rolled crafting results awaiting their craft, keyed by the unscaled recipe output.
    private static final List<PendingRoll> PENDING_ROLLS = new ArrayList<>();

    private record PendingRoll(ItemStack output, ItemStack preview, ModSettings.Snapshot settings) {}

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

    /**
     * Grids set their result again on every ingredient change. Rolling each time let players
     * re-place ingredients until the preview showed a good roll, so keep one roll per output
     * until that output is crafted.
     */
    public static synchronized ItemStack craftingResult(ItemStack output) {
        if (output == null || output.isEmpty()) {
            return output;
        }
        var settings = ModSettings.get();
        for (var iterator = PENDING_ROLLS.iterator(); iterator.hasNext(); ) {
            var pending = iterator.next();
            if (pending.settings() != settings) {
                iterator.remove();
            } else if (ItemStack.areItemsAndComponentsEqual(pending.output(), output)) {
                return pending.preview().copyWithCount(output.getCount());
            }
        }

        var preview = scaledCopy(output);
        // Only new rolls are kept; already-levelled outputs (client sync, anvils) pass through.
        if (preview != output && !ItemStack.areItemsAndComponentsEqual(preview, output)) {
            if (PENDING_ROLLS.size() >= MAX_PENDING_ROLLS) {
                PENDING_ROLLS.remove(0);
            }
            PENDING_ROLLS.add(new PendingRoll(output.copy(), preview.copy(), settings));
        }
        return preview;
    }

    /** The previewed result was taken, so the next craft of that output rolls anew. */
    public static synchronized void crafted(ItemStack stack) {
        if (!PENDING_ROLLS.isEmpty()) {
            PENDING_ROLLS.removeIf(pending -> ItemStack.areItemsAndComponentsEqual(pending.preview(), stack));
        }
    }
}
