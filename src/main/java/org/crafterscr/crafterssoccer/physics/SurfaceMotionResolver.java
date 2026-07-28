package org.crafterscr.crafterssoccer.physics;

import net.minecraft.world.phys.Vec3;

/** Applies deterministic ground friction, air drag and rest thresholds. */
public final class SurfaceMotionResolver {
    private SurfaceMotionResolver() { }

    public static Vec3 resolve(Vec3 velocity, boolean onGround) {
        double x = velocity.x;
        double y = velocity.y;
        double z = velocity.z;
        if (onGround) {
            x *= SoccerBallPhysics.GROUND_FRICTION;
            z *= SoccerBallPhysics.GROUND_FRICTION;
            if (Math.abs(x) < SoccerBallPhysics.STOP_HORIZONTAL_SPEED) x = 0.0D;
            if (Math.abs(z) < SoccerBallPhysics.STOP_HORIZONTAL_SPEED) z = 0.0D;
            if (Math.abs(y) < SoccerBallPhysics.STOP_VERTICAL_SPEED) y = 0.0D;
        } else {
            x *= SoccerBallPhysics.AIR_DRAG;
            y *= SoccerBallPhysics.AIR_DRAG;
            z *= SoccerBallPhysics.AIR_DRAG;
        }
        return new Vec3(x, y, z);
    }
}

