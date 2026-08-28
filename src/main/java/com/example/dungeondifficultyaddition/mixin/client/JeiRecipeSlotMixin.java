package com.example.dungeondifficultyaddition.mixin.client;

import com.example.dungeondifficultyaddition.FixedLevelPreview;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Pseudo
@Mixin(targets = "mezz.jei.library.gui.ingredients.RecipeSlot", remap = false)
public abstract class JeiRecipeSlotMixin {
    @Shadow(remap = false)
    @Final
    private RecipeIngredientRole role;

    @Unique
    private ITypedIngredient<?> ddJewelryCompat$cachedSource;
    @Unique
    private ITypedIngredient<?> ddJewelryCompat$cachedPreview;

    @Inject(method = "getDisplayedIngredient", at = @At("RETURN"), cancellable = true, remap = false)
    private void ddJewelryCompat$scaleRecipeOutputPreview(
            CallbackInfoReturnable<Optional<ITypedIngredient<?>>> cir
    ) {
        if (role != RecipeIngredientRole.OUTPUT || cir.getReturnValue().isEmpty()) {
            return;
        }

        var displayed = cir.getReturnValue().get();
        if (displayed == ddJewelryCompat$cachedSource && ddJewelryCompat$cachedPreview != null) {
            cir.setReturnValue(Optional.of(ddJewelryCompat$cachedPreview));
            return;
        }

        var stack = displayed.getItemStack().orElse(null);
        var preview = FixedLevelPreview.scaledCopy(stack);
        if (preview == stack) {
            return;
        }

        ddJewelryCompat$cachedSource = displayed;
        ddJewelryCompat$cachedPreview = new PreviewIngredient(preview);
        cir.setReturnValue(Optional.of(ddJewelryCompat$cachedPreview));
    }

    private record PreviewIngredient(ItemStack stack) implements ITypedIngredient<ItemStack> {
        @Override
        public mezz.jei.api.ingredients.IIngredientType<ItemStack> getType() {
            return VanillaTypes.ITEM_STACK;
        }

        @Override
        public ItemStack getIngredient() {
            return stack;
        }
    }
}
