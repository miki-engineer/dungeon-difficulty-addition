package com.miki.dungeondifficultyaddition.forge;

import com.miki.dungeondifficultyaddition.config.ModSettings;
import java.util.List;

/** Restart to reload. Netherite deliberately has no configurable level ceiling. */
public final class SalvageConfig {
    public boolean enabled = true;
    public int upgrade_xp_levels = 5;
    /** Exact item IDs: these equipment items yield and require Nebula materials. */
    // Arsenal 1.5.0 equipment. Absent mods are harmless; users can edit or clear this list.
    public List<String> legendary_items = List.of(
            "arsenal:unique_claymore_1",
            "arsenal:unique_claymore_2",
            "arsenal:unique_claymore_sw",
            "arsenal:unique_dagger_1",
            "arsenal:unique_dagger_2",
            "arsenal:unique_dagger_sw",
            "arsenal:unique_double_axe_1",
            "arsenal:unique_double_axe_2",
            "arsenal:unique_double_axe_sw",
            "arsenal:unique_glaive_1",
            "arsenal:unique_glaive_2",
            "arsenal:unique_glaive_sw",
            "arsenal:unique_hammer_1",
            "arsenal:unique_hammer_2",
            "arsenal:unique_hammer_sw",
            "arsenal:unique_heavy_crossbow_1",
            "arsenal:unique_heavy_crossbow_2",
            "arsenal:unique_heavy_crossbow_sw",
            "arsenal:unique_longbow_1",
            "arsenal:unique_longbow_2",
            "arsenal:unique_longbow_sw",
            "arsenal:unique_mace_1",
            "arsenal:unique_mace_2",
            "arsenal:unique_mace_sw",
            "arsenal:unique_shield_1",
            "arsenal:unique_shield_2",
            "arsenal:unique_shield_sw",
            "arsenal:unique_sickle_1",
            "arsenal:unique_sickle_2",
            "arsenal:unique_sickle_sw",
            "arsenal:unique_spear_1",
            "arsenal:unique_spear_2",
            "arsenal:unique_spear_sw",
            "arsenal:unique_staff_heal_1",
            "arsenal:unique_staff_heal_2",
            "arsenal:unique_staff_heal_sw",
            "arsenal:unique_longsword_sw",
            "arsenal:unique_staff_damage_1",
            "arsenal:unique_staff_damage_2",
            "arsenal:unique_staff_damage_3",
            "arsenal:unique_staff_damage_4",
            "arsenal:unique_staff_damage_5",
            "arsenal:unique_staff_damage_6",
            "arsenal:unique_staff_damage_sw");
    public boolean isLegendary(String itemId) { return legendary_items.contains(itemId); }
    public int gold_max_level = 3, diamond_max_level = 5;
    public int gold_durability = 32, diamond_durability = 1561, netherite_durability = 2031;
    public static SalvageConfig get() { return ModSettings.get().anvil(); }
    public void validate() {
        if (legendary_items == null || legendary_items.stream().anyMatch(id -> id == null
                || !id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")))
            throw new IllegalArgumentException("anvil.legendary_items must contain exact item IDs such as mod_id:item_name");
        if (upgrade_xp_levels < 1 || upgrade_xp_levels > 39 || gold_max_level < 1 || diamond_max_level < gold_max_level
                || gold_durability < 1 || diamond_durability <= gold_durability
                || netherite_durability <= diamond_durability)
            throw new IllegalArgumentException("Hammer limits must be positive and durability must increase by tier");
    }
    public int durability(HammerTier tier) {
        return switch (tier) { case GOLD -> gold_durability; case DIAMOND -> diamond_durability; case NETHERITE -> netherite_durability; };
    }
}
