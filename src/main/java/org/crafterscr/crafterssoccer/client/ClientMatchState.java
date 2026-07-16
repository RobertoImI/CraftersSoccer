package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.network.MatchStatePayload;

/**
 * Estado local utilizado por el HUD.
 */
public final class ClientMatchState {

    private static boolean active;

    private static String fieldId = "";
    private static String state = "";
    private static String message = "";

    private static String redTeamName =
            "Equipo Rojo";

    private static String blueTeamName =
            "Equipo Azul";

    private static int redScore;
    private static int blueScore;
    private static int remainingTicks;

    private static boolean participant;
    private static String playerTeamSide = "";

    private ClientMatchState() {
    }

    public static void apply(
            MatchStatePayload payload
    ) {
        if (!payload.active()) {
            reset();
            return;
        }

        active = true;

        fieldId =
                payload.fieldId();

        redTeamName =
                payload.redTeamName();

        blueTeamName =
                payload.blueTeamName();

        redScore =
                payload.redScore();

        blueScore =
                payload.blueScore();

        remainingTicks =
                payload.remainingTicks();

        state =
                payload.state();

        message =
                payload.message();

        participant =
                payload.participant();

        playerTeamSide =
                payload.playerTeamSide();
    }

    public static void reset() {
        active = false;

        fieldId = "";
        state = "";
        message = "";

        redTeamName =
                "Equipo Rojo";

        blueTeamName =
                "Equipo Azul";

        redScore = 0;
        blueScore = 0;
        remainingTicks = 0;

        participant = false;
        playerTeamSide = "";
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

    public static String getRedTeamName() {
        return redTeamName;
    }

    public static String getBlueTeamName() {
        return blueTeamName;
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

    public static boolean isParticipant() {
        return participant;
    }

    public static String getPlayerTeamSide() {
        return playerTeamSide;
    }

    public static String getLocalTeamName() {
        if ("RED".equals(playerTeamSide)) {
            return redTeamName;
        }

        if ("BLUE".equals(playerTeamSide)) {
            return blueTeamName;
        }

        return "";
    }
}