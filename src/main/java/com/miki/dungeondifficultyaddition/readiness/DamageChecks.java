package com.miki.dungeondifficultyaddition.readiness;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Opt-in, bounded observation. Never changes damage, gear, configuration or encounter data. */
public final class DamageChecks {
    private static final Map<UUID, Integer> REMAINING = new ConcurrentHashMap<>();
    private static final Map<UUID, Trace> PENDING = new ConcurrentHashMap<>();
    private static final AtomicLong IDS = new AtomicLong();

    static final class Trace {
        final long id = IDS.incrementAndGet();
        final ServerPlayerEntity viewer;
        final DamageSource source;
        final float original;
        final float before;

        Trace(ServerPlayerEntity viewer, LivingIncomingDamageEvent event) {
            this.viewer = viewer;
            source = event.getSource();
            original = event.getOriginalAmount();
            before = event.getAmount();
        }
    }

    private DamageChecks() {}

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(CommandManager.literal("dda_damage_debug")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("on").executes(context -> {
                    var player = context.getSource().getPlayerOrThrow();
                    REMAINING.put(player.getUuid(), 20);
                    player.sendMessage(Text.literal("Damage checks ON for your next 20 incoming/outgoing hits. "
                            + "Results appear in chat and latest.log. Two HP = one heart."), false);
                    return 1;
                }))
                .then(CommandManager.literal("off").executes(context -> {
                    var player = context.getSource().getPlayerOrThrow();
                    remove(player.getUuid());
                    player.sendMessage(Text.literal("Damage checks OFF."), false);
                    return 1;
                })));
    }

    static Trace begin(LivingIncomingDamageEvent event) {
        try {
            var attacker = event.getSource().getAttacker();
            var victim = event.getEntity();
            var viewer = victim instanceof ServerPlayerEntity player && REMAINING.containsKey(player.getUuid()) ? player
                    : attacker instanceof ServerPlayerEntity player && REMAINING.containsKey(player.getUuid()) ? player : null;
            if (viewer == null) return null;
            var left = REMAINING.getOrDefault(viewer.getUuid(), 0);
            if (left <= 0) return null;
            if (left == 1) REMAINING.remove(viewer.getUuid());
            else REMAINING.put(viewer.getUuid(), left - 1);
            var trace = new Trace(viewer, event);
            // A canceled hit may not produce Post. Keep bounded state, overwritten by the next hit.
            if (PENDING.size() >= 128) PENDING.clear();
            PENDING.put(victim.getUuid(), trace);
            print(trace, "source=" + event.getSource().getName()
                    + ", attacker=" + (attacker == null ? "unattributed" : Registries.ENTITY_TYPE.getId(attacker.getType()))
                    + ", target=" + Registries.ENTITY_TYPE.getId(victim.getType()) + "/" + victim.getId());
            if (left == 1) viewer.sendMessage(Text.literal("Damage-check limit reached; use /dda_damage_debug on for 20 more hits."), false);
            return trace;
        } catch (RuntimeException | LinkageError exception) {
            Encounters.warn("damage-check-start", exception);
            return null;
        }
    }

    static void adjusted(Trace trace, String rule, double multiplier, float after) {
        if (trace == null) return;
        try {
            print(trace, rule + "; before=" + trace.before + " HP, multiplier=" + multiplier
                    + ", after readiness=" + after + " HP (before armor)");
        } catch (RuntimeException | LinkageError exception) {
            Encounters.warn("damage-check-adjusted", exception);
        }
    }

    static void post(LivingDamageEvent.Post event) {
        var trace = PENDING.remove(event.getEntity().getUuid());
        if (trace == null || trace.source != event.getSource() || trace.original != event.getOriginalDamage()) return;
        try {
            print(trace, "final damage=" + event.getNewDamage() + " HP (" + event.getNewDamage() / 2
                    + " hearts), remaining health=" + event.getEntity().getHealth() + " HP");
        } catch (RuntimeException | LinkageError exception) {
            Encounters.warn("damage-check-final", exception);
        }
    }

    static void logout(PlayerEvent.PlayerLoggedOutEvent event) { remove(event.getEntity().getUuid()); }
    static void stopped(ServerStoppedEvent event) { REMAINING.clear(); PENDING.clear(); }

    private static void remove(UUID player) {
        REMAINING.remove(player);
        PENDING.values().removeIf(trace -> trace.viewer.getUuid().equals(player));
    }

    private static void print(Trace trace, String text) {
        var message = "[DDA damage #" + trace.id + "] " + text;
        trace.viewer.sendMessage(Text.literal(message), false);
        LoggerFactory.getLogger("dungeon_difficulty_addition").info("{}", message);
    }
}
