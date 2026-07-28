package org.crafterscr.crafterssoccer.match;

/** Pure clock calculations used by the match aggregate. */
public final class MatchClock {
    private MatchClock() { }

    public static int tickRemaining(int remainingTicks) {
        return Math.max(0, remainingTicks - 1);
    }

    public static int countdownSeconds(int stateTicks) {
        return Math.max(1, (stateTicks + 19) / 20);
    }

    public static int addedTimeTicks(int minutes) {
        return Math.max(0, minutes) * 60 * 20;
    }
}
