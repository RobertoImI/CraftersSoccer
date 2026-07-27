package org.crafterscr.crafterssoccer.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD superior del partido.
 *
 * Cambios:
 * - Centra correctamente el nombre de cada equipo
 *   dentro de su cuadro.
 * - Elimina el texto "TU EQUIPO: ...".
 * - Los espectadores solo ven marcador y tiempo.
 * - Los participantes también ven mensajes del partido.
 */
public final class SoccerMatchHud {

    private static final int PANEL_WIDTH = 290;
    private static final int PANEL_HEIGHT = 34;
    private static final int TOP_BAR_HEIGHT = 19;
    private static final int SCORE_BOX_HALF_WIDTH = 42;

    private SoccerMatchHud() {
    }

    public static void render(
            GuiGraphics graphics,
            DeltaTracker deltaTracker
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (!ClientMatchState.isActive()
                || minecraft.player == null
                || minecraft.options.hideGui) {
            return;
        }

        int centerX =
                graphics.guiWidth() / 2;

        int x =
                centerX - PANEL_WIDTH / 2;

        int y = 8;

        int leftBoxStart = x;
        int leftBoxEnd = centerX - SCORE_BOX_HALF_WIDTH;

        int rightBoxStart = centerX + SCORE_BOX_HALF_WIDTH;
        int rightBoxEnd = x + PANEL_WIDTH;

        graphics.fill(
                x - 1,
                y - 1,
                x + PANEL_WIDTH + 1,
                y + PANEL_HEIGHT + 1,
                0xAA000000
        );

        graphics.fill(
                x,
                y,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                0xDD171717
        );

        graphics.fill(
                leftBoxStart,
                y,
                leftBoxEnd,
                y + TOP_BAR_HEIGHT,
                0xCC9F2222
        );

        graphics.fill(
                rightBoxStart,
                y,
                rightBoxEnd,
                y + TOP_BAR_HEIGHT,
                0xCC2453A6
        );

        Font font =
                minecraft.font;

        String redName =
                fitTextCentered(
                        font,
                        ClientMatchState.getRedTeamName(),
                        Math.max(
                                10,
                                leftBoxEnd - leftBoxStart - 10
                        )
                );

        String blueName =
                fitTextCentered(
                        font,
                        ClientMatchState.getBlueTeamName(),
                        Math.max(
                                10,
                                rightBoxEnd - rightBoxStart - 10
                        )
                );

        graphics.drawCenteredString(
                font,
                redName,
                (leftBoxStart + leftBoxEnd) / 2,
                y + 6,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                font,
                blueName,
                (rightBoxStart + rightBoxEnd) / 2,
                y + 6,
                0xFFFFFFFF
        );

        String score =
                ClientMatchState.getRedScore()
                        + "  -  "
                        + ClientMatchState.getBlueScore();

        graphics.drawCenteredString(
                font,
                score,
                centerX,
                y + 6,
                0xFFFFFFFF
        );

        String displayedTime =
                formatTime(
                        ClientMatchState.getRemainingTicks()
                );

        if (ClientMatchState.getAddedTimeMinutes() > 0) {
            displayedTime +=
                    "  §e+"
                            + ClientMatchState
                            .getAddedTimeMinutes();
        }

        graphics.drawCenteredString(
                font,
                displayedTime,
                centerX,
                y + 21,
                0xFFEFEFEF
        );

        /*
         * Los espectadores solamente ven marcador y tiempo.
         */
        if (!ClientMatchState.isParticipant()) {
            return;
        }

        String message =
                ClientMatchState.getMessage();

        if (message != null
                && !message.isBlank()) {

            graphics.drawCenteredString(
                    font,
                    message,
                    centerX,
                    y + PANEL_HEIGHT + 5,
                    getMessageColor(
                            ClientMatchState.getState()
                    )
            );
        }
    }

    private static String fitTextCentered(
            Font font,
            String text,
            int maxWidth
    ) {
        if (text == null || text.isBlank()) {
            return "";
        }

        if (font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth =
                font.width(ellipsis);

        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            String next =
                    builder.toString()
                            + text.charAt(i);

            if (font.width(next) + ellipsisWidth
                    > maxWidth) {
                break;
            }

            builder.append(
                    text.charAt(i)
            );
        }

        return builder + ellipsis;
    }

    private static String formatTime(
            int ticks
    ) {
        int totalSeconds =
                Math.max(
                        0,
                        ticks / 20
                );

        int minutes =
                totalSeconds / 60;

        int seconds =
                totalSeconds % 60;

        return String.format(
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    private static int getMessageColor(
            String state
    ) {
        if ("GOAL_PAUSE".equals(state)) {
            return 0xFFFFD84D;
        }

        if ("PAUSED".equals(state)) {
            return 0xFFFFA64D;
        }

        if ("FINISHED".equals(state)) {
            return 0xFFFFFFFF;
        }

        return 0xFF55FF77;
    }
}