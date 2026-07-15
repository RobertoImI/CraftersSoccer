package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.network.MatchStatePayload;

/**
 * Estado local del partido utilizado por el HUD.
 */
public final class ClientMatchState {

    private static boolean active;

    private static String fieldId = "";
    private static String state = "";
    private static String message = "";

    private static int redScore;
    private static int blueScore;
    private static int remainingTicks;

    private ClientMatchState() {
    }

    public static void apply(
            MatchStatePayload payload
    ) {
        active = payload.active();
        fieldId = payload.fieldId();

        redScore = payload.redScore();
        blueScore = payload.blueScore();

        remainingTicks =
                payload.remainingTicks();

        state = payload.state();
        message = payload.message();

        if (!active) {
            reset();
        }
    }

    public static void reset() {
        active = false;

        fieldId = "";
        state = "";
        message = "";

        redScore = 0;
        blueScore = 0;
        remainingTicks = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static String getFieldId() {
        return fieldId;
    }

    public static String getState() {
        return state;
    }

    public static String getMessage() {
        return message;
    }

    public static int getRedScore() {
        return redScore;
    }

    public static int getBlueScore() {
        return blueScore;
    }

    public static int getRemainingTicks() {
        return remainingTicks;
    }
}