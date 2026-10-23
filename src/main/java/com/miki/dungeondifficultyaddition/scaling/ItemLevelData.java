package com.miki.dungeondifficultyaddition.scaling;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

/** Persisted level ownership markers. Keep keys and revision stable across refactors. */
final class ItemLevelData {
    private static final String FIXED_LEVEL_MARKER = "dungeon_difficulty_addition.fixed_level";
    private static final String FIXED_REVISION_MARKER = "dungeon_difficulty_addition.fixed_revision";
    private static final String MANUAL_LEVEL_MARKER = "dungeon_difficulty_addition.manual_level";
    private static final String ANVIL_LEVEL_MARKER = "dungeon_difficulty_addition.anvil_level";
    private static final String ANVIL_ITEM_MARKER = "dungeon_difficulty_addition.anvil_item";
    private static final String LEGACY_FIXED_LEVEL_MARKER = "dd_jewelry_compat.fixed_level";
    private static final String LEGACY_FIXED_REVISION_MARKER = "dd_jewelry_compat.fixed_revision";
    private static final String FORMULA_REVISION_MARKER = "dungeon_difficulty_addition.formula_revision";
    static final int FIXED_REVISION = 13;
    // 1: level 1 is base stats. Unmarked scaled stacks carry one extra bonus level.
    static final int FORMULA_REVISION = 1;

    private ItemLevelData() {
    }

    static void markAnvilLevel(ItemStack stack, int level) {
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, data -> data.apply(nbt -> {
            nbt.putInt(ANVIL_LEVEL_MARKER, level);
            nbt.putString(ANVIL_ITEM_MARKER, ItemTraits.of(stack).id());
        }));
    }

    static int anvilLevelMarker(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return 0;
        var nbt = data.getNbt();
        if (!ItemTraits.of(stack).id().equals(nbt.getString(ANVIL_ITEM_MARKER))) return 0;
        int level = nbt.getInt(ANVIL_LEVEL_MARKER);
        return com.miki.dungeondifficultyaddition.forge.ForgeRules.validLevel(level) ? level : 0;
    }

    static void clearAnvilLevel(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null || !data.contains(ANVIL_LEVEL_MARKER)) return;
        stack.set(DataComponentTypes.CUSTOM_DATA, data.apply(nbt -> {
            nbt.remove(ANVIL_LEVEL_MARKER);
            nbt.remove(ANVIL_ITEM_MARKER);
        }));
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

    static void markCurrentFormula(ItemStack stack) {
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT,
                data -> data.apply(nbt -> nbt.putInt(FORMULA_REVISION_MARKER, FORMULA_REVISION)));
    }

    static boolean hasCurrentFormula(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.contains(FORMULA_REVISION_MARKER)
                && customData.getNbt().getInt(FORMULA_REVISION_MARKER) == FORMULA_REVISION;
    }

    static int manualLevelMarker(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.contains(MANUAL_LEVEL_MARKER)
                ? customData.getNbt().getInt(MANUAL_LEVEL_MARKER)
                : 0;
    }
}
