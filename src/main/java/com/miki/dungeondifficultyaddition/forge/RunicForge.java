package com.miki.dungeondifficultyaddition.forge;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.RegistryKeys;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RunicForge {
    private static final String ID = DungeonDifficultyAddition.MOD_ID;
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RegistryKeys.ITEM, ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(RegistryKeys.ENTITY_TYPE, ID);
    private static final DeferredRegister<net.minecraft.recipe.RecipeSerializer<?>> RECIPES = DeferredRegister.create(RegistryKeys.RECIPE_SERIALIZER, ID);
    public static final DeferredHolder<net.minecraft.recipe.RecipeSerializer<?>, net.minecraft.recipe.SpecialRecipeSerializer<FragmentGemRecipe>> FRAGMENT_RECIPE =
            RECIPES.register("fragment_gem", () -> new net.minecraft.recipe.SpecialRecipeSerializer<>(FragmentGemRecipe::new));
    public static final DeferredHolder<Item, SalvageHammerItem> GOLD_HAMMER = ITEMS.register("gold_salvage_hammer", () -> new SalvageHammerItem(HammerTier.GOLD));
    public static final DeferredHolder<Item, SalvageHammerItem> DIAMOND_HAMMER = ITEMS.register("diamond_salvage_hammer", () -> new SalvageHammerItem(HammerTier.DIAMOND));
    public static final DeferredHolder<Item, SalvageHammerItem> NETHERITE_HAMMER = ITEMS.register("netherite_salvage_hammer", () -> new SalvageHammerItem(HammerTier.NETHERITE));
    // Keep old gem stacks loadable, but do not expose the retired prototype in creative tabs.
    public static final DeferredHolder<Item, LevelGemItem> LEVEL_GEM = ITEMS.register("level_gem", () -> new LevelGemItem());
    public static final DeferredHolder<Item, LevelGemItem> WEAPON_GEM = ITEMS.register("weapon_level_gem", () -> new LevelGemItem(GemKind.WEAPON));
    public static final DeferredHolder<Item, LevelGemItem> ARMOR_GEM = ITEMS.register("armor_level_gem", () -> new LevelGemItem(GemKind.ARMOR));
    public static final DeferredHolder<Item, LevelGemItem> ACCESSORY_GEM = ITEMS.register("accessory_level_gem", () -> new LevelGemItem(GemKind.ACCESSORY));
    public static final DeferredHolder<Item, LevelGemItem> NEBULA_GEM = ITEMS.register("nebula_level_gem", () -> new LevelGemItem(GemKind.NEBULA));
    public static final DeferredHolder<Item, LevelGemItem> WEAPON_FRAGMENT = ITEMS.register("weapon_level_fragment", () -> new LevelGemItem(GemKind.WEAPON, true));
    public static final DeferredHolder<Item, LevelGemItem> ARMOR_FRAGMENT = ITEMS.register("armor_level_fragment", () -> new LevelGemItem(GemKind.ARMOR, true));
    public static final DeferredHolder<Item, LevelGemItem> ACCESSORY_FRAGMENT = ITEMS.register("accessory_level_fragment", () -> new LevelGemItem(GemKind.ACCESSORY, true));
    public static final DeferredHolder<Item, LevelGemItem> NEBULA_FRAGMENT = ITEMS.register("nebula_level_fragment", () -> new LevelGemItem(GemKind.NEBULA, true));
    public static final DeferredHolder<EntityType<?>, EntityType<AnvilSalvageEntity>> PLACED_ITEM =
            ENTITIES.register("anvil_salvage_item", () -> EntityType.Builder.<AnvilSalvageEntity>create(
                    AnvilSalvageEntity::new, SpawnGroup.MISC).dimensions(.45F, .15F)
                    .maxTrackingRange(8).trackingTickInterval(1).build(ID + ":anvil_salvage_item"));

    private RunicForge() {}
    public static Item gem(GemKind kind) {
        return switch (kind) {
            case WEAPON -> WEAPON_GEM.get();
            case ARMOR -> ARMOR_GEM.get();
            case ACCESSORY -> ACCESSORY_GEM.get();
            case NEBULA -> NEBULA_GEM.get();
        };
    }
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        ENTITIES.register(bus);
        RECIPES.register(bus);
        bus.addListener(RunicForge::creativeItems);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, AnvilSalvageEvents::useBlock);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, AnvilSalvageEvents::hitBlock);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, AnvilSalvageEvents::hitEntity);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, AnvilUpgradeEvents::update);
    }
    public static Item fragment(GemKind kind) {
        return switch (kind) {
            case WEAPON -> WEAPON_FRAGMENT.get();
            case ARMOR -> ARMOR_FRAGMENT.get();
            case ACCESSORY -> ACCESSORY_FRAGMENT.get();
            case NEBULA -> NEBULA_FRAGMENT.get();
        };
    }
    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ItemGroups.TOOLS)) {
            event.add(GOLD_HAMMER.get()); event.add(DIAMOND_HAMMER.get()); event.add(NETHERITE_HAMMER.get());
        }
        if (event.getTabKey().equals(ItemGroups.INGREDIENTS)) {
            // Keep creative-tab and recipe-viewer ordering grouped by form, then equipment kind.
            for (var kind : GemKind.values()) event.add(gem(kind));
            for (var kind : GemKind.values()) event.add(fragment(kind));
        }
    }
}
