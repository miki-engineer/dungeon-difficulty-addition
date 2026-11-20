package com.miki.dungeondifficultyaddition.compat;

import com.miki.dungeondifficultyaddition.scaling.AccessoryItemScaling;
import com.miki.dungeondifficultyaddition.scaling.ItemTraits;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * Scales the stats of the extra relic mods' curios (scaling.extra_relic_namespaces, Realmbound Content by default) by item level,
 * as the Jewelry and Relics curio-class mixins do for those mods. Curios fires its attribute event for every curio, on the server
 * when it is equipped or taken off and on the client for its tooltip, so no item class needs hooking. Loaded only when Curios is.
 */
public final class ExtraCurioScaling {
    private ExtraCurioScaling() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ExtraCurioScaling::modifiers);
    }

    private static void modifiers(CurioAttributeModifierEvent event) {
        var stack = event.getItemStack();
        if (!AccessoryFamilies.extraRelics(ItemTraits.of(stack).namespace())) return;
        var scaled = AccessoryItemScaling.scaledCuriosModifiers(stack, event.getModifiers());
        if (scaled == null) return;
        event.clearModifiers();
        scaled.forEach(event::addModifier);
    }
}
