package com.miki.dungeondifficultyaddition.forge;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;

public final class SalvageHammerItem extends Item {
    private final HammerTier tier;
    public SalvageHammerItem(HammerTier tier) {
        super(new Settings().maxDamage(SalvageConfig.get().durability(tier)));
        this.tier = tier;
    }
    public boolean accepts(int level) {
        var config = SalvageConfig.get();
        return tier.accepts(level, config.gold_max_level, config.diamond_max_level);
    }
    public int maximumLevel() {
        var config = SalvageConfig.get();
        return tier == HammerTier.NETHERITE ? ForgeRules.MAX_LEVEL
                : tier == HammerTier.GOLD ? config.gold_max_level : config.diamond_max_level;
    }
    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        ForgeTooltips.addWrapped(tooltip, (tier == HammerTier.NETHERITE
                ? Text.translatable("salvage.dungeon_difficulty_addition.unlimited")
                : Text.translatable("salvage.dungeon_difficulty_addition.limit", maximumLevel()))
                .formatted(Formatting.GRAY));
        tooltip.add(Text.empty());
        if (ForgeTooltips.showDetails()) {
            ForgeTooltips.addWrapped(tooltip, Text.translatable("salvage.dungeon_difficulty_addition.place_hint").formatted(Formatting.GRAY));
            ForgeTooltips.addWrapped(tooltip, Text.translatable("salvage.dungeon_difficulty_addition.hammer_hint").formatted(Formatting.GRAY));
        } else ForgeTooltips.addHint(tooltip);
    }
}
