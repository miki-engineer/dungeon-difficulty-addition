package com.miki.dungeondifficultyaddition.compat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegendaryAttributePolicyTest {
    @Test void eventAttributesWinWithoutQueryingLegacyDefaults() {
        var effective = java.util.List.of(new Modifier("damage", "base_attack_damage", "mainhand", 7));
        assertSame(effective, LegendaryAttributePolicy.effectiveOrFallback(effective, java.util.List::isEmpty,
                () -> { throw new AssertionError("Legacy defaults must not replace event attributes"); }));
    }

    @Test void emptyEventAttributesUseLegacyDefaultsExactlyOnce() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var legacy = java.util.List.of(new Modifier("damage", "base_attack_damage", "mainhand", 12));
        assertSame(legacy, LegendaryAttributePolicy.effectiveOrFallback(java.util.List.<Modifier>of(),
                java.util.List::isEmpty, () -> { calls.incrementAndGet(); return legacy; }));
        assertEquals(1, calls.get());
        assertTrue(LegendaryAttributePolicy.effectiveOrFallback(java.util.List.<Modifier>of(),
                java.util.List::isEmpty, java.util.List::of).isEmpty());
    }

    @Test void repeatedResolutionUsesCurrentEventValuesWithoutAccumulatingOldStats() {
        var first = java.util.List.of(new Modifier("damage", "base_attack_damage", "mainhand", 7));
        var changed = java.util.List.of(new Modifier("damage", "base_attack_damage", "mainhand", 14));
        assertSame(first, LegendaryAttributePolicy.effectiveOrFallback(first, java.util.List::isEmpty, () -> changed));
        assertSame(changed, LegendaryAttributePolicy.effectiveOrFallback(changed, java.util.List::isEmpty, () -> first));
    }

    private record Modifier(String attribute, String id, String slot, double amount) {
        java.util.List<String> key() { return java.util.List.of(attribute, id, slot); }
    }

    @Test void duplicateSpeedIsNotAddedTwiceAndLatestValueWins() {
        var speed = new Modifier("speed", "base_attack_speed", "mainhand", -2.8);
        var damage = new Modifier("damage", "base_attack_damage", "mainhand", 22.5);
        var entries = java.util.List.of(speed, damage, speed);
        var result = LegendaryAttributePolicy.uniqueByKey(entries, Modifier::key);
        assertEquals(java.util.List.of(speed, damage), result);
        assertEquals(1.2, 4 + result.getFirst().amount(), 0.00001);
        assertEquals(result, LegendaryAttributePolicy.uniqueByKey(result, Modifier::key));
        var changedSpeed = new Modifier("speed", "base_attack_speed", "mainhand", -3);
        assertEquals(java.util.List.of(changedSpeed),
                LegendaryAttributePolicy.uniqueByKey(java.util.List.of(speed, changedSpeed), Modifier::key));
    }

    @Test void differentAttributesIdsAndSlotsRemainDistinct() {
        var entries = java.util.List.of(
                new Modifier("speed", "base_attack_speed", "mainhand", -2.8),
                new Modifier("speed", "base_attack_speed", "offhand", -2.8),
                new Modifier("speed", "other_bonus", "mainhand", 0.1),
                new Modifier("reach", "base_attack_speed", "mainhand", 1));
        assertEquals(entries, LegendaryAttributePolicy.uniqueByKey(entries, Modifier::key));
    }

    @Test void repairsOnlyTheKnownMisplacedSpeedModifier() {
        assertTrue(LegendaryAttributePolicy.misplacedSpeed("minecraft:generic.attack_damage", "minecraft:base_attack_speed"));
        assertFalse(LegendaryAttributePolicy.misplacedSpeed("minecraft:generic.attack_damage", "minecraft:base_attack_damage"));
        assertFalse(LegendaryAttributePolicy.misplacedSpeed("minecraft:generic.attack_damage", "example:penalty"));
        assertFalse(LegendaryAttributePolicy.misplacedSpeed("minecraft:player.entity_interaction_range", "minecraft:base_attack_speed"));
    }

    @Test void correctlyAssignedSpeedDoesNotNeedRepeatedCorrection() {
        assertFalse(LegendaryAttributePolicy.misplacedSpeed("minecraft:generic.attack_speed", "minecraft:base_attack_speed"));
        assertFalse(LegendaryAttributePolicy.misplacedSpeed("", ""));
    }
}
