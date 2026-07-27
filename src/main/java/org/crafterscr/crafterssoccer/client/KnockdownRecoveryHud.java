package org.crafterscr.crafterssoccer.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Barra que se llena pulsando repetidamente G.
 */
public final class KnockdownRecoveryHud {

    private static final int BAR_WIDTH = 164;
    private static final int BAR_HEIGHT = 12;

    private KnockdownRecoveryHud() {
    }

    public static void render(
            GuiGraphics graphics,
            DeltaTracker deltaTracker
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null
                || minecraft.options.hideGui
                || !ClientKnockdownState.isLocalPlayerDown()) {
            return;
        }

        int screenWidth =
                graphics.guiWidth();

        int screenHeight =
                graphics.guiHeight();

        int x =
                (screenWidth - BAR_WIDTH) / 2;

        int y =
                screenHeight - 88;

        float progress =
                ClientRecoveryState.getProgress();

        int insideWidth =
                BAR_WIDTH - 4;

        int filledWidth =
                Math.round(
                        insideWidth * progress
                );

        graphics.drawCenteredString(
                minecraft.font,
                "LEVÁNTATE — PULSA G REPETIDAMENTE",
                screenWidth / 2,
                y - 21,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                minecraft.font,
                ClientRecoveryState.getProgressPercent()
                        + "%",
                screenWidth / 2,
                y - 11,
                0xFFFFE36A
        );

        /*
         * Sombra.
         */
        graphics.fill(
                x - 2,
                y - 2,
                x + BAR_WIDTH + 2,
                y + BAR_HEIGHT + 2,
                0xAA000000
        );

        /*
         * Marco.
         */
        graphics.fill(
                x,
                y,
                x + BAR_WIDTH,
                y + BAR_HEIGHT,
                0xFFE8E8E8
        );

        /*
         * Interior vacío.
         */
        graphics.fill(
                x + 2,
                y + 2,
                x + BAR_WIDTH - 2,
                y + BAR_HEIGHT - 2,
                0xDD252525
        );

        /*
         * Progreso.
         */
        if (filledWidth > 0) {
            graphics.fill(
                    x + 2,
                    y + 2,
                    x + 2 + filledWidth,
                    y + BAR_HEIGHT - 2,
                    getProgressColor(
                            progress
                    )
            );
        }
    }

    private static int getProgressColor(
            float progress
    ) {
        if (progress < 0.40F) {
            return 0xFFE55353;
        }

        if (progress < 0.75F) {
            return 0xFFFFC83D;
        }

        return 0xFF55D978;
    }
}
