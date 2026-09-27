package com.miki.dungeondifficultyaddition.forge;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import java.util.ArrayList;

/** Four occupied slots; vanilla crafting consumes one fragment from each slot. */
public final class FragmentGemRecipe extends SpecialCraftingRecipe {
    public FragmentGemRecipe(CraftingRecipeCategory category) { super(category); }
    private UpgradeRules.Fragment ingredients(CraftingRecipeInput input) {
        var fragments = new ArrayList<UpgradeRules.Fragment>();
        for (var stack : input.getStacks()) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof LevelGemItem item) || !item.isFragment()) return null;
            fragments.add(new UpgradeRules.Fragment(item.kind(), LevelGemItem.level(stack)));
        }
        return UpgradeRules.matchingFragments(fragments) ? fragments.getFirst() : null;
    }
    @Override public boolean matches(CraftingRecipeInput input, World world) { return ingredients(input) != null; }
    @Override public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        var fragment = ingredients(input);
        return fragment == null ? ItemStack.EMPTY : LevelGemItem.create(fragment.kind(), fragment.level(), 1);
    }
    @Override public boolean fits(int width, int height) { return width * height >= 4; }
    @Override public RecipeSerializer<?> getSerializer() { return RunicForge.FRAGMENT_RECIPE.get(); }
}
