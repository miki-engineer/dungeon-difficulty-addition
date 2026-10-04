package com.miki.dungeondifficultyaddition.compat.legendary;

import com.miki.dungeondifficultyaddition.compat.LegendaryMonsterAttributes;
import com.miki.dungeondifficultyaddition.config.AccessoryScalingConfig;
import net.dungeon_difficulty.DungeonDifficulty;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.ItemScaling;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class LegendaryAbilityDamage {
    private static final String DATA = "dungeon_difficulty_addition.legendary_ability";
    private static final String PROJECTILES = "net.miauczel.legendary_monsters.entity.AnimatedMonster.Projectile.";
    private static final ClassValue<Boolean> EFFECT_TYPES = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) { return type.getName().startsWith(PROJECTILES); }
    };
    private LegendaryAbilityDamage() {}

    public static AbilityDamageScope.Cast cast(LivingEntity actor, ItemStack stack) {
        if (!(actor instanceof PlayerEntity) || actor.getWorld().isClient
                || !AccessoryScalingConfig.get().enabled || !LegendaryMonsterAttributes.applies(stack)) return null;
        int level = ItemScaling.getScaleFactor(stack);
        if (level <= 0) return null;
        var data = new PatternMatching.ItemData(PatternMatching.ItemKind.WEAPONS,
                Identifier.ofVanilla("none"), stack.getRegistryEntry(), stack.getRarity().toString());
        var result = PatternMatching.getItemScaleResult(data, DungeonDifficulty.config.value.loot_scaling, level);
        if (result.level() <= 0) return null;
        float addition = 0, multiply = 0;
        for (var modifier : result.modifiers()) {
            if (!PatternMatching.regexMatches("minecraft:generic.attack_damage", modifier.attribute)) continue;
            float value = modifier.randomizedValue(result.level());
            if (!Float.isFinite(value)) continue;
            if (modifier.operation == Config.Operation.ADDITION) addition += value;
            else if (modifier.operation == Config.Operation.MULTIPLY_BASE) multiply += value;
        }
        if (addition == 0 && multiply == 0) return null;
        return new AbilityDamageScope.Cast(actor.getUuid(), level, addition, multiply);
    }

    public static void spawned(EntityJoinLevelEvent event) {
        var cast = AbilityDamageScope.current();
        var entity = event.getEntity();
        if (cast == null || event.getLevel().isClient || !isEffect(entity)) return;
        // Spawned children inherit the original cast too; existing saved snapshots are retained.
        if (entity.getPersistentData().contains(DATA)) return;
        var data = new NbtCompound();
        data.putUuid("caster", cast.caster());
        data.putInt("level", cast.level());
        data.putFloat("addition", cast.addition());
        data.putFloat("multiply", cast.multiplyBase());
        entity.getPersistentData().put(DATA, data);
    }

    public static AbilityDamageScope.Cast savedCast(Entity entity) {
        if (!isEffect(entity) || !AccessoryScalingConfig.get().enabled) return null;
        if (!entity.getPersistentData().contains(DATA)) return null;
        var data = entity.getPersistentData().getCompound(DATA);
        if (!data.containsUuid("caster") || data.getInt("level") <= 0) return null;
        float addition = data.getFloat("addition"), multiply = data.getFloat("multiply");
        if (!Float.isFinite(addition) || !Float.isFinite(multiply)) return null;
        return new AbilityDamageScope.Cast(data.getUuid("caster"), data.getInt("level"), addition, multiply);
    }

    private static boolean isEffect(Entity entity) {
        return EFFECT_TYPES.get(entity.getClass());
    }
}
