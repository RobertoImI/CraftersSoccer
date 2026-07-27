package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Refuerza la sensación del barrido en primera persona.
 *
 * La altura sigue siendo controlada por KnockdownCameraController.
 * Aquí solamente añadimos una inclinación lateral y un pequeño
 * movimiento de cabeza hacia delante.
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
         * 0-2 ticks: entrada muy rápida.
         * 3-7 ticks: permanencia corta.
         * 8-14 ticks: recuperación rápida.
         */
        float strength;

        if (progress < 0.20F) {
            float normalized =
                    progress / 0.20F;

            strength =
                    1.0F
                            - (1.0F - normalized)
                            * (1.0F - normalized);

        } else if (progress < 0.53F) {
            strength = 1.0F;

        } else {
            float normalized =
                    (progress - 0.53F)
                            / 0.47F;

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
                        + 4.5F * strength
        );

        event.setPitch(
                event.getPitch()
                        + 1.5F * strength
        );
    }
}
