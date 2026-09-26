package com.miki.dungeondifficultyaddition.scaling;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

/** Persisted level ownership markers. Keep keys and revision stable across refactors. */
final class ItemLevelData {
    private static final String FIXED_LEVEL_MARKER = "dungeon_difficulty_addition.fixed_level";
    private static final String FIXED_REVISION_MARKER = "dungeon_difficulty_addition.fixed_revision";
    private static final String MANUAL_LEVEL_MARKER = "dungeon_difficulty_addition.manual_level";
    private static final String LEGACY_FIXED_LEVEL_MARKER = "dd_jewelry_compat.fixed_level";
    private static final String LEGACY_FIXED_REVISION_MARKER = "dd_jewelry_compat.fixed_revision";
    static final int FIXED_REVISION = 13;

    private ItemLevelData() {
    }

    static void markManualLevel(ItemStack stack, int requestedLevel) {
        stack.apply(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT,
                requestedLevel,
                (data, assignedLevel) -> data.apply(nbt -> {
                    nbt.putInt(MANUAL_LEVEL_MARKER, assignedLevel);
                    nbt.remove(FIXED_LEVEL_MARKER);
                    nbt.remove(FIXED_REVISION_MARKER);
                    nbt.remove(LEGACY_FIXED_LEVEL_MARKER);
                    nbt.remove(LEGACY_FIXED_REVISION_MARKER);
                })
        );
    }

    static void markFixedLevel(ItemStack stack, int level) {
        stack.apply(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT,
                level,
                (data, fixedLevel) -> data.apply(nbt -> {
                    nbt.putInt(FIXED_LEVEL_MARKER, fixedLevel);
                    nbt.putInt(FIXED_REVISION_MARKER, FIXED_REVISION);
                    nbt.remove(MANUAL_LEVEL_MARKER);
                    nbt.remove(LEGACY_FIXED_LEVEL_MARKER);
                    nbt.remove(LEGACY_FIXED_REVISION_MARKER);
                })
        );
    }

    static int fixedLevelMarker(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.contains(FIXED_LEVEL_MARKER)
                ? customData.getNbt().getInt(FIXED_LEVEL_MARKER)
                : 0;
    }

    static int fixedRevisionMarker(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.contains(FIXED_REVISION_MARKER)
                ? customData.getNbt().getInt(FIXED_REVISION_MARKER)
                : 0;
    }

    static int manualLevelMarker(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.contains(MANUAL_LEVEL_MARKER)
                ? customData.getNbt().getInt(MANUAL_LEVEL_MARKER)
                : 0;
    }
}
