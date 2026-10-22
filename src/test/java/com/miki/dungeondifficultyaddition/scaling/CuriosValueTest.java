package com.miki.dungeondifficultyaddition.scaling;

import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CuriosValueTest {
    @Test void flatValuesKeepTooltipPrecision() {
        assertEquals(0.04, AccessoryItemScaling.compactCuriosValue(0.04, Operation.ADD_VALUE));
        assertEquals(2.35, AccessoryItemScaling.compactCuriosValue(2.345, Operation.ADD_VALUE));
        assertEquals(0.125, AccessoryItemScaling.compactCuriosValue(0.1249, Operation.ADD_MULTIPLIED_BASE));
    }
}
