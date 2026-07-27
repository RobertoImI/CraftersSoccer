package org.crafterscr.crafterssoccer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;

/**
 * Baja la cámara local durante el derribo o la fase inicial del barrido.
 *
 * IMPORTANTE:
 * La pose SWIMMING se aplica únicamente en primera persona.
 *
 * Antes se aplicaba también en tercera persona. Eso hacía que el modelo
 * del jugador local combinara:
 *
 * - la pose vanilla de natación;
 * - la animación de PlayerAnimator.
 *
 * Por eso el propio jugador se veía deformado, aunque los demás clientes
 * vieran correctamente la animación.
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
                ClientKnockdownState.isLocalPlayerDown()
                        || ClientSlideState
                        .shouldKeepFirstPersonCameraLow();

        boolean firstPerson =
                minecraft.options
                        .getCameraType()
                        .isFirstPerson();

        /*
         * La pose baja solamente se usa cuando el jugador realmente
         * está viendo desde primera persona.
         *
         * En tercera persona el modelo debe quedar completamente libre
         * para que PlayerAnimator reproduzca slide_soccer sin mezclarse
         * con la pose vanilla de natación.
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
