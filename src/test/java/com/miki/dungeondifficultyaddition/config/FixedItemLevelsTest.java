package com.miki.dungeondifficultyaddition.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedItemLevelsTest {
    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    @Test void highestMatchingLevelWins() {
        var levels = FixedItemLevels.fromJson(json("""
                {"levels":{"1":["minecraft:.*"],"5":["minecraft:diamond_sword"],"3":[".*sword"]}}
                """));
        assertEquals(5, levels.levelFor("minecraft:diamond_sword"));
        assertEquals(3, levels.levelFor("minecraft:iron_sword"));
        assertEquals(1, levels.levelFor("minecraft:shield"));
        assertEquals(0, levels.levelFor("other:shield"));
    }

    @Test void invalidRegexDoesNotHideValidRules() {
        var levels = FixedItemLevels.fromJson(json("""
                {"levels":{"9":["["],"2":["minecraft:shield"]}}
                """));
        assertEquals(2, levels.levelFor("minecraft:shield"));
        assertEquals(0, levels.levelFor("minecraft:stone"));
    }

    @Test void invalidLevelsAndNonStringPatternsAreIgnored() {
        var levels = FixedItemLevels.fromJson(json("""
                {"levels":{"0":[".*"],"-2":[".*"],"bad":[".*"],
                "999999999999999":[".*"],"8":true,"4":[false,12,null,{},"minecraft:shield"]}}
                """));
        assertEquals(4, levels.levelFor("minecraft:shield"));
        assertEquals(0, levels.levelFor("minecraft:stone"));
        assertEquals(json("""
                {"levels":{"4":["minecraft:shield"]}}
                """), levels.toJson());
    }

    @Test void modernEntriesOverrideSameLevelLegacyEntries() {
        var levels = FixedItemLevels.fromJson(json("""
                {"level_3":["minecraft:shield"],"level_5":["minecraft:bow"],
                "levels":{"3":["minecraft:diamond_sword"],"5":[]}}
                """));
        assertEquals(3, levels.levelFor("minecraft:diamond_sword"));
        assertEquals(0, levels.levelFor("minecraft:shield"));
        assertEquals(0, levels.levelFor("minecraft:bow"));
    }

    @Test void legacyListMigrationGroupsRulesByLevel() {
        var levels = FixedItemLevels.fromLegacy(json("""
                {"fixed_item_levels":[{"item":"minecraft:shield","level":2},
                {"item":"minecraft:bow","level":2},{"item":".*","level":0},
                {},false]}
                """));
        assertEquals(json("""
                {"levels":{"2":["minecraft:shield","minecraft:bow"]}}
                """), levels.toJson());
        assertEquals(2, levels.levelFor("minecraft:bow"));
    }

    @Test void normalizedRulesRoundTripWithoutChangingMatches() {
        var original = FixedItemLevels.fromJson(json("""
                {"level_7":["mod:.*"],"levels":{"2":["minecraft:shield"]}}
                """));
        var restored = FixedItemLevels.fromJson(original.toJson());
        assertEquals(original.toJson(), restored.toJson());
        for (var id : new String[]{"mod:wand", "minecraft:shield", "minecraft:stone"}) {
            assertEquals(original.levelFor(id), restored.levelFor(id));
        }
    }

    @Test void missingRulesMeanNoFixedOverride() {
        assertEquals(0, FixedItemLevels.fromJson(json("{}")).levelFor("minecraft:shield"));
        assertEquals(0, FixedItemLevels.fromLegacy(json("{}")).levelFor("minecraft:shield"));
    }
}
