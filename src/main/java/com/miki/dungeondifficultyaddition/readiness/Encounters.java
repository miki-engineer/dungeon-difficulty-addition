package com.miki.dungeondifficultyaddition.readiness;

import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Records once, then survives movement and chunk reloads through NeoForge persistent data. */
public final class Encounters {
    public static final String KEY = "dungeon_difficulty_addition.encounter";
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    public record Encounter(String scope, int level, String origin) {}

    private Encounters() {}

    public static boolean eligible(Entity entity) {
        return entity instanceof MobEntity && entity.getType().getSpawnGroup() == SpawnGroup.MONSTER
                && !(entity instanceof Tameable tameable && tameable.getOwnerUuid() != null)
                && !(entity instanceof Ownable ownable && ownable.getOwner() != null);
    }

    public static Encounter get(MobEntity mob) {
        if (!EncounterConfig.get().active() || !(mob.getWorld() instanceof ServerWorld world)) return null;
        try {
            if (!eligible(mob)) return null;
            var data = mob.getPersistentData();
            if (data.contains(KEY, NbtElement.COMPOUND_TYPE)) {
                var saved = data.getCompound(KEY);
                return supported(saved.getString("scope"), saved.getInt("level"), saved.getString("origin"));
            }
            // Preserve valid prototype encounter records instead of moving an old mob to a new level.
            if (data.contains("realmbound_balance_level", NbtElement.NUMBER_TYPE)) {
                var tags = mob.getCommandTags();
                var scope = tags.contains("realmbound.heroic") ? "heroic"
                        : tags.contains("realmbound.dungeon") ? "dungeon" : "";
                if (!scope.isEmpty()) return save(mob, scope, data.getInt("realmbound_balance_level"), "legacy");
            }
            var location = PatternMatching.LocationData.create(world, mob.getBlockPos());
            // Passing the entity ID is essential for configured boss/entity overrides.
            var difficulty = PatternMatching.getDifficulty(location, Registries.ENTITY_TYPE.getId(mob.getType()), world);
            return difficulty == null || !difficulty.isValid() ? save(mob, "", 0, "none")
                    : save(mob, difficulty.type().name, difficulty.entityLevel(),
                            world.getRegistryKey().getValue() + " " + mob.getBlockPos().toShortString());
        } catch (RuntimeException | LinkageError exception) {
            warn("encounter", exception);
            // Cache failure on this mob to avoid repeatedly throwing during hits/ticks. Diagnostics expose it.
            return save(mob, "", 0, "lookup_failed");
        }
    }

    private static Encounter save(MobEntity mob, String scope, int level, String origin) {
        var data = new NbtCompound();
        data.putInt("version", 1);
        data.putString("scope", scope);
        data.putInt("level", ReadinessMath.clamp(level, EncounterConfig.get().max_level));
        data.putString("origin", origin);
        mob.getPersistentData().put(KEY, data);
        return supported(scope, level, origin);
    }

    private static Encounter supported(String scope, int level, String origin) {
        return EncounterConfig.get().scopes.contains(scope)
                ? new Encounter(scope, ReadinessMath.clamp(level, EncounterConfig.get().max_level), origin) : null;
    }

    public static void warn(String operation, Throwable exception) {
        if (WARNED.add(operation)) LoggerFactory.getLogger("dungeon_difficulty_addition")
                .warn("Encounter feature failed during {}; preserving original damage. Further warnings suppressed.", operation, exception);
    }
}
