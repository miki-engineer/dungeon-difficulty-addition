package com.miki.dungeondifficultyaddition.forge.client;

import com.miki.dungeondifficultyaddition.forge.LevelGemItem;
import com.miki.dungeondifficultyaddition.forge.RomanNumerals;
import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only icon decoration shared by inventories, hotbar and standard item slots. */
public final class ItemLevelDecoration implements IItemDecorator {
    private static final float BADGE_SCALE = .60F;
    public static final ItemLevelDecoration INSTANCE = new ItemLevelDecoration();
    // Bounded client-render-thread cache; do not retain item stacks or world references.
    private final Map<Integer, String> labels = new LinkedHashMap<>(128, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, String> entry) { return size() > 256; }
    };
    private ItemLevelDecoration() {}
    @Override public boolean render(DrawContext context, TextRenderer font, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return false;
        int level;
        if (stack.getItem() instanceof LevelGemItem) level = LevelGemItem.level(stack);
        else {
            level = ItemScaling.getScaleFactor(stack);
            if (level <= 0 || !AccessoryItemScaling.isForgeEquipment(stack)) return false;
        }
        var numeral = labels.computeIfAbsent(level, RomanNumerals::format);
        if (numeral.isEmpty()) return false;
        // Center the seven visible capital rows without an upward baseline correction.
        // The advance also includes one trailing spacing pixel that must not affect centering.
        int inkWidth = Math.max(1, font.getWidth(numeral) - 1);
        float textScale = Math.min(.60F, 11F / inkWidth);
        int badgeWidth = Math.max(7, (int) Math.ceil(inkWidth * textScale) + 4);
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(x, y, 200);
            matrices.scale(BADGE_SCALE, BADGE_SCALE, 1);
            drawBadge(context, badgeWidth);
            matrices.translate((badgeWidth - inkWidth * textScale) / 2F,
                    (8F - 7F * textScale) / 2F, 1);
            matrices.scale(textScale, textScale, 1);
            context.drawText(font, numeral, 0, 0, 0xFFFFF4D6, false);
        } finally {
            matrices.pop();
        }
        return true;
    }
    private static void drawBadge(DrawContext context, int width) {
        // Original compact footprint; the numeral is centered within the eight-pixel plate.
        context.fill(0, 1, width, 7, 0xFF806445);
        context.fill(1, 0, width - 1, 1, 0xFFD6B77A);
        context.fill(1, 7, width - 1, 8, 0xFF806445);
        context.fill(1, 1, width - 1, 7, 0xFF17151C);
    }
}
