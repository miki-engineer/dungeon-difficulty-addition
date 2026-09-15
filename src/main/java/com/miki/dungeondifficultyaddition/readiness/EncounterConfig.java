package com.miki.dungeondifficultyaddition.readiness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.List;

/** Separate file so existing accessory settings and pack balance stay intact. Restart to reload. */
public final class EncounterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EncounterConfig instance;
    public Readiness player_readiness = new Readiness();
    public List<String> scopes = List.of("dungeon", "heroic");
    public int max_level = 1000;

    public static final class Readiness {
        public boolean enabled = false;
        public int required_equipped_items = 3;
        public int unmarked_item_level = 0;
        public double incoming_penalty_per_level = 0.25;
        public boolean outgoing_penalty_enabled = true;
        public double outgoing_penalty_per_level = 0.10;
        public double maximum_outgoing_reduction = 0.90;
    }

    public static synchronized EncounterConfig get() {
        if (instance == null) {
            var path = FMLPaths.CONFIGDIR.get().resolve("dungeon_difficulty_addition/encounters.json");
            try {
                if (Files.exists(path)) {
                    try (var reader = Files.newBufferedReader(path)) {
                        instance = GSON.fromJson(reader, EncounterConfig.class);
                    }
                    if (instance == null) throw new IllegalArgumentException("Empty encounters config");
                    instance.validate();
                } else {
                    instance = new EncounterConfig();
                    Files.createDirectories(path.getParent());
                    try (var writer = Files.newBufferedWriter(path)) { GSON.toJson(instance, writer); }
                }
            } catch (Exception exception) {
                instance = new EncounterConfig();
                LoggerFactory.getLogger("dungeon_difficulty_addition").error(
                        "Could not load encounters.json; readiness disabled. File preserved.", exception);
            }
        }
        return instance;
    }

    void validate() {
        if (player_readiness == null || scopes == null
                || scopes.stream().anyMatch(scope -> scope == null || scope.isBlank())
                || max_level < 1 || max_level > 100000
                || player_readiness.required_equipped_items < 1 || player_readiness.required_equipped_items > 128
                || player_readiness.unmarked_item_level < 0 || player_readiness.unmarked_item_level > max_level
                || !validRate(player_readiness.incoming_penalty_per_level)
                || !validRate(player_readiness.outgoing_penalty_per_level)
                || !validRate(player_readiness.maximum_outgoing_reduction)) {
            throw new IllegalArgumentException("Invalid encounters configuration");
        }
    }

    private static boolean validRate(double value) { return Double.isFinite(value) && value >= 0 && value <= 1; }

    public boolean active() { return player_readiness.enabled; }
}
