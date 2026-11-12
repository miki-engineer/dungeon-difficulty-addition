package com.miki.dungeondifficultyaddition.forge;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import java.util.List;

public final class SalvageHammerItem extends Item {
    private static final SalvageConfig DEFAULTS = new SalvageConfig();
    private final HammerTier tier;
    public SalvageHammerItem(HammerTier tier) {
        // Item defaults are not synced, so a configured default made each side use its own
        // settings.json. Register the built-in value and let the server patch each stack instead.
        super(new Settings().maxDamage(DEFAULTS.durability(tier)));
        this.tier = tier;
    }
    @Override public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) return;
        int durability = SalvageConfig.get().durability(tier);
        // Setting the built-in value clears the patch again, so this settles after one tick.
        if (stack.getMaxDamage() != durability) stack.set(DataComponentTypes.MAX_DAMAGE, durability);
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
