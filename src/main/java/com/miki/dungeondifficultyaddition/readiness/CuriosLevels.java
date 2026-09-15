package com.miki.dungeondifficultyaddition.readiness;

import net.minecraft.entity.player.PlayerEntity;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.List;

/** Loaded only when Curios is installed. Cosmetic and inactive slots are intentionally excluded. */
final class CuriosLevels {
    static void append(PlayerEntity player, List<EquippedLevels.Slot> result) {
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory.getCurios().forEach((name, handler) -> {
            var stacks = handler.getStacks();
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (inventory.isSlotActive(name, index)) {
                    EquippedLevels.add(result, "curios:" + name + "/" + index, stacks.getStackInSlot(index));
                }
            }
        }));
    }
}
