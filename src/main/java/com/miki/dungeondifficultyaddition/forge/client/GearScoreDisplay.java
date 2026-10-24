package com.miki.dungeondifficultyaddition.forge.client;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import com.miki.dungeondifficultyaddition.readiness.EncounterConfig;
import com.miki.dungeondifficultyaddition.readiness.EquippedLevels;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/** Survival inventory tab, computed from the local player's synced armor, weapon and Curios. */
final class GearScoreDisplay {
    // Vanilla's rightmost selected creative tab: its right edge continues the panel's right border.
    private static final Identifier TAB = Identifier.ofVanilla("container/creative_inventory/tab_top_selected_7");
    private static final Identifier ICON = Identifier.of(DungeonDifficultyAddition.MOD_ID, "gear_score");
    private static final int TAB_WIDTH = 26, TAB_HEIGHT = 32, OVERLAP = 4, PANEL_WIDTH = 176;
    // Stretch the plain middle column so the corners and bevels keep their pixel art.
    private static final int EDGE = 4, MIDDLE_COLUMN = 12;
    // Creative tab icon position; the value sits beside it, clear of the 3-pixel right bevel.
    private static final int ICON_SIZE = 16, ICON_X = 5, ICON_Y = 9, GAP = 3, RIGHT_PADDING = 6;
    private static final int HOVER = 0x30FFFFFF;
    private GearScoreDisplay() {}

    static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen) || !EncounterConfig.get().active()) return;
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        double gearScore;
        try {
            gearScore = EquippedLevels.gearScore(EquippedLevels.slots(client.player));
        } catch (RuntimeException | LinkageError exception) {
            return; // Never break the inventory screen over an optional tab.
        }
        var font = client.textRenderer;
        var value = EquippedLevels.format(gearScore);
        int width = Math.max(TAB_WIDTH, ICON_X + ICON_SIZE + GAP + font.getWidth(value) + RIGHT_PADDING);
        int right = screen.getGuiLeft() + PANEL_WIDTH;
        int left = right - width;
        int top = screen.getGuiTop() - TAB_HEIGHT + OVERLAP;
        var context = event.getGuiGraphics();
        drawTab(context, left, top, width);
        boolean hovered = event.getMouseX() >= left && event.getMouseX() < right
                && event.getMouseY() >= top && event.getMouseY() < screen.getGuiTop();
        // Inside the bevels only, so the outline and corners stay crisp.
        if (hovered) context.fill(left + 3, top + 3, right - 3, screen.getGuiTop(), HOVER);
        int iconX = left + ICON_X, iconY = top + ICON_Y;
        context.drawGuiTexture(ICON, iconX, iconY, ICON_SIZE, ICON_SIZE);
        // White with shadow, like text on vanilla buttons; vertically centred on the icon.
        context.drawText(font, value, iconX + ICON_SIZE + GAP, iconY + 4, 0xFFFFFF, true);
        if (hovered) context.drawTooltip(font, tooltip(font, value), event.getMouseX(), event.getMouseY());
    }

    private static void drawTab(DrawContext context, int left, int top, int width) {
        context.drawGuiTexture(TAB, TAB_WIDTH, TAB_HEIGHT, 0, 0, left, top, EDGE, TAB_HEIGHT);
        for (int x = left + EDGE; x < left + width - EDGE; x++) {
            context.drawGuiTexture(TAB, TAB_WIDTH, TAB_HEIGHT, MIDDLE_COLUMN, 0, x, top, 1, TAB_HEIGHT);
        }
        context.drawGuiTexture(TAB, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH - EDGE, 0,
                left + width - EDGE, top, EDGE, TAB_HEIGHT);
    }

    private static List<Text> tooltip(TextRenderer font, String value) {
        var lines = new ArrayList<Text>();
        lines.add(Text.translatable("gui.dungeon_difficulty_addition.gear_score.title", value).formatted(Formatting.GOLD));
        for (var line : font.getTextHandler().wrapLines(
                Text.translatable("gui.dungeon_difficulty_addition.gear_score.hint"), 180, Style.EMPTY)) {
            lines.add(Text.literal(line.getString()).formatted(Formatting.GRAY));
        }
        return lines;
    }
}
