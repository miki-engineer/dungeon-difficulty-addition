package com.miki.dungeondifficultyaddition.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/** Standalone smoke benchmark; not an FPS/TPS benchmark and not a timing assertion in CI. */
public final class FixedLevelLookupBenchmark {
    private record Rule(int level, String pattern) {}
    private static volatile long sink;
    public static void main(String[] args) {
        var root = new JsonObject();
        var levels = new JsonObject();
        var reference = new ArrayList<Rule>();
        for (int level = 4; level >= 1; level--) {
            var patterns = new JsonArray();
            for (int n = 0; n < 8; n++) {
                String pattern = "pack" + n + ":.*_tier" + level;
                patterns.add(pattern); reference.add(new Rule(level, pattern));
            }
            levels.add(Integer.toString(level), patterns);
        }
        root.add("levels", levels);
        var optimized = FixedItemLevels.fromJson(root);
        String[] ids = new String[64];
        for (int i = 0; i < ids.length; i++) ids[i] = "pack" + (i % 12) + ":equipment_tier" + (i % 5);
        for (String id : ids) if (oldLookup(reference, id) != optimized.levelFor(id))
            throw new AssertionError("Mismatch: " + id);
        measure(reference, optimized, ids, 10000, false);
        measure(reference, optimized, ids, 100000, true);
    }
    private static int oldLookup(List<Rule> rules, String id) {
        for (var rule : rules) if (id.equals(rule.pattern()) || id.matches(rule.pattern())) return rule.level();
        return 0;
    }
    private static void measure(List<Rule> rules, FixedItemLevels optimized, String[] ids, int count, boolean report) {
        long start = System.nanoTime(), oldSum = 0;
        for (int i = 0; i < count; i++) oldSum += oldLookup(rules, ids[i % ids.length]);
        long oldNanos = System.nanoTime() - start;
        start = System.nanoTime();
        long newSum = 0;
        for (int i = 0; i < count; i++) newSum += optimized.levelFor(ids[i % ids.length]);
        long newNanos = System.nanoTime() - start;
        if (oldSum != newSum) throw new AssertionError("Different results");
        sink = oldSum + newSum;
        if (report) System.out.printf("Repeated fixed-level lookups: %,d; rules: %d; distinct IDs: %d%nBefore: %.2f ms; after: %.2f ms; equal checksum: %d%n",
                count, rules.size(), ids.length, oldNanos / 1e6, newNanos / 1e6, oldSum);
    }
}
