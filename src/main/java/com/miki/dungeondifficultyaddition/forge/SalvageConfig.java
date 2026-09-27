package com.miki.dungeondifficultyaddition.forge;

import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Files;

/** Restart to reload. Netherite deliberately has no configurable level ceiling. */
public final class SalvageConfig {
    private static SalvageConfig instance;
    public boolean enabled = true;
    public int upgrade_xp_levels = 1;
    public int gold_max_level = 3, diamond_max_level = 5;
    public int gold_durability = 32, diamond_durability = 1561, netherite_durability = 2031;
    public static synchronized SalvageConfig get() {
        if (instance != null) return instance;
        var path = FMLPaths.CONFIGDIR.get().resolve("dungeon_difficulty_addition/anvil_salvage.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(path)) {
                try (var reader = Files.newBufferedReader(path)) { instance = gson.fromJson(reader, SalvageConfig.class); }
                if (instance == null) throw new IllegalArgumentException("Empty salvage config");
                instance.validate();
            } else {
                instance = new SalvageConfig();
                Files.createDirectories(path.getParent());
                try (var writer = Files.newBufferedWriter(path)) { gson.toJson(instance, writer); }
            }
        } catch (Exception exception) {
            instance = new SalvageConfig(); instance.enabled = false;
            org.slf4j.LoggerFactory.getLogger("dungeon_difficulty_addition").error(
                    "Invalid anvil_salvage.json: salvage disabled; config preserved", exception);
        }
        return instance;
    }
    void validate() {
        if (upgrade_xp_levels < 1 || upgrade_xp_levels > 39 || gold_max_level < 1 || diamond_max_level < gold_max_level
                || gold_durability < 1 || diamond_durability <= gold_durability
                || netherite_durability <= diamond_durability)
            throw new IllegalArgumentException("Hammer limits must be positive and durability must increase by tier");
    }
    public int durability(HammerTier tier) {
        return switch (tier) { case GOLD -> gold_durability; case DIAMOND -> diamond_durability; case NETHERITE -> netherite_durability; };
    }
}
