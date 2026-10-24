package com.miki.dungeondifficultyaddition.readiness;

import com.miki.dungeondifficultyaddition.compat.AccessoryFamilies;
import com.miki.dungeondifficultyaddition.compat.OptionalModSupport;
import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import com.miki.dungeondifficultyaddition.scaling.ItemTraits;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

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
        if (isWeapon(player.getMainHandStack())) LAST_WEAPON_SLOT.put(player, player.getInventory().selectedSlot);
    }

    /** The held weapon; while holding food, blocks or a torch, the last weapon held keeps counting. */
    static ItemStack weapon(PlayerEntity player) {
        var held = player.getMainHandStack();
        if (isWeapon(held)) return held;
        var slot = LAST_WEAPON_SLOT.get(player);
        if (slot == null || slot < 0 || slot >= PlayerInventory.getHotbarSize()) return ItemStack.EMPTY;
        var last = player.getInventory().getStack(slot);
        return isWeapon(last) ? last : ItemStack.EMPTY;
    }

    private static boolean isWeapon(ItemStack stack) {
        return AccessoryItemScaling.isForgeEquipment(stack) && !(stack.getItem() instanceof ArmorItem)
                && !AccessoryFamilies.accessory(ItemTraits.of(stack).namespace());
    }

    public static double gearScore(List<Slot> slots) {
        var required = new ArrayList<Integer>();
        var optional = new ArrayList<Integer>();
        for (var slot : slots) {
            for (int count = 0; count < slot.weight(); count++) (slot.optional() ? optional : required).add(slot.level());
        }
        return ReadinessMath.gearScore(required, optional, EncounterConfig.get().max_level);
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
