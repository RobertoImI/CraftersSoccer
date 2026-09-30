package org.crafterscr.crafterssoccer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;

/**
 * Baja la cámara local únicamente durante un derribo.
 *
 * El barrido ya no fuerza Pose.SWIMMING. Usar una pose vanilla para
 * simular una cámara baja hacía que primera persona tuviera un estado
 * local distinto al de tercera persona y podía producir correcciones
 * visuales/de movimiento. El tackle utiliza ahora efectos de cámara
 * puramente visuales.
 */
public final class KnockdownCameraController {

    private static LocalPlayer affectedPlayer;

    private static Pose previousForcedPose;

    private KnockdownCameraController() {
    }

    public static void clientTick(
            Minecraft minecraft
    ) {
        LocalPlayer player =
                minecraft.player;

        if (player == null) {
            restoreCamera();
            return;
        }

        boolean needsLowCamera =
                ClientKnockdownState.isLocalPlayerDown();

        boolean firstPerson =
                minecraft.options
                        .getCameraType()
                        .isFirstPerson();

        /*
         * La pose baja se reserva al sistema de derribo. El barrido no
         * debe modificar la pose física/local del jugador.
         */
        if (needsLowCamera && firstPerson) {
            lowerFirstPersonCamera(
                    player
            );
        } else {
            restoreCamera();
        }
    }

    private static void lowerFirstPersonCamera(
            LocalPlayer player
    ) {
        if (affectedPlayer == player
                && player.getForcedPose()
                == Pose.SWIMMING) {
            return;
        }

        /*
         * Si cambió la instancia del jugador o había otro estado
         * guardado, restaurarlo antes de aplicar la cámara baja.
         */
        restoreCamera();

        affectedPlayer =
                player;

        previousForcedPose =
                player.getForcedPose();

        /*
         * En primera persona no se renderiza normalmente el cuerpo
         * completo del jugador, por lo que podemos aprovechar la altura
         * de ojos de SWIMMING para acercar la cámara al suelo.
         */
        player.setForcedPose(
                Pose.SWIMMING
        );
    }

    public static void restoreCamera() {
        if (affectedPlayer != null) {
            /*
             * Restaurar únicamente si la pose actual todavía es la que
             * colocó este controlador. Así evitamos borrar una pose que
             * otro sistema haya aplicado después.
             */
            if (affectedPlayer.getForcedPose()
                    == Pose.SWIMMING) {

                affectedPlayer.setForcedPose(
                        previousForcedPose
                );
            }
        }

        clearStoredState();
    }

    private static void clearStoredState() {
        affectedPlayer = null;
        previousForcedPose = null;
    }
}
