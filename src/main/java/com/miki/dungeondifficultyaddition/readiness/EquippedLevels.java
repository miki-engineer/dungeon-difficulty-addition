package com.miki.dungeondifficultyaddition.readiness;

import com.miki.dungeondifficultyaddition.compat.AccessoryFamilies;
import com.miki.dungeondifficultyaddition.compat.OptionalModSupport;
import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import com.miki.dungeondifficultyaddition.scaling.ItemTraits;
import com.miki.dungeondifficultyaddition.scaling.LevelBonus;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class EquippedLevels {
    /** The weapon slot counts twice, so a weak weapon still clearly lowers the score. */
    public static final int WEAPON_WEIGHT = 2;
    // Hotbar slot of each player's last held weapon. Client and server player objects are separate
    // keys (they share a JVM in singleplayer); weak keys drop players that leave.
    private static final Map<PlayerEntity, Integer> LAST_WEAPON_SLOT = Collections.synchronizedMap(new WeakHashMap<>());
    /** Optional slots (Curios) only count when they raise the score. */
    public record Slot(String name, String item, int level, int weight, boolean optional) {
        /** Optional slots count exactly when they are above the final score. */
        public boolean counts(double gearScore) { return !optional || level > gearScore; }
    }
    private EquippedLevels() {}

    public static int level(ItemStack stack) {
        var config = EncounterConfig.get();
        if (stack.isEmpty()) return 0;
        return ReadinessMath.clamp(ItemScaling.isScaled(stack) ? ItemScaling.getScaleFactor(stack)
                : config.player_readiness.unmarked_item_level, config.max_level);
    }

    static void addCurio(List<Slot> slots, String name, ItemStack stack) {
        if (!stack.isEmpty()) slots.add(new Slot(name, id(stack), level(stack), 1, true));
    }

    /** All four armor slots and the weapon slot (empty ones as level 0), and occupied active Curios slots. */
    public static List<Slot> slots(PlayerEntity player) {
        var slots = new ArrayList<Slot>();
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            slots.add(required(slot.getName(), player.getEquippedStack(slot), 1));
        }
        slots.add(required("weapon", weapon(player), WEAPON_WEIGHT));
        if (OptionalModSupport.isLoaded("curios")) CuriosLevels.append(player, slots);
        return List.copyOf(slots);
    }

    /** Called every player tick on both sides, so the client tab and server damage agree. */
    public static void trackWeapon(PlayerEntity player) {
        if (replacesRemembered(player, player.getMainHandStack())) {
            LAST_WEAPON_SLOT.put(player, player.getInventory().selectedSlot);
        }
    }

    /**
     * The remembered weapon slot item. Holding a weapon always takes the slot, even a weaker one. A held
     * tool takes it only when higher, so tools can raise the score but never lower it; with no weapon in
     * the hotbar, the best tool held keeps counting. Food, blocks or a torch leave the slot as it is.
     */
    static ItemStack weapon(PlayerEntity player) {
        var held = player.getMainHandStack();
        return replacesRemembered(player, held) ? held : remembered(player);
    }

    private static boolean replacesRemembered(PlayerEntity player, ItemStack held) {
        return isWeapon(held) || (isTool(held) && level(held) > level(remembered(player)));
    }

    private static ItemStack remembered(PlayerEntity player) {
        var slot = LAST_WEAPON_SLOT.get(player);
        if (slot == null || slot < 0 || slot >= PlayerInventory.getHotbarSize()) return ItemStack.EMPTY;
        var last = player.getInventory().getStack(slot);
        return isHeldGear(last) ? last : ItemStack.EMPTY;
    }

    /** Held equipment that is not armor or an accessory: weapons and tools. */
    private static boolean isHeldGear(ItemStack stack) {
        return AccessoryItemScaling.isForgeEquipment(stack) && !(stack.getItem() instanceof ArmorItem)
                && !(AccessoryFamilies.accessory(ItemTraits.of(stack).namespace()) && !com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling.isExtraModEquipment(stack));
    }

    /**
     * Pickaxes, shovels and hoes live in the hotbar, so they are tools, not the remembered weapon. Axes
     * stay weapons: combat packs use them as weapons. Tags cover modded tools.
     */
    private static boolean isTool(ItemStack stack) {
        return isHeldGear(stack)
                && (stack.isIn(ItemTags.PICKAXES) || stack.isIn(ItemTags.SHOVELS) || stack.isIn(ItemTags.HOES));
    }

    private static boolean isWeapon(ItemStack stack) {
        return isHeldGear(stack) && !isTool(stack);
    }

    public static double gearScore(List<Slot> slots) {
        var required = new ArrayList<Integer>();
        var optional = new ArrayList<Integer>();
        for (var slot : slots) {
            for (int count = 0; count < slot.weight(); count++) (slot.optional() ? optional : required).add(slot.level());
        }
        return ReadinessMath.withLevelOneFloor(ReadinessMath.gearScore(required, optional, EncounterConfig.get().max_level),
                LevelBonus.levelOneIsBase());
    }

    private static Slot required(String name, ItemStack stack, int weight) {
        return stack.isEmpty() ? new Slot(name, "empty", 0, weight, false)
                : new Slot(name, id(stack), level(stack), weight, false);
    }

    public static String format(double gearScore) {
        return String.format(Locale.ROOT, "%.1f", gearScore);
    }

    private static String id(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }
}
