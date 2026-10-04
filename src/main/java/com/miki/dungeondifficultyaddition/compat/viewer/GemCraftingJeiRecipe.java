package com.miki.dungeondifficultyaddition.compat.viewer;

import com.miki.dungeondifficultyaddition.forge.FragmentGemRecipe;
import com.miki.dungeondifficultyaddition.forge.client.GemCraftingDisplays;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/** Client-only recipe entry for JEI's native crafting category; never registered on the server. */
final class GemCraftingJeiRecipe extends SpecialCraftingRecipe {
    final GemCraftingDisplays.Display display;
    private final FragmentGemRecipe delegate = new FragmentGemRecipe(CraftingRecipeCategory.MISC);

    GemCraftingJeiRecipe(GemCraftingDisplays.Display display) {
        super(CraftingRecipeCategory.MISC);
        this.display = display;
    }

    @Override public boolean matches(CraftingRecipeInput input, World world) { return delegate.matches(input, world); }
    @Override public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return delegate.craft(input, registries);
    }
    @Override public boolean fits(int width, int height) { return delegate.fits(width, height); }
    @Override public RecipeSerializer<?> getSerializer() { return delegate.getSerializer(); }
}
