package com.miki.dungeondifficultyaddition.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Fixed-level rules and legacy decoding, independent of game registries and file I/O. */
final class FixedItemLevels {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern LEGACY_LEVEL_KEY = Pattern.compile("level_(\\d+)");
    private final NavigableMap<Integer, List<String>> levels = new TreeMap<>();
    private List<CompiledRule> compiled = List.of();
    // Config-local and bounded: reloading rules never reuses old lookup results.
    private final Map<String, Integer> resolved = new LinkedHashMap<>(256, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Integer> entry) { return size() > 4096; }
    };
    private record CompiledRule(int level, String literal, Pattern regex) {
        boolean matches(String itemId) {
            return itemId.equals(literal) || (regex != null && regex.matcher(itemId).matches());
        }
    }

    synchronized int levelFor(String itemId) {
        if (compiled.isEmpty()) return 0;
        var cached = resolved.get(itemId);
        if (cached != null) return cached;
        int result = 0;
        for (var rule : compiled) {
            if (rule.matches(itemId)) {
                result = rule.level();
                break;
            }
        }
        resolved.put(itemId, result);
        return result;
    }

    private void compileRules() {
        var rules = new ArrayList<CompiledRule>();
        for (var entry : levels.descendingMap().entrySet()) for (var text : entry.getValue()) {
            if (text == null || text.isEmpty()) continue;
            Pattern regex = null;
            try { regex = Pattern.compile(text); } catch (PatternSyntaxException ignored) {}
            rules.add(new CompiledRule(entry.getKey(), text, regex));
        }
        compiled = List.copyOf(rules);
    }

    JsonObject toJson() {
        var root = new JsonObject();
        var dynamicLevels = new JsonObject();
        for (var entry : levels.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                dynamicLevels.add(Integer.toString(entry.getKey()), GSON.toJsonTree(entry.getValue()));
            }
        }
        root.add("levels", dynamicLevels);
        return root;
    }

    static FixedItemLevels fromJson(JsonObject root) {
        var result = new FixedItemLevels();

        for (var entry : root.entrySet()) {
            var matcher = LEGACY_LEVEL_KEY.matcher(entry.getKey());
            if (matcher.matches()) {
                result.putLevel(matcher.group(1), entry.getValue());
            }
        }

        if (root.has("levels") && root.get("levels").isJsonObject()) {
            for (var entry : root.getAsJsonObject("levels").entrySet()) {
                result.putLevel(entry.getKey(), entry.getValue());
            }
        }
        result.compileRules();
        return result;
    }

    private void putLevel(String levelText, JsonElement value) {
        if (!value.isJsonArray()) {
            return;
        }
        try {
            var level = Integer.parseInt(levelText);
            if (level <= 0) {
                return;
            }
            var items = new ArrayList<String>();
            for (var item : value.getAsJsonArray()) {
                if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                    items.add(item.getAsString());
                }
            }
            if (items.isEmpty()) {
                levels.remove(level);
            } else {
                levels.put(level, List.copyOf(items));
            }
        } catch (NumberFormatException ignored) {
            // Ignore invalid level keys.
        }
    }

    static FixedItemLevels fromLegacy(JsonObject root) {
        var result = new FixedItemLevels();
        if (!root.has("fixed_item_levels") || !root.get("fixed_item_levels").isJsonArray()) {
            return result;
        }

        for (var element : root.getAsJsonArray("fixed_item_levels")) {
            if (!element.isJsonObject()) {
                continue;
            }
            var rule = element.getAsJsonObject();
            if (!rule.has("item") || !rule.has("level")) {
                continue;
            }
            var level = rule.get("level").getAsInt();
            if (level >= 1) {
                var items = new ArrayList<>(result.levels.getOrDefault(level, List.of()));
                items.add(rule.get("item").getAsString());
                result.levels.put(level, List.copyOf(items));
            }
        }
        result.compileRules();
        return result;
    }

}
