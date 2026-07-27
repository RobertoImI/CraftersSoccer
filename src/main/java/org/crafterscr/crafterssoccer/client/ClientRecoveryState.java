package org.crafterscr.crafterssoccer.client;

/**
 * Progreso recibido del servidor para la barra de recuperación.
 */
public final class ClientRecoveryState {

    private static int progressPercent;

    private ClientRecoveryState() {
    }

    public static void apply(
            int progress
    ) {
        progressPercent =
                Math.max(
                        0,
                        Math.min(
                                100,
                                progress
                        )
                );
    }

    public static int getProgressPercent() {
        return progressPercent;
    }

    public static float getProgress() {
        return progressPercent / 100.0F;
    }

    public static void reset() {
        progressPercent = 0;
    }
}
