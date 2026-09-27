package com.miki.dungeondifficultyaddition.forge;

import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class AnvilSalvageEvents {
    private AnvilSalvageEvents() {}
    private static AnvilSalvageEntity find(World world, BlockPos pos) {
        return world.getEntitiesByClass(AnvilSalvageEntity.class, new Box(pos).expand(0, 1, 0),
                e -> !e.isRemoved() && e.getBlockPos().down().equals(pos)).stream().findFirst().orElse(null);
    }
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var world = player.getWorld();
        var pos = event.getPos();
        if (event.getHand() != Hand.MAIN_HAND || !player.isSneaking()
                || !world.getBlockState(pos).isIn(BlockTags.ANVIL)) return;
        var held = player.getMainHandStack();
        var placed = find(world, pos);
        if (held.isEmpty() && placed != null) {
            event.setCanceled(true); event.setCancellationResult(ActionResult.SUCCESS);
            if (!world.isClient && placed.canUse(player)) placed.release();
            return;
        }
        if (!SalvageConfig.get().enabled) return;
        var kind = SalvageEquipment.kind(held);
        if (kind == null) return;
        event.setCanceled(true); event.setCancellationResult(ActionResult.SUCCESS);
        if (world.isClient || player.isSpectator() || !player.getAbilities().allowModifyWorld
                || !player.canInteractWithBlockAt(pos, 0) || !world.canPlayerModifyAt(player, pos)) return;
        if (placed != null) {
            player.sendMessage(Text.translatable("salvage.dungeon_difficulty_addition.occupied"), true);
            return;
        }
        var display = new AnvilSalvageEntity(RunicForge.PLACED_ITEM.get(), world);
        display.initialize(pos, held, kind, ItemScaling.getScaleFactor(held), player.getYaw());
        if (world.spawnEntity(display)) held.decrement(1);
    }
    public static void hitBlock(PlayerInteractEvent.LeftClickBlock event) {
        var player = event.getEntity();
        if (!(player.getMainHandStack().getItem() instanceof SalvageHammerItem)
                || !player.getWorld().getBlockState(event.getPos()).isIn(BlockTags.ANVIL)) return;
        event.setCanceled(true);
        if (!player.getWorld().isClient && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            var placed = find(player.getWorld(), event.getPos());
            if (placed != null) placed.strike(player);
        }
    }
    public static void hitEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof AnvilSalvageEntity placed)) return;
        event.setCanceled(true);
        if (!event.getEntity().getWorld().isClient) placed.strike(event.getEntity());
    }
}
