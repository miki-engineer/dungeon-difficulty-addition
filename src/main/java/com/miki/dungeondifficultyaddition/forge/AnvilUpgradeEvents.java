package com.miki.dungeondifficultyaddition.forge;

import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.text.Text;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

public final class AnvilUpgradeEvents {
    private AnvilUpgradeEvents() {}
    public static void update(AnvilUpdateEvent event) {
        if (!(event.getRight().getItem() instanceof LevelGemItem gem)) return;
        // Never allow invalid gem combinations to fall through into another anvil operation.
        var left = event.getLeft();
        int target = LevelGemItem.level(event.getRight());
        if (!SalvageConfig.get().enabled || gem.isFragment() || left.getCount() != 1
                || !UpgradeRules.canUpgrade(SalvageEquipment.kind(left), ItemScaling.getScaleFactor(left), gem.kind(), target)) {
            event.setCanceled(true);
            return;
        }
        var output = left.copy();
        if (AccessoryItemScaling.applyAnvilLevel(output, target) != target) {
            event.setCanceled(true);
            return;
        }
        // Match vanilla rename semantics; all other components come from the original stack.
        var name = event.getName();
        if (name != null) {
            if (name.isBlank()) output.remove(DataComponentTypes.CUSTOM_NAME);
            else if (!name.equals(left.getName().getString())) output.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        }
        event.setOutput(output);
        event.setMaterialCost(1);
        event.setCost(SalvageConfig.get().upgrade_xp_levels);
    }
}
