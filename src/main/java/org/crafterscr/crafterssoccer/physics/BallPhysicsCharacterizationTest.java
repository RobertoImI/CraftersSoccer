package org.crafterscr.crafterssoccer.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;

class BallPhysicsCharacterizationTest {
    @Test void kickPowerPreservesEndpointsAndCurve() {
        assertEquals(0.38D, KickResolver.horizontalPower(-1.0F), 1.0E-9D);
        assertEquals(1.45D, KickResolver.horizontalPower(2.0F), 1.0E-9D);
        assertEquals(0.6768098338D, KickResolver.horizontalPower(0.5F), 1.0E-8D);
    }

    @Test void groundMotionPreservesFrictionAndStopThresholds() {
        Vec3 result = SurfaceMotionResolver.resolve(new Vec3(0.5D, 0.02D, 0.005D), true);
        assertEquals(new Vec3(0.458D, 0.0D, 0.0D), result);
    }

    @Test void wallAndFloorBouncePreserveVelocity() {
        Vec3 result = BlockBounceResolver.resolve(
                new Vec3(1.0D, -0.5D, 0.25D), new Vec3(0.0D, 0.0D, 0.25D),
                new Vec3(1.0D, -0.5D, 0.25D), true);
        assertEquals(new Vec3(-0.65D, 0.24D, 0.25D), result);
    }
}
