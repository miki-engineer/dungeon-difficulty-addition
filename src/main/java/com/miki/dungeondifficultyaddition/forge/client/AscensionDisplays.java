package com.miki.dungeondifficultyaddition.forge.client;

import com.miki.dungeondifficultyaddition.forge.*;
import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.dungeon_difficulty.DungeonDifficulty;
import java.util.ArrayList;
import java.util.List;

/** One entry per equipment item, with synchronized consecutive upgrade steps. */
public final class AscensionDisplays {
    public record Step(ItemStack input, ItemStack gem, ItemStack output) {}
    public record Display(Identifier itemId, GemKind kind, List<Step> steps, int xp) {
        public List<ItemStack> inputs() { return steps.stream().map(Step::input).toList(); }
        public List<ItemStack> gems() { return steps.stream().map(Step::gem).toList(); }
        public List<ItemStack> outputs() { return steps.stream().map(Step::output).toList(); }
    }
    private AscensionDisplays() {}
    public static List<Display> create() {
        if (!SalvageConfig.get().enabled || !AccessoryScalingConfig.get().enabled) return List.of();
        var displays = new ArrayList<Display>();
        int maximum = AscensionPreviewLevels.maximum(DungeonDifficulty.config.value);
        for (var item : Registries.ITEM) {
            var input = new ItemStack(item);
            if (!AccessoryItemScaling.isForgeEquipment(input)) continue;
            int level = AccessoryItemScaling.applyCommandLevel(input, 1);
            if (!ForgeRules.validLevel(level)) continue;
            var kind = SalvageEquipment.kind(input);
            if (kind == null) continue;
            var steps = new ArrayList<Step>();
            while (level < maximum) {
                var output = input.copy();
                if (AccessoryItemScaling.applyAnvilLevel(output, level + 1) != level + 1) break;
                steps.add(
                        new Step(input, LevelGemItem.create(kind, level + 1, 1), output));
                input = output;
                level++;
            }
            if (!steps.isEmpty()) displays.add(new Display(Registries.ITEM.getId(item), kind,
                    List.copyOf(steps), SalvageConfig.get().upgrade_xp_levels));
        }
        return List.copyOf(displays);
    }
}
