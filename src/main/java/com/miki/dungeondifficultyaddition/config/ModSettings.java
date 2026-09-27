package com.miki.dungeondifficultyaddition.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.miki.dungeondifficultyaddition.forge.SalvageConfig;
import com.miki.dungeondifficultyaddition.readiness.EncounterConfig;
import net.neoforged.fml.loading.FMLPaths;

/** One published snapshot prevents readers from seeing partially loaded config sections. */
public final class ModSettings {
    private static final Gson GSON = new Gson();
    private static volatile Snapshot current;
    public record Snapshot(AccessoryScalingConfig scaling, EncounterConfig encounters,
                           SalvageConfig anvil, FixedItemLevels fixedLevels) {}
    private ModSettings() {}
    public static Snapshot get() {
        var value = current;
        if (value == null) synchronized (ModSettings.class) {
            value = current;
            if (value == null) current = value = load();
        }
        return value;
    }
    public static synchronized void reload() {
        // Preserve the previous working snapshot if loading/validation fails.
        current = load();
    }
    private static Snapshot load() {
        var directory = FMLPaths.CONFIGDIR.get().resolve("dungeon_difficulty_addition");
        var defaults = new JsonObject();
        defaults.add("scaling", GSON.toJsonTree(new AccessoryScalingConfig()));
        defaults.add("encounters", GSON.toJsonTree(new EncounterConfig()));
        defaults.add("anvil", GSON.toJsonTree(new SalvageConfig()));
        try {
            var root = SettingsFiles.load(directory, defaults, ModSettings::validate);
            return new Snapshot(section(root, "scaling", AccessoryScalingConfig.class),
                    section(root, "encounters", EncounterConfig.class), section(root, "anvil", SalvageConfig.class),
                    FixedItemLevels.fromJson(SettingsFiles.read(directory.resolve("fixed_item_levels.json"))));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load DDA settings from " + directory
                    + ". Existing files are preserved; fix the reported config error before restarting.", exception);
        }
    }
    private static void validate(JsonObject root) {
        var scaling = section(root, "scaling", AccessoryScalingConfig.class);
        if (scaling.attributes != null && scaling.attributes.stream().anyMatch(value -> value == null || value.operation == null))
            throw new IllegalArgumentException("scaling.attributes contains a null modifier or operation");
        section(root, "encounters", EncounterConfig.class).validate();
        section(root, "anvil", SalvageConfig.class).validate();
    }
    private static <T> T section(JsonObject root, String name, Class<T> type) {
        if (!root.has(name) || !root.get(name).isJsonObject())
            throw new IllegalArgumentException("settings.json: " + name + " must be an object");
        return GSON.fromJson(root.get(name), type);
    }
}
