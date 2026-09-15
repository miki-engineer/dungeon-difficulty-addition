package com.miki.dungeondifficultyaddition.readiness;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class EncounterEvents {
    private EncounterEvents() {}

    public static void register() {
        // This event precedes armor/toughness/protection. Change the existing hit, never emit a second hit.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, EncounterEvents::incoming);
        NeoForge.EVENT_BUS.addListener(EncounterEvents::tick);
        NeoForge.EVENT_BUS.addListener(EncounterCommands::register);
        NeoForge.EVENT_BUS.addListener(DamageChecks::register);
        NeoForge.EVENT_BUS.addListener(DamageChecks::post);
        NeoForge.EVENT_BUS.addListener(DamageChecks::logout);
        NeoForge.EVENT_BUS.addListener(DamageChecks::stopped);
    }

    private static void tick(EntityTickEvent.Pre event) {
        if (EncounterConfig.get().active() && event.getEntity() instanceof MobEntity mob
                && !mob.getWorld().isClient && !mob.getPersistentData().contains(Encounters.KEY)) {
            // First server tick avoids querying structures while a chunk is being deserialized.
            Encounters.get(mob);
        }
    }

    private static boolean participating(ServerPlayerEntity player) {
        return !player.isCreative() && !player.isSpectator();
    }

    private static void incoming(LivingIncomingDamageEvent event) {
        var config = EncounterConfig.get().player_readiness;
        var trace = DamageChecks.begin(event);
        if (!config.enabled || event.isCanceled() || !Float.isFinite(event.getAmount()) || event.getAmount() <= 0) {
            DamageChecks.adjusted(trace, !config.enabled ? "readiness disabled" : "canceled/invalid hit", 1, event.getAmount());
            return;
        }
        double multiplier = 1;
        String rule = "no eligible player/mob pair, or outgoing rule disabled";
        try {
            // getAttacker is Yarn's name for DamageSource.getEntity: shooter, not the arrow itself.
            var attacker = event.getSource().getAttacker();
            var victim = event.getEntity();
            if (victim instanceof ServerPlayerEntity player && participating(player) && attacker instanceof MobEntity mob) {
                var encounter = Encounters.get(mob);
                if (encounter == null) {
                    rule = "incoming: no eligible recorded encounter";
                    return;
                }
                int readiness = EquippedLevels.readiness(EquippedLevels.slots(player));
                rule = "incoming: " + encounter.scope() + " level=" + encounter.level() + ", armor/Curios readiness=" + readiness;
                multiplier = ReadinessMath.incoming(encounter.level(), readiness, config.incoming_penalty_per_level);
            } else if (config.outgoing_penalty_enabled && attacker instanceof ServerPlayerEntity player
                    && participating(player) && victim instanceof MobEntity mob) {
                var encounter = Encounters.get(mob);
                if (encounter == null) {
                    rule = "outgoing: no eligible recorded encounter";
                    return;
                }
                // Explicit hit-time policy also covers attributed projectiles and spells.
                int weapon = EquippedLevels.level(player.getMainHandStack());
                rule = "outgoing: " + encounter.scope() + " level=" + encounter.level() + ", main-hand level=" + weapon;
                multiplier = ReadinessMath.outgoing(encounter.level(), weapon,
                        config.outgoing_penalty_per_level, config.maximum_outgoing_reduction);
            }
            var amount = event.getAmount() * multiplier;
            if (Double.isFinite(amount) && amount <= Float.MAX_VALUE) event.setAmount((float) amount);
        } catch (RuntimeException | LinkageError exception) {
            rule = "lookup failed; original hit preserved";
            multiplier = 1;
            Encounters.warn("damage", exception);
        } finally {
            DamageChecks.adjusted(trace, rule, multiplier, event.getAmount());
        }
    }
}
