package org.crafterscr.crafterssoccer.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Indicador alrededor de la cruceta cuando el jugador
 * puede patear o está cargando un tiro.
 */
public final class SoccerCrosshairGui {

    private SoccerCrosshairGui() {
    }

    public static void render(
            GuiGraphics graphics,
            DeltaTracker deltaTracker
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.options.hideGui) {
            return;
        }

        boolean lookingAtBall =
                ClientSoccerState
                        .isLookingAtKickableBall(
                                minecraft
                        );

        boolean goalkeeperIndicator =
                ClientMatchState.isGoalkeeper()
                        && ClientMatchState.isGoalkeeperInArea();

        boolean charging =
                ClientSoccerState.isCharging();

        boolean passWindow =
                ClientSoccerState.isPassWindow();

        boolean assistedPass =
                passWindow
                        && ClientSoccerState
                        .hasLikelyPassTarget(
                                minecraft
                        );

        /*
         * Solo mostrar cuando puede comenzar un tiro
         * o mientras ya está cargando.
         */
        if (!lookingAtBall
                && !charging
                && !goalkeeperIndicator) {
            return;
        }

        int centerX =
                graphics.guiWidth() / 2;

        int centerY =
                graphics.guiHeight() / 2;

        if (goalkeeperIndicator) {
            int goalkeeperColor;

            if (ClientMatchState.isGoalkeeperHoldingBall()) {
                goalkeeperColor = 0xFF55FFFF;
            } else if (ClientMatchState.isGoalkeeperAvailable()) {
                goalkeeperColor = 0xFFFFFFFF;
            } else {
                goalkeeperColor = 0xFF777777;
            }

            graphics.drawCenteredString(
                    minecraft.font,
                    "G",
                    centerX,
                    centerY + 11,
                    goalkeeperColor
            );
        }

        double lookY =
                minecraft.player
                        .getLookAngle().y;

        int color;

        /*
         * Azul/cian durante la ventana de pase. Cian significa que el
         * cliente detecta un compañero probable dentro del cono de ayuda.
         */
        if (assistedPass) {
            color = 0xFF55FFFF;
        } else if (passWindow) {
            color = 0xFF70B7FF;
        } else if (charging && lookY > 0.48D) {
            color = 0xFFFFA64D;
        } else if (charging && lookY > 0.20D) {
            color = 0xFFFFFF55;
        } else if (charging) {
            color = 0xFF47FF75;
        } else {
            color = 0xFF56D978;
        }

        if (passWindow) {
            graphics.drawCenteredString(
                    minecraft.font,
                    assistedPass
                            ? "P"
                            : "p",
                    centerX,
                    centerY + 13,
                    color
            );
        }

        int shadowColor =
                0xAA000000;

        /*
         * Marcador izquierdo.
         */
        graphics.fill(
                centerX - 11,
                centerY - 1,
                centerX - 7,
                centerY + 2,
                shadowColor
        );

        graphics.fill(
                centerX - 10,
                centerY,
                centerX - 7,
                centerY + 1,
                color
        );

        /*
         * Marcador derecho.
         */
        graphics.fill(
                centerX + 7,
                centerY - 1,
                centerX + 11,
                centerY + 2,
                shadowColor
        );

        graphics.fill(
                centerX + 7,
                centerY,
                centerX + 10,
                centerY + 1,
                color
        );

        /*
         * Marcador superior.
         */
        graphics.fill(
                centerX - 1,
                centerY - 11,
                centerX + 2,
                centerY - 7,
                shadowColor
        );

        graphics.fill(
                centerX,
                centerY - 10,
                centerX + 1,
                centerY - 7,
                color
        );

        /*
         * Marcador inferior.
         */
        graphics.fill(
                centerX - 1,
                centerY + 7,
                centerX + 2,
                centerY + 11,
                shadowColor
        );

        graphics.fill(
                centerX,
                centerY + 7,
                centerX + 1,
                centerY + 10,
                color
        );
    }
}