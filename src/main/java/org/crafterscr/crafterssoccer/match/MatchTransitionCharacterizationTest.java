package org.crafterscr.crafterssoccer.match;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

class MatchTransitionCharacterizationTest {
    @Test void countdownMessagesKeepExistingTickBoundaries() {
        assertEquals(3, MatchClock.countdownSeconds(60));
        assertEquals(2, MatchClock.countdownSeconds(40));
        assertEquals(1, MatchClock.countdownSeconds(20));
        assertEquals(1, MatchClock.countdownSeconds(0));
    }

    @Test void clockNeverBecomesNegative() {
        assertEquals(4, MatchClock.tickRemaining(5));
        assertEquals(0, MatchClock.tickRemaining(0));
    }

    @Test void sweptBallPathDetectsGoalWithoutEndingInsideIt() {
        AABB goal = new AABB(2, 0, 0, 3, 2, 2);
        assertTrue(GoalDetector.crossed(goal, new Vec3(1, 1, 1), new Vec3(4, 1, 1)));
        assertFalse(GoalDetector.crossed(goal, new Vec3(1, 3, 1), new Vec3(4, 3, 1)));
    }
}
