package com.miki.dungeondifficultyaddition.compat.viewer;

import com.miki.dungeondifficultyaddition.forge.client.AscensionDisplays;
import com.miki.dungeondifficultyaddition.forge.client.GemCraftingDisplays;
import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.*;
import dev.emi.emi.api.render.EmiTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import java.util.List;

@EmiEntrypoint
public final class AscensionEmiPlugin implements EmiPlugin {
    private static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            Identifier.of("dungeon_difficulty_addition", "ascension"), EmiStack.of(Items.ANVIL));
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(Items.ANVIL));
        for (var display : AscensionDisplays.create()) registry.addRecipe(new Recipe(display));
        for (var display : GemCraftingDisplays.create()) registry.addRecipe(new GemRecipe(display));
    }
    private static final class GemRecipe implements EmiRecipe {
        private final GemCraftingDisplays.Display display;
        private final List<EmiStack> fragments, gems;
        GemRecipe(GemCraftingDisplays.Display display) {
            this.display = display;
            fragments = display.fragments().stream().map(EmiStack::of).toList();
            gems = display.gems().stream().map(EmiStack::of).toList();
        }
        public EmiRecipeCategory getCategory() { return VanillaEmiRecipeCategories.CRAFTING; }
        public Identifier getId() {
            return Identifier.of("dungeon_difficulty_addition", "/gem_crafting/" + display.kind().id());
        }
        public List<EmiIngredient> getInputs() {
            return java.util.stream.IntStream.range(0, 4).mapToObj(i -> EmiIngredient.of(fragments)).toList();
        }
        public List<EmiStack> getOutputs() { return gems; }
        public int getDisplayWidth() { return 118; }
        public int getDisplayHeight() { return 54; }
        // Cycling levels are alternatives, not multiple simultaneous outputs. No unsafe auto-fill.
        public boolean supportsRecipeTree() { return false; }
        public void addWidgets(WidgetHolder widgets) {
            int[] frame = {0};
            widgets.addDrawable(0, 0, 0, 0, (context, mx, my, delta) ->
                    frame[0] = (int) ((Util.getMeasuringTimeMs() / 2000) % fragments.size()));
            for (int i = 0; i < 9; i++) {
                if (i < 4) {
                    widgets.add(new SlotWidget(fragments.getFirst(), (i % 3) * 18, (i / 3) * 18) {
                        public EmiIngredient getStack() { return fragments.get(frame[0]); }
                    });
                } else {
                    widgets.addSlot(EmiStack.of(ItemStack.EMPTY), (i % 3) * 18, (i / 3) * 18);
                }
            }
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);
            widgets.addTexture(EmiTexture.SHAPELESS, 97, 0);
            widgets.add(new SlotWidget(gems.getFirst(), 92, 14) {
                public EmiIngredient getStack() { return gems.get(frame[0]); }
            }.large(true).recipeContext(this));
        }
    }
    private static final class Recipe implements EmiRecipe {
        private final AscensionDisplays.Display display;
        private final List<EmiStack> inputs, gems, outputs;
        Recipe(AscensionDisplays.Display display) {
            this.display = display;
            inputs = stacks(display.inputs()); gems = stacks(display.gems()); outputs = stacks(display.outputs());
        }
        private static List<EmiStack> stacks(List<ItemStack> stacks) { return stacks.stream().map(EmiStack::of).toList(); }
        public EmiRecipeCategory getCategory() { return CATEGORY; }
        // Leading '/' tells EMI this is a synthetic display, not a server recipe-manager entry.
        public Identifier getId() {
            return Identifier.of("dungeon_difficulty_addition", "/ascension/"
                    + display.itemId().getNamespace() + "/" + display.itemId().getPath());
        }
        public List<EmiIngredient> getInputs() { return List.of(EmiIngredient.of(inputs), EmiIngredient.of(gems)); }
        public List<EmiStack> getOutputs() { return outputs; }
        public int getDisplayWidth() { return 125; }
        public int getDisplayHeight() { return 22; }
        public boolean supportsRecipeTree() { return false; }
        public void addWidgets(WidgetHolder widgets) {
            // Sample once per frame before rendering slots so all three use exactly the same step.
            int[] frame = {0};
            widgets.addDrawable(0, 0, 0, 0, (context, mx, my, delta) ->
                    frame[0] = (int) ((Util.getMeasuringTimeMs() / 2000) % inputs.size()));
            widgets.add(new SlotWidget(inputs.getFirst(), 0, 2) {
                public EmiIngredient getStack() { return inputs.get(frame[0]); }
            });
            widgets.addTexture(EmiTexture.PLUS, 27, 4);
            widgets.add(new SlotWidget(gems.getFirst(), 49, 2) {
                public EmiIngredient getStack() { return gems.get(frame[0]); }
            });
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 77, 3);
            widgets.add(new SlotWidget(outputs.getFirst(), 107, 2) {
                public EmiIngredient getStack() { return outputs.get(frame[0]); }
            }.recipeContext(this));
        }
    }
}
