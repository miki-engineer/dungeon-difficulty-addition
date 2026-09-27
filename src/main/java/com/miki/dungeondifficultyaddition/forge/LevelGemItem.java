package com.miki.dungeondifficultyaddition.forge;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.Formatting;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class LevelGemItem extends Item {
    private static final String LEVEL = "dungeon_difficulty_addition.gem_level";
    // Installed by client setup; dedicated servers never reference client input classes.
    private static BooleanSupplier detailsKeyDown = () -> false;
    public static void setDetailsKeyCheck(BooleanSupplier check) { detailsKeyDown = check; }
    private final GemKind kind;
    private final boolean fragment;

    public LevelGemItem() {
        this(GemKind.WEAPON);
    }

    public LevelGemItem(GemKind kind) {
        this(kind, false);
    }

    public LevelGemItem(GemKind kind, boolean fragment) {
        super(new Settings().maxCount(64).component(DataComponentTypes.CUSTOM_DATA, data(1)));
        this.kind = kind;
        this.fragment = fragment;
    }

    public GemKind kind() { return kind; }
    public boolean isFragment() { return fragment; }

    private static NbtComponent data(int level) {
        var nbt = new NbtCompound();
        nbt.putInt(LEVEL, level);
        return NbtComponent.of(nbt);
    }

    public static int level(ItemStack stack) {
        if (!(stack.getItem() instanceof LevelGemItem)) return 0;
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        int level = data == null ? 0 : data.copyNbt().getInt(LEVEL);
        return ForgeRules.validLevel(level) ? level : 0;
    }

    public static ItemStack create(int level, int count) {
        return create(GemKind.WEAPON, level, count);
    }

    public static ItemStack create(GemKind kind, int level, int count) {
        return create(kind, level, count, false);
    }

    public static ItemStack createFragment(GemKind kind, int level, int count) {
        return create(kind, level, count, true);
    }

    private static ItemStack create(GemKind kind, int level, int count, boolean fragment) {
        if (!ForgeRules.validLevel(level) || count < 1 || count > 64) return ItemStack.EMPTY;
        var stack = new ItemStack(fragment ? RunicForge.fragment(kind) : RunicForge.gem(kind), count);
        stack.set(DataComponentTypes.CUSTOM_DATA, data(level));
        return stack;
    }

    @Override public Text getName(ItemStack stack) {
        var name = Text.translatable("item.dungeon_difficulty_addition." + kind.id()
                + (fragment ? "_level_fragment" : "_level_gem"));
        return Text.translatable("item.dungeon_difficulty_addition.levelled_essence_name",
                name, RomanNumerals.format(level(stack)));
    }

    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (fragment) {
            tooltip.add(Text.translatable("tooltip.dungeon_difficulty_addition.fragment.description").formatted(Formatting.GRAY));
            return;
        }
        tooltip.add(Text.translatable("tooltip.dungeon_difficulty_addition.ascension.description").formatted(Formatting.GRAY));
        tooltip.add(Text.empty());
        if (detailsKeyDown.getAsBoolean()) {
            tooltip.add(Text.translatable("tooltip.dungeon_difficulty_addition.ascension.use." + kind.id()).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.dungeon_difficulty_addition.ascension.limit").formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.dungeon_difficulty_addition.hold_shift").formatted(Formatting.DARK_GRAY));
        }
    }
}
