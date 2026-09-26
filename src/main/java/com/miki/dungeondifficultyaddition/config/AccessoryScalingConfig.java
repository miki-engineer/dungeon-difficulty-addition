package com.miki.dungeondifficultyaddition.config;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.dungeon_difficulty.config.Config;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AccessoryScalingConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DungeonDifficultyAddition.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dungeon_difficulty_addition");
    private static final Path OLD_CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dd_jewelry_compat");
    private static final Path SETTINGS_PATH = CONFIG_DIR.resolve("settings.json");
    private static final Path FIXED_LEVELS_PATH = CONFIG_DIR.resolve("fixed_item_levels.json");
    private static final Path OLD_SETTINGS_PATH = OLD_CONFIG_DIR.resolve("settings.json");
    private static final Path OLD_FIXED_LEVELS_PATH = OLD_CONFIG_DIR.resolve("fixed_item_levels.json");
    private static final Path LEGACY_PATH = FMLPaths.CONFIGDIR.get().resolve("dd_jewelry_compat.json");
    private static final Path LEGACY_BACKUP_PATH = CONFIG_DIR.resolve("legacy_dd_jewelry_compat.json");
    private static final Map<String, String> SETTINGS_COMMENTS = createSettingsComments();
    private static final Map<String, String> LEVEL_COMMENTS = Map.of(
            "levels", String.join("\n",
                    "Add any level number here.",
                    "Put item IDs or regex patterns in its list.",
                    "The highest matching level wins."
            )
    );

    private static AccessoryScalingConfig instance;
    private static FixedItemLevels fixedLevels;

    public boolean enabled = true;
    public boolean minimum_equipment_level_enabled = true;
    public boolean scale_jewelry = true;
    public boolean scale_relics = true;
    public boolean merge_accessory_modifiers = true;
    public boolean show_compat_tooltip = true;
    public List<Config.AttributeModifier> attributes = List.of(defaultAttributeModifier());

    public static AccessoryScalingConfig get() {
        if (instance == null) {
            loadAll();
        }
        return instance;
    }

    public static void reload() {
        instance = null;
        fixedLevels = null;
        loadAll();
    }

    public List<Config.AttributeModifier> modifiers() {
        return attributes != null ? attributes : List.of();
    }

    public int fixedLevel(ItemStack stack) {
        return fixedLevels.levelFor(Registries.ITEM.getId(stack.getItem()).toString());
    }

    private static void loadAll() {
        try {
            Files.createDirectories(CONFIG_DIR);
            var legacyRoot = readLegacyForMigration();

            if (Files.exists(SETTINGS_PATH)) {
                instance = readSettings(SETTINGS_PATH);
            } else if (Files.exists(OLD_SETTINGS_PATH)) {
                instance = readSettings(OLD_SETTINGS_PATH);
                writeSettings(SETTINGS_PATH, instance);
            } else if (legacyRoot != null) {
                instance = GSON.fromJson(legacyRoot, AccessoryScalingConfig.class);
                if (instance == null) {
                    instance = new AccessoryScalingConfig();
                }
                writeSettings(SETTINGS_PATH, instance);
            } else {
                instance = new AccessoryScalingConfig();
                writeSettings(SETTINGS_PATH, instance);
            }

            if (Files.exists(FIXED_LEVELS_PATH)) {
                fixedLevels = readFixedLevels(FIXED_LEVELS_PATH);
                normalizeFixedLevels(FIXED_LEVELS_PATH, fixedLevels);
            } else if (Files.exists(OLD_FIXED_LEVELS_PATH)) {
                fixedLevels = readFixedLevels(OLD_FIXED_LEVELS_PATH);
                writeFixedLevels(FIXED_LEVELS_PATH, fixedLevels);
            } else {
                fixedLevels = legacyRoot != null
                        ? FixedItemLevels.fromLegacy(legacyRoot)
                        : new FixedItemLevels();
                writeFixedLevels(FIXED_LEVELS_PATH, fixedLevels);
            }

            if (legacyRoot != null && Files.exists(LEGACY_PATH)) {
                Files.move(LEGACY_PATH, LEGACY_BACKUP_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load configs from " + CONFIG_DIR, exception);
        }
    }

    private static JsonObject readLegacyForMigration() throws IOException {
        if (Files.exists(SETTINGS_PATH) || Files.notExists(LEGACY_PATH)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(LEGACY_PATH)) {
            return parseObject(reader);
        }
    }

    private static AccessoryScalingConfig readSettings(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            try {
                var settings = GSON.fromJson(parseObject(reader), AccessoryScalingConfig.class);
                return settings != null ? settings : new AccessoryScalingConfig();
            } catch (RuntimeException exception) {
                LOGGER.error("Could not parse config {}; using safe defaults. Fix the JSON and restart the game.", path, exception);
                return new AccessoryScalingConfig();
            }
        }
    }

    private static FixedItemLevels readFixedLevels(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            try {
                return FixedItemLevels.fromJson(parseObject(reader));
            } catch (RuntimeException exception) {
                LOGGER.error("Could not parse config {}; using safe defaults. Fix the JSON and restart the game.", path, exception);
                return new FixedItemLevels();
            }
        }
    }

    private static void writeFixedLevels(Path path, FixedItemLevels levels) throws IOException {
        writeJsonWithComments(path, levels.toJson(), LEVEL_COMMENTS);
    }

    private static void normalizeFixedLevels(Path path, FixedItemLevels levels) throws IOException {
        try {
            JsonObject current;
            try (Reader reader = Files.newBufferedReader(path)) {
                current = parseObject(reader);
            }
            var normalized = levels.toJson();
            if (!normalized.equals(current)) {
                writeFixedLevels(path, levels);
            }
        } catch (RuntimeException ignored) {
            // Keep malformed files intact.
        }
    }

    private static void writeSettings(Path path, AccessoryScalingConfig settings) throws IOException {
        writeJsonWithComments(
                path,
                GSON.toJsonTree(settings).getAsJsonObject(),
                SETTINGS_COMMENTS
        );
    }

    private static void writeJsonWithComments(
            Path path,
            JsonObject root,
            Map<String, String> comments
    ) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            writer.write("{\n");
            var remaining = root.size();
            for (var entry : root.entrySet()) {
                var comment = comments.get(entry.getKey());
                if (comment != null) {
                    for (var line : comment.split("\\R")) {
                        writer.write("  // " + line + "\n");
                    }
                }

                var renderedValue = GSON.toJson(entry.getValue()).replace("\n", "\n  ");
                writer.write("  " + GSON.toJson(entry.getKey()) + ": " + renderedValue);
                writer.write(--remaining > 0 ? ",\n" : "\n");
            }
            writer.write("}\n");
        }
    }

    @SuppressWarnings("deprecation")
    private static JsonObject parseObject(Reader reader) {
        var jsonReader = new JsonReader(reader);
        jsonReader.setLenient(true);
        return JsonParser.parseReader(jsonReader).getAsJsonObject();
    }

    private static Config.AttributeModifier defaultAttributeModifier() {
        var modifier = new Config.AttributeModifier(".*", 0.1F);
        modifier.operation = Config.Operation.MULTIPLY_BASE;
        modifier.randomness = 0.05F;
        return modifier;
    }

    private static Map<String, String> createSettingsComments() {
        var comments = new LinkedHashMap<String, String>();
        comments.put("enabled", "Turns the mod on or off.");
        comments.put("minimum_equipment_level_enabled", String.join("\n",
                "Enables the level 1 minimum for equipment. Requires enabled = true.",
                "Equipment with no level or level 0 receives level 1 with normal scaling.",
                "Existing levels (such as level 2 loot) stay unchanged; configured fixed levels still apply.",
                "Turning this off does not remove levels already applied."
        ));
        comments.put("scale_jewelry", "Scales Jewelry items.");
        comments.put("scale_relics", "Scales Relics items.");
        comments.put("merge_accessory_modifiers", "Prevents duplicate accessory stat lines.");
        comments.put("show_compat_tooltip", "Shows scaled values in tooltips.");
        comments.put("attributes", "Controls stat scaling per level. Supports attribute IDs and regex.");
        return comments;
    }
}
