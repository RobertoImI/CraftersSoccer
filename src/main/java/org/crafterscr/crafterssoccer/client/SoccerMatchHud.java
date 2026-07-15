package org.crafterscr.crafterssoccer.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Marcador superior del partido.
 */
public final class SoccerMatchHud {

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

        int panelWidth = 184;
        int panelHeight = 34;

        int x =
                centerX - panelWidth / 2;

        int y = 8;

        graphics.fill(
                x - 1,
                y - 1,
                x + panelWidth + 1,
                y + panelHeight + 1,
                0xAA000000
        );

        graphics.fill(
                x,
                y,
                x + panelWidth,
                y + panelHeight,
                0xD91A1A1A
        );

        graphics.fill(
                x,
                y,
                centerX - 32,
                y + 19,
                0xAAAD2525
        );

        graphics.fill(
                centerX + 32,
                y,
                x + panelWidth,
                y + 19,
                0xAA2859B8
        );

        graphics.drawCenteredString(
                minecraft.font,
                "ROJO",
                x + 39,
                y + 6,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                minecraft.font,
                "AZUL",
                x + panelWidth - 39,
                y + 6,
                0xFFFFFFFF
        );

        String score =
                ClientMatchState.getRedScore()
                        + "  -  "
                        + ClientMatchState.getBlueScore();

        graphics.drawCenteredString(
                minecraft.font,
                score,
                centerX,
                y + 6,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                minecraft.font,
                formatTime(
                        ClientMatchState
                                .getRemainingTicks()
                ),
                centerX,
                y + 21,
                0xFFEFEFEF
        );

        String message =
                ClientMatchState.getMessage();

        if (message != null
                && !message.isBlank()) {

            graphics.drawCenteredString(
                    minecraft.font,
                    message,
                    centerX,
                    y + panelHeight + 5,
                    getMessageColor(
                            ClientMatchState.getState()
                    )
            );
        }
    }

    private static String formatTime(
            int ticks
    ) {
        int totalSeconds =
                Math.max(0, ticks / 20);

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