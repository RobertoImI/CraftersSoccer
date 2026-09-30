package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Refuerza la sensación del barrido en primera persona sin alterar la
 * pose ni las dimensiones físicas del jugador.
 *
 * El efecto es deliberadamente discreto: la cámara acompaña el tackle
 * pero no debe hacer que primera persona se sienta más lenta o torpe.
 */
@EventBusSubscriber(
        modid = CraftersSoccer.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class SlideCameraEffects {

    private SlideCameraEffects() {
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(
            ViewportEvent.ComputeCameraAngles event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || !minecraft.options
                .getCameraType()
                .isFirstPerson()
                || !ClientSlideState
                .isLocalPlayerSliding()) {
            return;
        }

        float progress =
                ClientSlideState.getLocalCameraProgress();

        /*
         * Entrada rápida, contacto corto y recuperación suave.
         * No se fuerza Pose.SWIMMING: este efecto es 100 % visual.
         */
        float strength;

        if (progress < 0.25F) {
            float normalized =
                    progress / 0.25F;

            strength =
                    1.0F
                            - (1.0F - normalized)
                            * (1.0F - normalized);

        } else if (progress < 0.58F) {
            strength = 1.0F;

        } else {
            float normalized =
                    (progress - 0.58F)
                            / 0.42F;

            strength =
                    Math.max(
                            0.0F,
                            1.0F
                                    - normalized
                                    * normalized
                    );
        }

        event.setRoll(
                event.getRoll()
                        + 2.8F * strength
        );

        event.setPitch(
                event.getPitch()
                        + 0.7F * strength
        );
    }
}
