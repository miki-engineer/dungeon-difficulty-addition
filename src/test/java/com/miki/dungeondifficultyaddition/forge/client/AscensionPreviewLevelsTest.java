package com.miki.dungeondifficultyaddition.forge.client;

import net.dungeon_difficulty.config.Config;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AscensionPreviewLevelsTest {
    @Test void includesDimensionsZonesAndEntityRewards() {
        var config = new Config();
        var dimension = new Config.Dimension();
        dimension.difficulty = new Config.DifficultyReference("adventure", 3);
        var zone = new Config.Zone();
        zone.difficulty = new Config.DifficultyReference("dungeon", 6);
        var entity = new Config.EntityMatcher();
        entity.difficulty = new Config.DifficultyReference("heroic", 5);
        entity.difficulty.reward_level = 8;
        entity.difficulty.entity_level = 50;
        dimension.zones = List.of(zone);
        dimension.entities = List.of(entity);
        config.dimensions = new Config.Dimension[] {dimension};
        assertEquals(8, AscensionPreviewLevels.maximum(config));
        entity.difficulty.reward_level = 2;
        assertEquals(6, AscensionPreviewLevels.maximum(config));
        dimension.zones = List.of();
        assertEquals(3, AscensionPreviewLevels.maximum(config));
    }

    @Test void handlesEmptyConfigurationAndSupportedLevelLimit() {
        assertEquals(1, AscensionPreviewLevels.maximum(null));
        var config = new Config();
        config.dimensions = null;
        assertEquals(1, AscensionPreviewLevels.maximum(config));
        var dimension = new Config.Dimension();
        dimension.difficulty = new Config.DifficultyReference("dungeon", Integer.MAX_VALUE);
        dimension.zones = null;
        dimension.entities = null;
        config.dimensions = new Config.Dimension[] {null, dimension};
        assertEquals(100_000, AscensionPreviewLevels.maximum(config));
    }
}
