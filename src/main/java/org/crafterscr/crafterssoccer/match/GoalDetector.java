package org.crafterscr.crafterssoccer.match;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Deterministic swept-segment goal detection. */
public final class GoalDetector {
    private GoalDetector() { }

    public static boolean crossed(AABB goal, Vec3 previous, Vec3 current) {
        if (goal == null || previous == null || current == null) return false;
        if (goal.contains(previous) || goal.contains(current)) return true;

        Vec3 movement = current.subtract(previous);
        double[] interval = {0.0D, 1.0D};
        return clip(previous.x, movement.x, goal.minX, goal.maxX, interval)
                && clip(previous.y, movement.y, goal.minY, goal.maxY, interval)
                && clip(previous.z, movement.z, goal.minZ, goal.maxZ, interval);
    }

    private static boolean clip(double origin, double delta, double min, double max,
                                double[] interval) {
        if (Math.abs(delta) < 1.0E-9D) return origin >= min && origin <= max;
        double near = (min - origin) / delta;
        double far = (max - origin) / delta;
        if (near > far) { double swap = near; near = far; far = swap; }
        interval[0] = Math.max(interval[0], near);
        interval[1] = Math.min(interval[1], far);
        return interval[0] <= interval[1];
    }
}
