package com.miki.dungeondifficultyaddition.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SettingsFilesTest {
    @TempDir Path temporary;
    private static JsonObject json(String text) { return JsonParser.parseString(text).getAsJsonObject(); }
    private JsonObject defaults() { return json("""
        {"scaling":{"enabled":true,"minimum_equipment_level_enabled":true},
         "encounters":{"player_readiness":{"enabled":false,"required_equipped_items":3}},
         "anvil":{"enabled":true,"gold_max_level":3,"diamond_max_level":5,"upgrade_xp_levels":1}}
        """); }
    private Path directory() throws Exception { return Files.createDirectories(temporary.resolve("dungeon_difficulty_addition")); }
    private static void validate(JsonObject root) {
        for (String section : new String[]{"scaling", "encounters", "anvil"})
            if (!root.get(section).isJsonObject()) throw new IllegalArgumentException(section);
    }
    private JsonObject load(Path directory) throws Exception { return SettingsFiles.load(directory, defaults(), SettingsFilesTest::validate); }

    @Test void freshInstallCreatesOnlyTwoActiveFiles() throws Exception {
        var directory = directory();
        assertEquals(defaults(), load(directory));
        try (var files = Files.list(directory)) { assertEquals(2, files.count()); }
        assertEquals(json("{\"levels\":{}}"), SettingsFiles.read(directory.resolve("fixed_item_levels.json")));
    }
    @Test void migrationPreservesValuesRulesAndOriginalBackups() throws Exception {
        var directory = directory();
        var settings = "{ // user's config\n\"enabled\":false,\"custom_setting\":42}";
        var fixed = "{ // unchanged rules\n\"levels\":{\"9\":[\"pack:.*\"]}}";
        Files.writeString(directory.resolve("settings.json"), settings);
        Files.writeString(directory.resolve("fixed_item_levels.json"), fixed);
        Files.writeString(directory.resolve("encounters.json"), "{\"player_readiness\":{\"enabled\":true,\"required_equipped_items\":6}}");
        Files.writeString(directory.resolve("anvil_salvage.json"), "{\"enabled\":false,\"gold_max_level\":8,\"diamond_max_level\":12}");
        Files.writeString(directory.resolve("runic_anvil.json"), "{\"gems_per_upgrade\":4}");
        var migrated = load(directory);
        assertFalse(migrated.getAsJsonObject("scaling").get("enabled").getAsBoolean());
        assertEquals(42, migrated.getAsJsonObject("scaling").get("custom_setting").getAsInt());
        assertEquals(6, migrated.getAsJsonObject("encounters").getAsJsonObject("player_readiness").get("required_equipped_items").getAsInt());
        assertEquals(8, migrated.getAsJsonObject("anvil").get("gold_max_level").getAsInt());
        assertFalse(migrated.getAsJsonObject("anvil").get("enabled").getAsBoolean());
        assertEquals(fixed, Files.readString(directory.resolve("fixed_item_levels.json")));
        Path backup;
        try (var files = Files.list(directory.resolve("backups"))) { backup = files.findFirst().orElseThrow(); }
        assertEquals(settings, Files.readString(backup.resolve("settings.json")));
        for (String name : new String[]{"encounters.json", "anvil_salvage.json", "runic_anvil.json"}) {
            assertFalse(Files.exists(directory.resolve(name)));
            assertTrue(Files.exists(backup.resolve(name)));
        }
        var saved = Files.readString(directory.resolve("settings.json"));
        assertEquals(migrated, load(directory));
        assertEquals(saved, Files.readString(directory.resolve("settings.json")));
        try (var files = Files.list(directory.resolve("backups"))) { assertEquals(1, files.count()); }
    }
    @Test void invalidLegacyJsonIsNotOverwrittenOrArchived() throws Exception {
        var directory = directory();
        Files.writeString(directory.resolve("settings.json"), "{\"enabled\":false}");
        Files.writeString(directory.resolve("anvil_salvage.json"), "broken json");
        assertThrows(RuntimeException.class, () -> load(directory));
        assertEquals("{\"enabled\":false}", Files.readString(directory.resolve("settings.json")));
        assertEquals("broken json", Files.readString(directory.resolve("anvil_salvage.json")));
        assertFalse(Files.exists(directory.resolve("backups")));
    }
    @Test void semanticValidationRunsBeforeAnyWrites() throws Exception {
        var directory = directory();
        var invalid = "{\"scaling\":null,\"encounters\":{},\"anvil\":{}}";
        Files.writeString(directory.resolve("settings.json"), invalid);
        assertThrows(IllegalArgumentException.class, () -> load(directory));
        assertEquals(invalid, Files.readString(directory.resolve("settings.json")));
        assertFalse(Files.exists(directory.resolve("fixed_item_levels.json")));
    }
    @Test void unifiedValuesTakePriorityOverStaleSplitFiles() throws Exception {
        var directory = directory();
        Files.writeString(directory.resolve("settings.json"), defaults().toString());
        Files.writeString(directory.resolve("anvil_salvage.json"), "{\"gold_max_level\":99}");
        assertEquals(3, load(directory).getAsJsonObject("anvil").get("gold_max_level").getAsInt());
        assertFalse(Files.exists(directory.resolve("anvil_salvage.json")));
    }
    @Test void oldSingleFileMigratesScalingAndFixedRules() throws Exception {
        var directory = directory();
        Files.writeString(temporary.resolve("dd_jewelry_compat.json"), "{\"enabled\":false,\"fixed_item_levels\":[{\"item\":\"pack:sword\",\"level\":7}]}");
        var migrated = load(directory);
        assertFalse(migrated.getAsJsonObject("scaling").get("enabled").getAsBoolean());
        assertEquals(7, FixedItemLevels.fromJson(SettingsFiles.read(directory.resolve("fixed_item_levels.json"))).levelFor("pack:sword"));
        assertFalse(Files.exists(temporary.resolve("dd_jewelry_compat.json")));
    }
    @Test void oldFolderMigratesWithoutChangingModernRules() throws Exception {
        var directory = directory();
        var old = Files.createDirectories(temporary.resolve("dd_jewelry_compat"));
        Files.writeString(old.resolve("settings.json"), "{\"enabled\":false}");
        Files.writeString(old.resolve("fixed_item_levels.json"), "{\"level_4\":[\"pack:shield\"]}");
        assertFalse(load(directory).getAsJsonObject("scaling").get("enabled").getAsBoolean());
        assertEquals(4, FixedItemLevels.fromJson(SettingsFiles.read(directory.resolve("fixed_item_levels.json"))).levelFor("pack:shield"));
    }
}
