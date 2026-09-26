package com.miki.dungeondifficultyaddition.command;

import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DungeonDifficultyCommands {
    private DungeonDifficultyCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(CommandManager.literal("dda")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument(
                                                "item",
                                                ItemStackArgumentType.itemStack(event.getBuildContext())
                                        )
                                        .then(CommandManager.argument(
                                                        "level",
                                                        IntegerArgumentType.integer(1)
                                                )
                                                .executes(context -> giveScaledItem(
                                                        context.getSource(),
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                        ItemStackArgumentType.getItemStackArgument(context, "item")
                                                                .createStack(1, false),
                                                        IntegerArgumentType.getInteger(context, "level")
                                                )))))));
    }

    private static int giveScaledItem(
            ServerCommandSource source,
            ServerPlayerEntity player,
            ItemStack stack,
            int level
    ) {
        var appliedLevel = AccessoryItemScaling.applyCommandLevel(stack, level);
        if (appliedLevel <= 0) {
            source.sendError(Text.literal("That item cannot be scaled."));
            return 0;
        }

        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
        var fixedLevelApplied = appliedLevel != level;
        source.sendFeedback(() -> Text.literal("Gave " + player.getName().getString()
                        + " " + stack.getName().getString() + " at level " + appliedLevel + "."
                        + (fixedLevelApplied ? " The configured fixed level overrode the requested level." : "")),
                true);
        return 1;
    }
}
