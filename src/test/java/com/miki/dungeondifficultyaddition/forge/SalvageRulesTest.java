package com.miki.dungeondifficultyaddition.forge;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SalvageRulesTest {
    @Test void defaultHammerCapsAreThreeAndFive() {
        var config = new SalvageConfig();
        assertEquals(3, config.gold_max_level);
        assertEquals(5, config.diamond_max_level);
        assertTrue(HammerTier.GOLD.accepts(3, config.gold_max_level, config.diamond_max_level));
        assertFalse(HammerTier.GOLD.accepts(4, config.gold_max_level, config.diamond_max_level));
        assertTrue(HammerTier.DIAMOND.accepts(5, config.gold_max_level, config.diamond_max_level));
        assertFalse(HammerTier.DIAMOND.accepts(6, config.gold_max_level, config.diamond_max_level));
        assertTrue(HammerTier.NETHERITE.accepts(100000, config.gold_max_level, config.diamond_max_level));
    }
    @Test void exactlyThreeSeparatedStrikesCompleteSalvage() {
        var progress = new SalvageProgress(0, 0).strike(10);
        assertEquals(1, progress.hits()); assertFalse(progress.complete());
        assertSame(progress, progress.strike(11));
        progress = progress.strike(16);
        assertEquals(2, progress.hits()); assertFalse(progress.complete());
        progress = progress.strike(22);
        assertTrue(progress.complete());
        assertSame(progress, progress.strike(100));
    }
    @Test void restoredProgressContinuesWithoutExtraHits() {
        var progress = new SalvageProgress(2, 50);
        assertSame(progress, progress.strike(49));
        assertTrue(progress.strike(50).complete());
        assertThrows(IllegalArgumentException.class, () -> new SalvageProgress(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SalvageProgress(4, 0));
    }
    @Test void hammerCapsAreInclusiveAndNetheriteHasNoTierCap() {
        assertTrue(HammerTier.GOLD.accepts(10, 10, 30));
        assertFalse(HammerTier.GOLD.accepts(11, 10, 30));
        assertTrue(HammerTier.DIAMOND.accepts(30, 10, 30));
        assertFalse(HammerTier.DIAMOND.accepts(31, 10, 30));
        assertTrue(HammerTier.NETHERITE.accepts(100000, 10, 30));
        for (var tier : HammerTier.values()) assertFalse(tier.accepts(0, 10, 30));
    }
    @Test void categoriesRoundTripAndInvalidCategoryIsRejected() {
        for (var kind : GemKind.values()) assertEquals(kind, GemKind.read(kind.id()));
        assertNull(GemKind.read("invalid"));
    }
    @Test void configRejectsReversedTiers() {
        var config = new SalvageConfig(); config.validate();
        config.diamond_max_level = 1;
        assertThrows(IllegalArgumentException.class, config::validate);
        config.diamond_max_level = 30; config.netherite_durability = 1;
        assertThrows(IllegalArgumentException.class, config::validate);
    }
}
