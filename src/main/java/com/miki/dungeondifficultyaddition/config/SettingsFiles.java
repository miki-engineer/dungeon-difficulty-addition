package com.miki.dungeondifficultyaddition.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Filesystem-only migration, independently testable without Minecraft registries. */
final class SettingsFiles {
    private SettingsFiles() {}
    static JsonObject load(Path directory, JsonObject defaults, Consumer<JsonObject> validate) throws IOException {
        Files.createDirectories(directory);
        Path settings = directory.resolve("settings.json");
        Path fixed = directory.resolve("fixed_item_levels.json");
        Path oldDirectory = directory.getParent().resolve("dd_jewelry_compat");
        Path legacy = directory.getParent().resolve("dd_jewelry_compat.json");
        Path source = Files.exists(settings) ? settings
                : Files.exists(oldDirectory.resolve("settings.json")) ? oldDirectory.resolve("settings.json")
                : Files.exists(legacy) ? legacy : null;
        JsonObject original = source == null ? new JsonObject() : read(source);
        boolean sectioned = original.has("scaling") || original.has("encounters") || original.has("anvil");
        JsonObject merged = sectioned ? original.deepCopy() : new JsonObject();
        if (!sectioned && source != null) {
            var scaling = original.deepCopy();
            scaling.remove("fixed_item_levels");
            merged.add("scaling", scaling);
        }
        Path encounters = directory.resolve("encounters.json");
        Path anvil = directory.resolve("anvil_salvage.json");
        Path prototype = directory.resolve("runic_anvil.json");
        if (!merged.has("encounters") && Files.exists(encounters)) merged.add("encounters", read(encounters));
        if (!merged.has("anvil") && Files.exists(anvil)) merged.add("anvil", read(anvil));
        fillMissing(merged, defaults);
        // A malformed or invalid section aborts before backups, rewrites or archiving.
        validate.accept(merged);

        JsonObject fixedToWrite = null;
        if (!Files.exists(fixed)) {
            if (Files.exists(oldDirectory.resolve("fixed_item_levels.json")))
                fixedToWrite = FixedItemLevels.fromJson(read(oldDirectory.resolve("fixed_item_levels.json"))).toJson();
            else if (Files.exists(legacy)) fixedToWrite = FixedItemLevels.fromLegacy(read(legacy)).toJson();
            else fixedToWrite = new FixedItemLevels().toJson();
        } else {
            read(fixed); // Do not complete migration while the retained rule file is malformed.
        }
        boolean rewrite = !Files.exists(settings) || !sectioned || !merged.equals(original);
        Map<Path, String> retired = new LinkedHashMap<>();
        retired.put(encounters, "encounters.json");
        retired.put(anvil, "anvil_salvage.json");
        retired.put(prototype, "runic_anvil.json");
        retired.put(legacy, "dd_jewelry_compat.json");
        retired.put(oldDirectory.resolve("settings.json"), "old-settings.json");
        retired.put(oldDirectory.resolve("fixed_item_levels.json"), "old-fixed_item_levels.json");
        boolean archive = retired.keySet().stream().anyMatch(Files::exists);
        if (rewrite || fixedToWrite != null || archive) {
            Path backup = null;
            if (Files.exists(settings) || archive) {
                Files.createDirectories(directory.resolve("backups"));
                backup = Files.createTempDirectory(directory.resolve("backups"), "config-migration-");
                if (Files.exists(settings)) Files.copy(settings, backup.resolve("settings.json"));
                for (var entry : retired.entrySet()) if (Files.exists(entry.getKey()))
                    Files.copy(entry.getKey(), backup.resolve(entry.getValue()));
            }
            // Publish the retained rules first: interrupted migrations never lose legacy fixed rules.
            if (fixedToWrite != null) writeAtomic(fixed, fixedToWrite, false);
            if (rewrite) writeAtomic(settings, merged, true);
            // Both new files are durable before retiring old sources. Copies already exist on failure.
            if (backup != null) for (var entry : retired.entrySet()) if (Files.exists(entry.getKey()))
                Files.move(entry.getKey(), backup.resolve(entry.getValue()), StandardCopyOption.REPLACE_EXISTING);
        }
        return merged;
    }

    private static void fillMissing(JsonObject target, JsonObject defaults) {
        for (var entry : defaults.entrySet()) {
            if (!target.has(entry.getKey())) target.add(entry.getKey(), entry.getValue().deepCopy());
            else if (target.get(entry.getKey()).isJsonObject() && entry.getValue().isJsonObject())
                fillMissing(target.getAsJsonObject(entry.getKey()), entry.getValue().getAsJsonObject());
        }
    }

    @SuppressWarnings("deprecation")
    static JsonObject read(Path path) throws IOException {
        try (var reader = new JsonReader(Files.newBufferedReader(path))) {
            reader.setLenient(true);
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void writeAtomic(Path path, JsonObject root, boolean comments) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), ".dda-config-", ".tmp");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            String json = gson.toJson(root) + System.lineSeparator();
            if (comments) json = json
                    .replace("  \"scaling\": {", "  // Equipment scaling and tooltips. Existing item levels are preserved by the level-1 minimum.\n  \"scaling\": {")
                    .replace("  \"encounters\": {", "  // Dungeon/heroic combat readiness and damage penalties.\n  \"encounters\": {")
                    .replace("  \"anvil\": {", "  // Salvaging, hammer caps/durability, and ascension XP cost. Netherite has no level cap.\n  \"anvil\": {");
            Files.writeString(temporary, json);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
