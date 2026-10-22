package com.miki.dungeondifficultyaddition.scaling;

import com.miki.dungeondifficultyaddition.compat.AccessoryFamilies;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry-derived facts per item. Inventory ticks and slot rendering query these for every
 * stack, so derive the ID strings once instead of on every call. Never caches stack state.
 */
public record ItemTraits(String id, String namespace, boolean builtInExcluded,
                         boolean spellContainerName, boolean magicWeaponName) {
    private static final Set<String> BUILT_IN_EXCLUSIONS = Set.of(
            "jewelry:ruby",
            "jewelry:topaz",
            "jewelry:citrine",
            "jewelry:jade",
            "jewelry:sapphire",
            "jewelry:tanzanite",
            "jewelry:jewelers_kit",
            "jewelry:gem_vein",
            "jewelry:deepslate_gem_vein"
    );
    private static final Map<Item, ItemTraits> CACHE = new ConcurrentHashMap<>();

    public static ItemTraits of(ItemStack stack) {
        var item = stack.getItem();
        var cached = CACHE.get(item);
        if (cached != null) return cached;
        var traits = compute(item);
        // Unregistered items report the default ID; do not pin that result.
        if (Registries.ITEM.getKey(item).isPresent()) CACHE.put(item, traits);
        return traits;
    }

    private static ItemTraits compute(Item item) {
        var identifier = Registries.ITEM.getId(item);
        var id = identifier.toString();
        var path = identifier.getPath().toLowerCase(Locale.ROOT);
        var compactPath = path.replace("_", "").replace("-", "");
        return new ItemTraits(
                id,
                identifier.getNamespace(),
                BUILT_IN_EXCLUSIONS.contains(id) || AccessoryFamilies.material(id),
                compactPath.contains("spellbook") || compactPath.contains("spellscroll"),
                // Some caster weapons expose their type only through the registry name.
                path.contains("staff")
                        || path.contains("wand")
                        || path.contains("scepter")
                        || path.contains("spellbook")
                        || path.contains("grimoire")
                        || path.contains("tome")
        );
    }
}
