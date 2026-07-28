package org.crafterscr.crafterssoccer.physics;

import net.minecraft.util.Mth;

/** Deterministic conversion from kick charge to horizontal power. */
public final class KickResolver {
    static final double MIN_POWER = 0.38D;
    static final double MAX_POWER = 1.45D;

    private KickResolver() { }

    public static double horizontalPower(float charge) {
        float safeCharge = Mth.clamp(charge, 0.0F, 1.0F);
        double curvedCharge = Math.pow(safeCharge, 1.85D);
        return Mth.lerp(curvedCharge, MIN_POWER, MAX_POWER);
    }
}
