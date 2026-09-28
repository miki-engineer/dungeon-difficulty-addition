package com.miki.dungeondifficultyaddition.forge;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ForgeAssetsTest {
    @Test void hammersUseOneLeftBlockAndTwoMatchingMaterials() throws Exception {
        for (var tier : new String[]{"gold", "diamond", "netherite"}) {
            var path = "data/dungeon_difficulty_addition/recipe/" + tier + "_salvage_hammer.json";
            try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(stream);
                var recipe = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                var pattern = recipe.getAsJsonArray("pattern");
                assertEquals(3, pattern.size());
                assertEquals("BII", pattern.get(0).getAsString());
                assertEquals(" S ", pattern.get(1).getAsString());
                assertEquals(" S ", pattern.get(2).getAsString());
                var key = recipe.getAsJsonObject("key");
                assertEquals("minecraft:" + tier + "_block", key.getAsJsonObject("B").get("item").getAsString());
                var material = tier.equals("diamond") ? "diamond" : tier + "_ingot";
                assertEquals("minecraft:" + material, key.getAsJsonObject("I").get("item").getAsString());
                assertEquals("minecraft:stick", key.getAsJsonObject("S").get("item").getAsString());
                assertEquals("dungeon_difficulty_addition:" + tier + "_salvage_hammer", recipe.getAsJsonObject("result").get("id").getAsString());
                assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
            }
        }
    }
    private static final String ASSETS = "assets/dungeon_difficulty_addition/";
    @Test void suppliedGemSpritesAre16Pixels() throws Exception {
        for (var kind : GemKind.values()) try (var stream = getClass().getClassLoader().getResourceAsStream(
                ASSETS + "textures/item/" + kind.id() + "_level_gem.png")) {
            assertNotNull(stream);
            var image = ImageIO.read(stream);
            assertEquals(16, image.getWidth()); assertEquals(16, image.getHeight());
        }
    }
    @Test void allModelsReferenceExistingTextures() throws Exception {
        for (var name : new String[]{"weapon_level_gem", "armor_level_gem", "accessory_level_gem",
                "weapon_level_fragment", "armor_level_fragment", "accessory_level_fragment",
                "gold_salvage_hammer", "diamond_salvage_hammer", "netherite_salvage_hammer"}) {
            try (var stream = getClass().getClassLoader().getResourceAsStream(ASSETS + "models/item/" + name + ".json")) {
                assertNotNull(stream);
                var model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var entry : model.getAsJsonObject("textures").entrySet()) {
                    var path = entry.getValue().getAsString().split(":", 2);
                    assertNotNull(getClass().getClassLoader().getResource("assets/" + path[0] + "/textures/" + path[1] + ".png"));
                }
            }
        }
    }
}
