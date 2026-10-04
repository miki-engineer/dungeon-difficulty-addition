package com.miki.dungeondifficultyaddition.forge.client;

import com.miki.dungeondifficultyaddition.forge.GemKind;
import com.miki.dungeondifficultyaddition.forge.LevelGemItem;
import net.dungeon_difficulty.DungeonDifficulty;
import net.minecraft.item.ItemStack;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/** One display per kind; all four occupied crafting slots and the result share a level. */
public final class GemCraftingDisplays {
    public record Display(GemKind kind, List<ItemStack> fragments, List<ItemStack> gems) {}
    private GemCraftingDisplays() {}

    public static List<Display> create() {
        int maximum = AscensionPreviewLevels.maximum(DungeonDifficulty.config.value);
        return Arrays.stream(GemKind.values()).map(kind -> new Display(kind,
                IntStream.rangeClosed(1, maximum).mapToObj(level -> LevelGemItem.createFragment(kind, level, 1)).toList(),
                IntStream.rangeClosed(1, maximum).mapToObj(level -> LevelGemItem.create(kind, level, 1)).toList())).toList();
    }
}
