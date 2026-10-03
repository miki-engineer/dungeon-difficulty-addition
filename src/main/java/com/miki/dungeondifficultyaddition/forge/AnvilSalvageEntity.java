package com.miki.dungeondifficultyaddition.forge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Persistent anchored display, not an ItemEntity: hoppers cannot steal it or merge stacks into it. */
public final class AnvilSalvageEntity extends Entity {
    private static final TrackedData<ItemStack> STACK = DataTracker.registerData(AnvilSalvageEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> HITS = DataTracker.registerData(AnvilSalvageEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private BlockPos anchor = BlockPos.ORIGIN;
    private GemKind kind;
    private int level;
    private SalvageProgress progress = new SalvageProgress(0, 0);
    private long nextWeakHammerAlertTick;

    public AnvilSalvageEntity(EntityType<? extends AnvilSalvageEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(STACK, ItemStack.EMPTY); builder.add(HITS, 0);
    }
    public ItemStack stack() { return dataTracker.get(STACK); }
    public int hits() { return dataTracker.get(HITS); }
    public BlockPos anchor() { return anchor; }
    public void initialize(BlockPos pos, ItemStack stack, GemKind category, int itemLevel, float yaw) {
        anchor = pos.toImmutable(); kind = category; level = itemLevel;
        dataTracker.set(STACK, stack.copyWithCount(1));
        setPosition(pos.getX() + .5, pos.getY() + 1.025, pos.getZ() + .5);
        setYaw(yaw);
    }
    @Override public void tick() {
        super.tick();
        setVelocity(Vec3d.ZERO);
        if (getWorld().isClient) return;
        if (stack().isEmpty()) { discard(); return; }
        if (!getWorld().getBlockState(anchor).isIn(BlockTags.ANVIL)) release();
        else if (getX() != anchor.getX() + .5 || getY() != anchor.getY() + 1.025 || getZ() != anchor.getZ() + .5)
            setPosition(anchor.getX() + .5, anchor.getY() + 1.025, anchor.getZ() + .5);
    }
    public boolean canUse(PlayerEntity player) {
        return !isRemoved() && !player.isSpectator() && player.getAbilities().allowModifyWorld
                && player.canInteractWithBlockAt(anchor, 0)
                && getWorld().canPlayerModifyAt(player, anchor);
    }
    public void strike(PlayerEntity player) {
        if (!(getWorld() instanceof ServerWorld world) || !SalvageConfig.get().enabled || !canUse(player)
                || !world.getBlockState(anchor).isIn(BlockTags.ANVIL) || stack().isEmpty()) return;
        var held = player.getMainHandStack();
        if (!(held.getItem() instanceof SalvageHammerItem hammer)) return;
        if (!hammer.accepts(level)) {
            if (world.getTime() >= nextWeakHammerAlertTick) {
                nextWeakHammerAlertTick = world.getTime() + 20;
                player.sendMessage(Text.translatable("salvage.dungeon_difficulty_addition.too_high", hammer.maximumLevel(), level)
                        .formatted(Formatting.RED), true);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, .35F, 1F);
                }
            }
            return;
        }
        if (kind == null || !ForgeRules.validLevel(level)) return;
        var next = progress.strike(world.getTime());
        if (next == progress) return;
        var dismantledItem = next.complete() ? stack().copy() : ItemStack.EMPTY;
        progress = next;
        dataTracker.set(HITS, progress.hits());
        if (next.complete()) {
            // Re-resolve the shared config for equipment placed before a config change/restart.
            var currentKind = SalvageEquipment.kind(stack());
            if (currentKind == null || !releaseAs(LevelGemItem.createFragment(currentKind, level, 1))) {
                progress = new SalvageProgress(2, world.getTime() + SalvageProgress.INTERVAL_TICKS);
                dataTracker.set(HITS, 2);
                return;
            }
        }
        // One durability per accepted strike, following vanilla enchantment and creative rules.
        held.damage(1, player, EquipmentSlot.MAINHAND);
        world.playSound(null, anchor, next.complete() ? SoundEvents.BLOCK_ANVIL_DESTROY : SoundEvents.BLOCK_ANVIL_USE,
                SoundCategory.BLOCKS, .7F, next.complete() ? 1.4F : .85F + progress.hits() * .12F);
        playStrikeParticles(world, dismantledItem, next.complete());
        player.sendMessage(next.complete()
                ? Text.translatable("salvage.dungeon_difficulty_addition.complete").formatted(Formatting.GREEN)
                : Text.translatable("salvage.dungeon_difficulty_addition.progress", progress.hits()).formatted(Formatting.GOLD), true);
    }
    private void playStrikeParticles(ServerWorld world, ItemStack equipment, boolean complete) {
        double x = getX(), y = getY() + .10, z = getZ();
        // Short, localized impacts; emitted only for accepted server-side strikes.
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                complete ? 18 : 6, .14, .05, .14, complete ? .10 : .05);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(anchor)),
                x, y, z, complete ? 12 : 4, .12, .03, .12, .025);
        if (complete) {
            world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, equipment),
                    x, y + .06, z, 14, .16, .08, .16, .08);
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                    x, y + .08, z, 12, .18, .10, .18, .05);
        }
    }
    private boolean releaseAs(ItemStack result) {
        if (!(getWorld() instanceof ServerWorld world) || result.isEmpty()) return false;
        var drop = new ItemEntity(world, getX(), getY() + .12, getZ(), result.copy());
        drop.setToDefaultPickupDelay();
        if (!world.spawnEntity(drop)) return false;
        dataTracker.set(STACK, ItemStack.EMPTY);
        discard();
        return true;
    }
    public void release() { if (!stack().isEmpty()) releaseAs(stack()); }
    @Override public ActionResult interact(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND || !player.isSneaking()) return ActionResult.PASS;
        if (player.getStackInHand(hand).isEmpty()) {
            if (!getWorld().isClient && canUse(player)) release();
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
    @Override public boolean canHit() { return true; }
    @Override public boolean isAttackable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluids() { return false; }
    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.put("Item", stack().encodeAllowEmpty(getRegistryManager()));
        nbt.putLong("Anvil", anchor.asLong()); nbt.putInt("Level", level);
        nbt.putString("GemKind", kind == null ? "" : kind.id());
        nbt.putInt("Hits", progress.hits()); nbt.putLong("NextStrike", progress.nextAllowedTick());
    }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        dataTracker.set(STACK, ItemStack.fromNbtOrEmpty(getRegistryManager(), nbt.getCompound("Item")));
        anchor = BlockPos.fromLong(nbt.getLong("Anvil")); level = nbt.getInt("Level"); kind = GemKind.read(nbt.getString("GemKind"));
        progress = new SalvageProgress(Math.clamp(nbt.getInt("Hits"), 0, 2), nbt.getLong("NextStrike"));
        dataTracker.set(HITS, progress.hits()); setNoGravity(true);
    }
}
