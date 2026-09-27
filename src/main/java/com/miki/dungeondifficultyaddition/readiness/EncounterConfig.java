package com.miki.dungeondifficultyaddition.readiness;

import com.miki.dungeondifficultyaddition.config.ModSettings;
import java.util.List;

/** The encounters section of settings.json. Restart to reload. */
public final class EncounterConfig {
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

    public static EncounterConfig get() { return ModSettings.get().encounters(); }

    public void validate() {
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
