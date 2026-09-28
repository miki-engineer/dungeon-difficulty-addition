package com.miki.dungeondifficultyaddition.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.miki.dungeondifficultyaddition.forge.client.ItemLevelDecoration;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Only suppress our badge on floating cursor items, never normal slot decorations. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenCursorMixin {
    @WrapMethod(method = "drawItem")
    private void dungeonDifficultyAddition$hideCursorBadge(DrawContext context, ItemStack stack,
            int x, int y, String amountText, Operation<Void> original) {
        ItemLevelDecoration.withoutCursorBadge(() -> original.call(context, stack, x, y, amountText));
    }
}
