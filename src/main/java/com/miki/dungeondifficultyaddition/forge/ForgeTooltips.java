package com.miki.dungeondifficultyaddition.forge;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** Shared presentation with client hooks installed at setup; safe on dedicated servers. */
public final class ForgeTooltips {
    private static BooleanSupplier detailsKeyDown = () -> false;
    private static Function<Text, List<Text>> wrapper = List::of;
    private ForgeTooltips() {}
    public static void setDetailsKeyCheck(BooleanSupplier check) { detailsKeyDown = check; }
    public static void setTooltipWrapper(Function<Text, List<Text>> wrap) { wrapper = wrap; }
    public static boolean showDetails() { return detailsKeyDown.getAsBoolean(); }
    public static void addWrapped(List<Text> tooltip, Text text) { tooltip.addAll(wrapper.apply(text)); }
    public static void addHint(List<Text> tooltip) {
        addWrapped(tooltip, Text.translatable("tooltip.dungeon_difficulty_addition.hold_shift").formatted(Formatting.DARK_GRAY));
    }
}
