package com.miki.dungeondifficultyaddition.gametest;

import com.miki.dungeondifficultyaddition.DungeonDifficultyAddition;
import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import com.miki.dungeondifficultyaddition.scaling.LevelBonus;
import com.miki.dungeondifficultyaddition.mixin.loot.ItemScalingInvoker;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.EntityScaling;
import net.dungeon_difficulty.logic.ExperienceScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Level 1 is base stats everywhere: items, mob stats, mob XP and spawners. Each test drives Dungeon
 * Difficulty's real scaling code with a randomness-free modifier, so the numbers are exact.
 * Run with: gradlew runGameTestServer (development only; excluded from the published jar).
 */
@GameTestHolder(DungeonDifficultyAddition.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LevelBonusGameTests {
    private static final BlockPos MOB = new BlockPos(1, 2, 1);
    private static final double EPSILON = 1e-6;
    private LevelBonusGameTests() {}

    private static Config.AttributeModifier modifier(String attribute, Config.Operation operation, float value) {
        var modifier = new Config.AttributeModifier(attribute, value);
        modifier.operation = operation;
        modifier.randomness = 0;
        modifier.offset = 0;
        return modifier;
    }

    /** Dungeon Difficulty's private per-mob step, the one our redirect hooks. */
    private static void scaleMob(LivingEntity mob, int level, Config.AttributeModifier... modifiers) throws Exception {
        scaleMob(mob, "gametest", level, modifiers);
    }

    private static void scaleMob(LivingEntity mob, String source, int level, Config.AttributeModifier... modifiers) throws Exception {
        Method apply = EntityScaling.class.getDeclaredMethod("apply", PatternMatching.EntityScaleResult.class, LivingEntity.class);
        apply.setAccessible(true);
        apply.invoke(null, new PatternMatching.EntityScaleResult(source, List.of(modifiers), level, 0), mob);
    }

    private static double value(LivingEntity mob, RegistryEntry<EntityAttribute> attribute) {
        return mob.getAttributeValue(attribute);
    }

    @GameTest(templateName = "test_empty")
    public static void mobStatsStartAtLevelTwo(TestContext context) throws Exception {
        var health = modifier("minecraft:generic.max_health", Config.Operation.MULTIPLY_BASE, 0.25F);
        var armor = modifier("minecraft:generic.armor", Config.Operation.ADDITION, 1F);

        var levelOne = context.spawnEntity(EntityType.ZOMBIE, MOB);
        double baseHealth = value(levelOne, EntityAttributes.GENERIC_MAX_HEALTH);
        double baseArmor = value(levelOne, EntityAttributes.GENERIC_ARMOR);
        scaleMob(levelOne, 1, health, armor);
        context.assertTrue(Math.abs(value(levelOne, EntityAttributes.GENERIC_MAX_HEALTH) - baseHealth) < EPSILON,
                "Level 1 mob keeps vanilla health " + baseHealth + ", got " + value(levelOne, EntityAttributes.GENERIC_MAX_HEALTH));
        context.assertTrue(Math.abs(value(levelOne, EntityAttributes.GENERIC_ARMOR) - baseArmor) < EPSILON,
                "Level 1 mob keeps vanilla armor " + baseArmor + ", got " + value(levelOne, EntityAttributes.GENERIC_ARMOR));

        var levelThree = context.spawnEntity(EntityType.ZOMBIE, MOB);
        scaleMob(levelThree, 3, health, armor);
        // Two bonus levels: +50% health, +2 armor.
        context.assertTrue(Math.abs(value(levelThree, EntityAttributes.GENERIC_MAX_HEALTH) - baseHealth * 1.5) < EPSILON,
                "Level 3 mob has two levels of health bonus, got " + value(levelThree, EntityAttributes.GENERIC_MAX_HEALTH));
        context.assertTrue(Math.abs(value(levelThree, EntityAttributes.GENERIC_ARMOR) - (baseArmor + 2)) < EPSILON,
                "Level 3 mob has two levels of armor bonus, got " + value(levelThree, EntityAttributes.GENERIC_ARMOR));
        context.complete();
    }

    /**
     * Per-player difficulty already passes min(players, cap) - 1, so it must not be shifted again:
     * two players (level 1) give one bonus step, as in Dungeon Difficulty without this mod.
     */
    @GameTest(templateName = "test_empty")
    public static void perPlayerDifficultyKeepsItsOwnOffset(TestContext context) throws Exception {
        var health = modifier("minecraft:generic.max_health", Config.Operation.MULTIPLY_BASE, 0.25F);

        var twoPlayers = context.spawnEntity(EntityType.ZOMBIE, MOB);
        double baseHealth = value(twoPlayers, EntityAttributes.GENERIC_MAX_HEALTH);
        scaleMob(twoPlayers, "per_player", 1, health);
        context.assertTrue(Math.abs(value(twoPlayers, EntityAttributes.GENERIC_MAX_HEALTH) - baseHealth * 1.25) < EPSILON,
                "Two players add one step (+25% health), got " + value(twoPlayers, EntityAttributes.GENERIC_MAX_HEALTH));

        var fivePlayers = context.spawnEntity(EntityType.ZOMBIE, MOB);
        scaleMob(fivePlayers, "per_player", 4, health);
        context.assertTrue(Math.abs(value(fivePlayers, EntityAttributes.GENERIC_MAX_HEALTH) - baseHealth * 2) < EPSILON,
                "Five players add four steps (+100% health), got " + value(fivePlayers, EntityAttributes.GENERIC_MAX_HEALTH));
        context.complete();
    }

    @GameTest(templateName = "test_empty")
    public static void spawnersStartAtLevelTwo(TestContext context) {
        context.assertTrue(new PatternMatching.SpawnerScaleResult(List.of(), 1).level() == 0,
                "A level 1 spawner has no bonus levels");
        context.assertTrue(new PatternMatching.SpawnerScaleResult(List.of(), 3).level() == 2,
                "A level 3 spawner has two bonus levels");
        context.complete();
    }

    @GameTest(templateName = "test_empty")
    public static void experienceHookLoads(TestContext context) {
        // Loading ExperienceScaling applies our redirect; a mismatched target would fail here. With no
        // dungeon zone at the test arena, the XP must come back unchanged.
        var mob = context.spawnEntity(EntityType.ZOMBIE, MOB);
        context.assertTrue(ExperienceScaling.scale(context.getWorld(), mob, 10) == 10, "XP outside zones is vanilla");
        context.complete();
    }

    private static double attackDamage(ItemStack stack) {
        double total = 0;
        for (var entry : stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT).modifiers()) {
            if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                    && entry.modifier().operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                total += entry.modifier().value();
            }
        }
        return total;
    }

    private static ItemStack rolledSword(int level) {
        var stack = new ItemStack(Items.IRON_SWORD);
        ItemScalingInvoker.dungeonDifficultyAddition$applyModifiersForItemStack(List.of(AttributeModifierSlot.MAINHAND),
                "minecraft:iron_sword", stack,
                List.of(modifier("minecraft:generic.attack_damage", Config.Operation.ADDITION, 1F)), level);
        return stack;
    }

    @GameTest(templateName = "test_empty")
    public static void itemsStartAtLevelTwo(TestContext context) {
        double base = attackDamage(new ItemStack(Items.IRON_SWORD));
        double levelOne = attackDamage(rolledSword(1));
        double levelThree = attackDamage(rolledSword(3));
        context.assertTrue(Math.abs(levelOne - base) < EPSILON, "Level 1 sword keeps base damage " + base + ", got " + levelOne);
        context.assertTrue(Math.abs(levelThree - (base + 2)) < EPSILON, "Level 3 sword has +2 damage, got " + levelThree);
        context.complete();
    }

    /**
     * The level-1 rule follows scaling.minimum_equipment_level_enabled. Switch it off in this server's real
     * settings file, reload, and check that equipment, mobs and spawners all use Dungeon Difficulty's
     * original math again; the file is restored and reloaded even if an assertion fails.
     */
    @GameTest(templateName = "test_empty")
    public static void switchOffRestoresOriginalMath(TestContext context) throws Exception {
        var settings = FMLPaths.CONFIGDIR.get().resolve("dungeon_difficulty_addition").resolve("settings.json");
        var original = Files.readString(settings);
        var switchedOff = original.replace("\"minimum_equipment_level_enabled\": true", "\"minimum_equipment_level_enabled\": false");
        context.assertTrue(!switchedOff.equals(original), "Test config has minimum_equipment_level_enabled: true");
        try {
            Files.writeString(settings, switchedOff);
            AccessoryScalingConfig.reload();
            context.assertTrue(!LevelBonus.levelOneIsBase(), "Switch off disables the level-1 rule");

            double base = attackDamage(new ItemStack(Items.IRON_SWORD));
            context.assertTrue(Math.abs(attackDamage(rolledSword(1)) - (base + 1)) < EPSILON,
                    "Original math: level 1 sword has +1 damage, got " + attackDamage(rolledSword(1)));
            context.assertTrue(Math.abs(attackDamage(rolledSword(3)) - (base + 3)) < EPSILON,
                    "Original math: level 3 sword has +3 damage, got " + attackDamage(rolledSword(3)));

            var mob = context.spawnEntity(EntityType.ZOMBIE, MOB);
            double baseHealth = value(mob, EntityAttributes.GENERIC_MAX_HEALTH);
            scaleMob(mob, 1, modifier("minecraft:generic.max_health", Config.Operation.MULTIPLY_BASE, 0.25F));
            context.assertTrue(Math.abs(value(mob, EntityAttributes.GENERIC_MAX_HEALTH) - baseHealth * 1.25) < EPSILON,
                    "Original math: level 1 mob has +25% health, got " + value(mob, EntityAttributes.GENERIC_MAX_HEALTH));

            context.assertTrue(new PatternMatching.SpawnerScaleResult(List.of(), 1).level() == 1,
                    "Original math: a level 1 spawner has one bonus level");
        } finally {
            restore(settings, original);
        }
        context.assertTrue(LevelBonus.levelOneIsBase(), "Restored config turns the level-1 rule back on");
        context.complete();
    }

    private static void restore(Path settings, String original) throws IOException {
        Files.writeString(settings, original);
        AccessoryScalingConfig.reload();
    }
}
