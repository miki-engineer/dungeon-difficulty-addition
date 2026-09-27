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
        int textWidth = Math.max(1, font.getWidth(numeral));
        float scale = Math.min(.60F, 11F / textWidth);
        int badgeWidth = Math.max(7, (int) Math.ceil(textWidth * scale) + 4);
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(x, y, 200);
            // Keep the entire framed badge below 9 x 5.4 slot pixels, including its shadow.
            matrices.scale(BADGE_SCALE, BADGE_SCALE, 1);
            drawBadge(context, badgeWidth);
            matrices.translate((badgeWidth - textWidth * scale) / 2F, 1.5F, 1);
            matrices.scale(scale, scale, 1);
            context.drawText(font, numeral, 0, 0, 0xFFFFE4A3, true);
        } finally {
            matrices.pop();
        }
        return true;
    }
    private static void drawBadge(DrawContext context, int width) {
        // An eight-pixel-high, clipped-corner metal plaque. Lower slot overlays remain free.
        context.fill(1, 1, width, 9, 0xA0000000);
        context.fill(1, 0, width - 1, 8, 0xFF211B20);
        context.fill(0, 1, width, 7, 0xFF211B20);
        context.fill(1, 0, width - 1, 1, 0xFFE8C78C);
        context.fill(0, 1, 1, 7, 0xFFB69155);
        context.fill(width - 1, 1, width, 7, 0xFF725137);
        context.fill(1, 7, width - 1, 8, 0xFF725137);
        context.fill(1, 1, width - 1, 3, 0xFF3D3540);
        context.fill(1, 3, width - 1, 7, 0xFF25212B);
        context.fill(1, 1, 2, 2, 0xFFFFDF9E);
    }
}
