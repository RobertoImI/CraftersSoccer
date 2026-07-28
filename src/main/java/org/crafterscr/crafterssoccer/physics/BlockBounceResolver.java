package org.crafterscr.crafterssoccer.physics;

import net.minecraft.world.phys.Vec3;

/** Resolves block bounces from the requested and actual movement of an entity. */
public final class BlockBounceResolver {
    private static final double BLOCKED_MOVEMENT_RATIO = 0.35D;

    private BlockBounceResolver() { }

    public static Vec3 resolve(Vec3 requestedMovement, Vec3 actualMovement,
                               Vec3 velocity, boolean verticalCollision) {
        double x = velocity.x;
        double y = velocity.y;
        double z = velocity.z;

        if (isBlocked(requestedMovement.x, actualMovement.x)) {
            x = -velocity.x * SoccerBallPhysics.WALL_BOUNCE;
        }
        if (isBlocked(requestedMovement.z, actualMovement.z)) {
            z = -velocity.z * SoccerBallPhysics.WALL_BOUNCE;
        }
        if (verticalCollision) {
            if (velocity.y < -SoccerBallPhysics.STOP_VERTICAL_SPEED) {
                y = -velocity.y * SoccerBallPhysics.FLOOR_BOUNCE;
            } else if (velocity.y > SoccerBallPhysics.STOP_VERTICAL_SPEED) {
                y = -velocity.y * SoccerBallPhysics.WALL_BOUNCE;
            } else {
                y = 0.0D;
            }
        }
        return new Vec3(x, y, z);
    }

    private static boolean isBlocked(double requested, double actual) {
        return Math.abs(requested) > 0.001D
                && Math.abs(actual) < Math.abs(requested) * BLOCKED_MOVEMENT_RATIO;
    }
}
