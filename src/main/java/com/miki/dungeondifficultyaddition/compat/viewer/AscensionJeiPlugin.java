package com.miki.dungeondifficultyaddition.compat.viewer;

import com.miki.dungeondifficultyaddition.forge.client.AscensionDisplays;
import com.miki.dungeondifficultyaddition.forge.client.GemCraftingDisplays;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.fml.ModList;

@JeiPlugin
public final class AscensionJeiPlugin implements IModPlugin {
    private static final RecipeType<AscensionDisplays.Display> TYPE = RecipeType.create(
            "dungeon_difficulty_addition", "ascension", AscensionDisplays.Display.class);
    public Identifier getPluginUid() { return Identifier.of("dungeon_difficulty_addition", "ascension"); }
    // EMI has its own implementation; avoid duplicate categories through its JEI bridge.
    private static boolean enabled() { return !ModList.get().isLoaded("emi"); }
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (enabled()) {
            var gui = registration.getJeiHelpers().getGuiHelper();
            registration.addRecipeCategories(new Category(gui.createDrawableItemStack(new ItemStack(Items.ANVIL)),
                    gui.getRecipePlusSign(), gui.getRecipeArrow()));
        }
    }
    public void registerRecipes(IRecipeRegistration registration) {
        if (enabled()) {
            registration.addRecipes(TYPE, AscensionDisplays.create());
            registration.addRecipes(RecipeTypes.CRAFTING, GemCraftingDisplays.create().stream()
                    .map(display -> new RecipeEntry<CraftingRecipe>(Identifier.of("dungeon_difficulty_addition",
                            "gem_crafting/" + display.kind().id()), new GemCraftingJeiRecipe(display))).toList());
        }
    }
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (enabled()) {
            registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), TYPE);
        }
    }
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        if (enabled()) registration.getCraftingCategory().addExtension(GemCraftingJeiRecipe.class, new GemExtension());
    }
    private static final class GemExtension implements ICraftingCategoryExtension<GemCraftingJeiRecipe> {
        public void setRecipe(RecipeEntry<GemCraftingJeiRecipe> entry, IRecipeLayoutBuilder builder,
                              ICraftingGridHelper grid, IFocusGroup focuses) {
            var display = entry.value().display;
            var inputs = java.util.Collections.nCopies(4, display.fragments());
            var slots = grid.createAndSetInputs(builder, inputs, 0, 0);
            var output = grid.createAndSetOutputs(builder, display.gems());
            builder.createFocusLink(slots.get(0), slots.get(1), slots.get(3), slots.get(4), output);
        }
    }
    private record Category(IDrawable icon, IDrawable plus, IDrawable arrow) implements IRecipeCategory<AscensionDisplays.Display> {
        public RecipeType<AscensionDisplays.Display> getRecipeType() { return TYPE; }
        public Text getTitle() { return Text.translatable("emi.category.dungeon_difficulty_addition.ascension"); }
        public IDrawable getIcon() { return icon; }
        public int getWidth() { return 125; }
        public int getHeight() { return 22; }
        public void setRecipe(IRecipeLayoutBuilder builder, AscensionDisplays.Display recipe, IFocusGroup focuses) {
            var input = builder.addInputSlot(1, 3).addItemStacks(recipe.inputs()).setStandardSlotBackground();
            var gem = builder.addInputSlot(50, 3).addItemStacks(recipe.gems()).setStandardSlotBackground();
            var output = builder.addOutputSlot(108, 3).addItemStacks(recipe.outputs()).setStandardSlotBackground();
            builder.createFocusLink(input, gem, output);
        }
        public void draw(AscensionDisplays.Display recipe, IRecipeSlotsView slots, DrawContext context, double mx, double my) {
            plus.draw(context, 27, 4);
            arrow.draw(context, 77, 3);
        }
    }
}
