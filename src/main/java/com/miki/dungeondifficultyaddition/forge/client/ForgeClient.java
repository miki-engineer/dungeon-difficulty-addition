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
        event.enqueueWork(() -> LevelGemItem.setDetailsKeyCheck(Screen::hasShiftDown));
    }
    @SubscribeEvent public static void registerDecorations(RegisterItemDecorationsEvent event) {
        // Eligibility and level are checked per stack: modded equipment needs no item whitelist.
        for (var item : Registries.ITEM) event.register(item, ItemLevelDecoration.INSTANCE);
    }
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RunicForge.PLACED_ITEM.get(), AnvilSalvageRenderer::new);
    }
}
