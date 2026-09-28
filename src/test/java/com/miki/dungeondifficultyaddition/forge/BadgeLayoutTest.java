package com.miki.dungeondifficultyaddition.forge;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BadgeLayoutTest {
    @Test void preservesApprovedSmallBadge() {
        assertEquals(.60F, BadgeLayout.SCALE);
        assertEquals(8, BadgeLayout.HEIGHT);
        assertEquals(7, BadgeLayout.forAdvance(4).width());
        assertEquals(.60F, BadgeLayout.forAdvance(4).textScale());
    }
    @Test void centersShortAndLongLabelsAtEveryGuiScale() {
        // Includes widths beyond ordinary Roman numerals and wider resource-pack fonts.
        for (int advance = 1; advance <= 256; advance++) {
            var layout = BadgeLayout.forAdvance(advance);
            float inkWidth = Math.max(1, advance - 1) * layout.textScale();
            for (int guiScale = 1; guiScale <= 8; guiScale++) {
                float scale = BadgeLayout.SCALE * guiScale;
                assertEquals(layout.textX() * scale,
                        (layout.width() - layout.textX() - inkWidth) * scale, .0001F);
                assertEquals(layout.textY() * scale,
                        (BadgeLayout.HEIGHT - layout.textY() - 7 * layout.textScale()) * scale, .0001F);
            }
            assertTrue(layout.textX() >= 1);
            assertTrue(layout.textY() >= 1);
            assertTrue(layout.width() * BadgeLayout.SCALE <= 9.61F);
            assertTrue(BadgeLayout.HEIGHT * BadgeLayout.SCALE < 6);
        }
    }
}
