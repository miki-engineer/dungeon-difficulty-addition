package com.miki.dungeondifficultyaddition.forge;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import java.util.List;
import static com.miki.dungeondifficultyaddition.forge.ForgeTooltips.addWrapped;

public final class LevelGemItem extends Item {
    private static final String LEVEL = "dungeon_difficulty_addition.gem_level";
    private final GemKind kind;
    private final boolean fragment;

    public LevelGemItem() {
        this(GemKind.WEAPON);
    }

    public LevelGemItem(GemKind kind) {
        this(kind, false);
    }

    public LevelGemItem(GemKind kind, boolean fragment) {
        super(new Settings().maxCount(64).rarity(kind == GemKind.NEBULA
                        ? (fragment ? Rarity.UNCOMMON : Rarity.EPIC)
                        : (fragment ? Rarity.COMMON : Rarity.UNCOMMON))
                .component(DataComponentTypes.CUSTOM_DATA, data(1)));
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
        // Read only: no need to copy the compound for every rendered icon or tooltip.
        int level = data == null ? 0 : data.getNbt().getInt(LEVEL);
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
        return Text.translatable("item.dungeon_difficulty_addition." + kind.id()
                + (fragment ? "_level_fragment" : "_level_gem"));
    }

    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        // Match Dungeon Difficulty's presentation without assigning native equipment scaling.
        int essenceLevel = level(stack);
        if (essenceLevel > 0) {
            tooltip.add(Text.translatable("item.power.level", essenceLevel)
                    .formatted(net.minecraft.entity.attribute.EntityAttribute.Category.POSITIVE.getFormatting(true)));
            tooltip.add(Text.empty());
        }
        if (fragment) {
            addWrapped(tooltip, Text.translatable(kind == GemKind.NEBULA
                    ? "tooltip.dungeon_difficulty_addition.nebula.fragment_description"
                    : "tooltip.dungeon_difficulty_addition.fragment.description").formatted(Formatting.GRAY));
            return;
        }
        addWrapped(tooltip, Text.translatable(kind == GemKind.NEBULA
                ? "tooltip.dungeon_difficulty_addition.nebula.description"
                : "tooltip.dungeon_difficulty_addition.ascension.description").formatted(Formatting.GRAY));
        tooltip.add(Text.empty());
        if (ForgeTooltips.showDetails()) {
            addWrapped(tooltip, Text.translatable("tooltip.dungeon_difficulty_addition.ascension.use." + kind.id()).formatted(Formatting.GRAY));
            addWrapped(tooltip, Text.translatable("tooltip.dungeon_difficulty_addition.ascension.limit").formatted(Formatting.DARK_GRAY));
        } else {
            ForgeTooltips.addHint(tooltip);
        }
    }
}
