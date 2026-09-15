package com.miki.dungeondifficultyaddition.readiness;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncounterConfigTest {
    @Test void readinessIsOptIn() {
        var config = new EncounterConfig();
        config.validate();
        assertFalse(config.player_readiness.enabled);
        assertFalse(config.active());
        config.player_readiness.enabled = true;
        assertTrue(config.active());
    }

    @Test void malformedOrDangerousValuesAreRejected() {
        var config = new EncounterConfig();
        config.player_readiness.outgoing_penalty_per_level = Double.NaN;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.player_readiness.outgoing_penalty_per_level = .1;
        config.player_readiness.maximum_outgoing_reduction = 1.1;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.player_readiness.maximum_outgoing_reduction = .9;
        config.player_readiness.required_equipped_items = 0;
        assertThrows(IllegalArgumentException.class, config::validate);
    }
}
