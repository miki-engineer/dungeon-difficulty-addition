package com.miki.dungeondifficultyaddition.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModSettingsTest {
    @Test void attributePatternsAreCheckedWhenSettingsLoad() {
        assertDoesNotThrow(() -> ModSettings.requireValidAttributePattern(".*"));
        assertDoesNotThrow(() -> ModSettings.requireValidAttributePattern("minecraft:generic.armor"));
        assertDoesNotThrow(() -> ModSettings.requireValidAttributePattern("*"));
        assertDoesNotThrow(() -> ModSettings.requireValidAttributePattern(""));
        assertDoesNotThrow(() -> ModSettings.requireValidAttributePattern(null));
        var error = assertThrows(IllegalArgumentException.class,
                () -> ModSettings.requireValidAttributePattern("generic.armor("));
        assertTrue(error.getMessage().contains("generic.armor("));
    }
}
