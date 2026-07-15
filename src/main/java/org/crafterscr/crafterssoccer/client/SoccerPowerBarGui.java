package org.crafterscr.crafterssoccer.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Barra visual de potencia.
 */
public final class SoccerPowerBarGui {

    private static final int BAR_WIDTH = 122;
    private static final int BAR_HEIGHT = 10;

    private SoccerPowerBarGui() {
    }

    public static void render(
            GuiGraphics graphics,
            DeltaTracker deltaTracker
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (!ClientSoccerState.isCharging()) {
            return;
        }

        if (minecraft.options.hideGui
                || minecraft.player == null) {
            return;
        }

        int screenWidth =
                graphics.guiWidth();

        int screenHeight =
                graphics.guiHeight();

        int x =
                (screenWidth - BAR_WIDTH) / 2;

        int y =
                screenHeight - 65;

        float charge =
                ClientSoccerState.getCharge();

        int internalWidth =
                BAR_WIDTH - 4;

        int filledWidth =
                Math.round(
                        internalWidth * charge
                );

        /*
         * Sombra.
         */
        graphics.fill(
                x - 1,
                y - 1,
                x + BAR_WIDTH + 1,
                y + BAR_HEIGHT + 1,
                0xAA000000
        );

        /*
         * Fondo.
         */
        graphics.fill(
                x,
                y,
                x + BAR_WIDTH,
                y + BAR_HEIGHT,
                0xDD151515
        );

        /*
         * Borde.
         */
        graphics.fill(
                x + 1,
                y + 1,
                x + BAR_WIDTH - 1,
                y + BAR_HEIGHT - 1,
                0xFFEEEEEE
        );

        /*
         * Interior vacío.
         */
        graphics.fill(
                x + 2,
                y + 2,
                x + BAR_WIDTH - 2,
                y + BAR_HEIGHT - 2,
                0xFF333333
        );

        int powerColor =
                getPowerColor(charge);

        graphics.fill(
                x + 2,
                y + 2,
                x + 2 + filledWidth,
                y + BAR_HEIGHT - 2,
                powerColor
        );

        String percentageText =
                "POTENCIA "
                        + Math.round(charge * 100.0F)
                        + "%";

        graphics.drawCenteredString(
                minecraft.font,
                percentageText,
                screenWidth / 2,
                y - 20,
                0xFFFFFFFF
        );

        /*
         * Mostrar el tipo aproximado de tiro según
         * la inclinación de la cámara.
         */
        String shotType =
                getShotType(
                        minecraft.player
                                .getLookAngle().y
                );

        graphics.drawCenteredString(
                minecraft.font,
                shotType,
                screenWidth / 2,
                y - 10,
                getShotTypeColor(
                        minecraft.player
                                .getLookAngle().y
                )
        );
    }

    private static int getPowerColor(
            float charge
    ) {
        if (charge < 0.55F) {
            return 0xFF39D353;
        }

        if (charge < 0.85F) {
            return 0xFFFFC928;
        }

        return 0xFFFF4545;
    }

    private static String getShotType(
            double lookY
    ) {
        if (lookY > 0.48D) {
            return "GLOBO ALTO";
        }

        if (lookY > 0.20D) {
            return "TIRO ELEVADO";
        }

        return "TIRO RASO";
    }

    private static int getShotTypeColor(
            double lookY
    ) {
        if (lookY > 0.48D) {
            return 0xFFFFA64D;
        }

        if (lookY > 0.20D) {
            return 0xFFFFFF55;
        }

        return 0xFF55FF77;
    }
}