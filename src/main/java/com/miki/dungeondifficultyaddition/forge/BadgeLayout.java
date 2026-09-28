package com.miki.dungeondifficultyaddition.forge;

/** Pure geometry for the approved compact badge, measured before its outer scale. */
public record BadgeLayout(int width, float textScale, float textX, float textY) {
    public static final float SCALE = .60F;
    public static final int HEIGHT = 8;
    public static BadgeLayout forAdvance(int advance) {
        int inkWidth = Math.max(1, advance - 1);
        float scale = Math.min(.60F, 11F / inkWidth);
        int width = Math.max(7, (int) Math.ceil(inkWidth * scale) + 4);
        return new BadgeLayout(width, scale, (width - inkWidth * scale) / 2F,
                (HEIGHT - 7F * scale) / 2F);
    }
}
