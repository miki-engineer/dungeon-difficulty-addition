package com.miki.dungeondifficultyaddition.forge.client;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import com.miki.dungeondifficultyaddition.readiness.EncounterConfig;
import com.miki.dungeondifficultyaddition.readiness.EquippedLevels;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

/** Survival inventory tab, computed from the local player's synced armor, weapon and Curios. */
final class GearScoreDisplay {
    // Vanilla's unselected creative tab: darker, and sitting behind the panel. Vanilla draws
    // the panel over its bottom rows; drawn after the panel, the tab stops at the panel's top edge instead.
    private static final Identifier TAB = Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_7");
    private static final Identifier ICON = Identifier.of(DungeonDifficultyAddition.MOD_ID, "gear_score");
    private static final int SPRITE_WIDTH = 26, SPRITE_HEIGHT = 32, HIDDEN_BOTTOM = 4, PANEL_WIDTH = 176;
    // Clear of the panel's rounded top-right corner, so the corner stays whole.
    private static final int CORNER_INSET = 4;
    // Stretch the plain middle column, and drop plain middle rows, so corners and bevels keep their pixel art.
    private static final int EDGE = 4, MIDDLE_COLUMN = 12, TOP_CAP = 6, CUT_ROWS = 4;
    private static final int TAB_HEIGHT = SPRITE_HEIGHT - HIDDEN_BOTTOM - CUT_ROWS;
    // Below the sprite's 2 transparent rows and its bevel; the value sits clear of the right bevel.
    private static final int ICON_SIZE = 16, ICON_X = 5, ICON_Y = 6, FACE_TOP = 5, GAP = 3, RIGHT_PADDING = 6;
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
        int width = Math.max(SPRITE_WIDTH, ICON_X + ICON_SIZE + GAP + font.getWidth(value) + RIGHT_PADDING);
        int right = screen.getGuiLeft() + PANEL_WIDTH - CORNER_INSET;
        int left = right - width;
        int top = screen.getGuiTop() - TAB_HEIGHT;
        var context = event.getGuiGraphics();
        drawTab(context, left, top, width);
        boolean hovered = event.getMouseX() >= left && event.getMouseX() < right
                && event.getMouseY() >= top + FACE_TOP - 3 && event.getMouseY() < screen.getGuiTop();
        // Inside the bevels only, so the outline and corners stay crisp.
        if (hovered) context.fill(left + 3, top + FACE_TOP, right - 3, screen.getGuiTop(), HOVER);
        int iconX = left + ICON_X, iconY = top + ICON_Y;
        context.drawGuiTexture(ICON, iconX, iconY, ICON_SIZE, ICON_SIZE);
        // White with shadow, like text on vanilla buttons; vertically centred on the icon.
        context.drawText(font, value, iconX + ICON_SIZE + GAP, iconY + 4, 0xFFFFFF, true);
        if (hovered) context.drawTooltip(font, List.of(
                Text.translatable("gui.dungeon_difficulty_addition.gear_score.title", value).formatted(Formatting.GOLD),
                Text.translatable("gui.dungeon_difficulty_addition.gear_score.hint").formatted(Formatting.GRAY)),
                event.getMouseX(), event.getMouseY());
    }

    private static void drawTab(DrawContext context, int left, int top, int width) {
        drawColumns(context, 0, left, top, EDGE);
        for (int x = left + EDGE; x < left + width - EDGE; x++) drawColumns(context, MIDDLE_COLUMN, x, top, 1);
        drawColumns(context, SPRITE_WIDTH - EDGE, left + width - EDGE, top, EDGE);
    }

    /** The rounded top cap, then the rows below the cut, down to where the panel would cover them. */
    private static void drawColumns(DrawContext context, int u, int x, int top, int width) {
        context.drawGuiTexture(TAB, SPRITE_WIDTH, SPRITE_HEIGHT, u, 0, x, top, width, TOP_CAP);
        context.drawGuiTexture(TAB, SPRITE_WIDTH, SPRITE_HEIGHT, u, TOP_CAP + CUT_ROWS, x, top + TOP_CAP,
                width, TAB_HEIGHT - TOP_CAP);
    }
}
