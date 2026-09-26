package com.miki.dungeondifficultyaddition.mixin;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class MixinConfigurationTest {
    @Test void everyConfiguredMixinAndPluginHasACompiledClass() throws Exception {
        var loader = getClass().getClassLoader();
        try (var stream = loader.getResourceAsStream("dungeon_difficulty_addition.mixins.json")) {
            assertNotNull(stream);
            var config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            var base = config.get("package").getAsString();
            var names = new HashSet<String>();
            for (var side : new String[]{"mixins", "client"}) {
                for (var entry : config.getAsJsonArray(side)) {
                    var name = base + "." + entry.getAsString();
                    assertTrue(names.add(name), "Duplicate mixin: " + name);
                    assertNotNull(loader.getResource(name.replace('.', '/') + ".class"), name);
                }
            }
            assertEquals(20, names.size(), "Keep all existing hooks during reorganization");
            var plugin = config.get("plugin").getAsString();
            assertNotNull(loader.getResource(plugin.replace('.', '/') + ".class"), plugin);
        }
    }
}
