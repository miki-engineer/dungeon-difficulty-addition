package com.miki.dungeondifficultyaddition.readiness;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

final class EncounterCommands {
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(CommandManager.literal("dda_readiness")
                .executes(context -> player(context.getSource(), context.getSource().getPlayerOrThrow()))
                .then(CommandManager.literal("player").requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("target", EntityArgumentType.player()).executes(context ->
                                player(context.getSource(), EntityArgumentType.getPlayer(context, "target")))))
                .then(CommandManager.literal("encounter").requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("target", EntityArgumentType.entity()).executes(context -> {
                            var target = EntityArgumentType.getEntity(context, "target");
                            if (!(target instanceof MobEntity mob)) {
                                context.getSource().sendError(Text.literal("Select a mob."));
                                return 0;
                            }
                            return encounter(context.getSource(), mob);
                        }))));
    }

    private static int encounter(ServerCommandSource source, MobEntity mob) {
        try {
            var encounter = Encounters.get(mob);
            reply(source, "Encounter: " + encounter + "; saved data: " + mob.getPersistentData().getCompound(Encounters.KEY));
            if (encounter != null && source.getEntity() instanceof ServerPlayerEntity viewer) {
                var config = EncounterConfig.get().player_readiness;
                var ready = EquippedLevels.readiness(EquippedLevels.slots(viewer));
                reply(source, "Your incoming multiplier: " + ReadinessMath.incoming(encounter.level(), ready,
                        config.incoming_penalty_per_level) + "; outgoing multiplier: "
                        + ReadinessMath.outgoing(encounter.level(), EquippedLevels.level(viewer.getMainHandStack()),
                        config.outgoing_penalty_per_level, config.maximum_outgoing_reduction)
                        + " (applied only when the corresponding readiness settings are enabled)");
            }
            return 1;
        } catch (RuntimeException | LinkageError exception) {
            Encounters.warn("diagnostics", exception);
            source.sendError(Text.literal("Encounter inspection failed; see the server log."));
            return 0;
        }
    }

    private static int player(ServerCommandSource source, ServerPlayerEntity player) {
        try {
            var slots = EquippedLevels.slots(player);
            reply(source, "Readiness enabled: " + EncounterConfig.get().player_readiness.enabled
                    + "; required items: " + EncounterConfig.get().player_readiness.required_equipped_items);
            for (var slot : slots) reply(source, slot.name() + ": " + slot.item() + " level " + slot.level());
            reply(source, "Armor/Curios readiness: " + EquippedLevels.readiness(slots)
                    + "; main-hand level: " + EquippedLevels.level(player.getMainHandStack()));
            return 1;
        } catch (RuntimeException | LinkageError exception) {
            Encounters.warn("diagnostics", exception);
            source.sendError(Text.literal("Equipment lookup failed; see the server log."));
            return 0;
        }
    }

    private static void reply(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message), false);
    }
}
