package com.miki.dungeondifficultyaddition.forge.client;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import com.miki.dungeondifficultyaddition.forge.RunicForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.minecraft.registry.Registries;
import net.minecraft.client.gui.screen.Screen;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.miki.dungeondifficultyaddition.forge.LevelGemItem;

@EventBusSubscriber(modid = DungeonDifficultyAddition.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ForgeClient {
    private ForgeClient() {}
    @SubscribeEvent public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            LevelGemItem.setDetailsKeyCheck(Screen::hasShiftDown);
            LevelGemItem.setTooltipWrapper(ForgeClient::wrapTooltip);
        });
    }
    private static java.util.List<net.minecraft.text.Text> wrapTooltip(net.minecraft.text.Text text) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        int width = Math.min(180, Math.max(40, client.getWindow().getScaledWidth() - 24));
        var result = new java.util.ArrayList<net.minecraft.text.Text>();
        for (var line : client.textRenderer.getTextHandler().wrapLines(text, width, net.minecraft.text.Style.EMPTY)) {
            var wrapped = net.minecraft.text.Text.empty();
            line.visit((style, part) -> {
                wrapped.append(net.minecraft.text.Text.literal(part).setStyle(style));
                return java.util.Optional.empty();
            }, net.minecraft.text.Style.EMPTY);
            result.add(wrapped);
        }
        return result;
    }
    @SubscribeEvent public static void registerDecorations(RegisterItemDecorationsEvent event) {
        // Eligibility and level are checked per stack: modded equipment needs no item whitelist.
        for (var item : Registries.ITEM) event.register(item, ItemLevelDecoration.INSTANCE);
    }
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RunicForge.PLACED_ITEM.get(), AnvilSalvageRenderer::new);
    }
}
