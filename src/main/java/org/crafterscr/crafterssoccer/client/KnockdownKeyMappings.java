package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.network.StandUpPayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Tecla G para levantarse.
 */
public final class KnockdownKeyMappings {

    public static final KeyMapping STAND_UP =
            new KeyMapping(
                    "key.crafterssoccer.stand_up",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    "key.categories.crafterssoccer"
            );

    private static boolean standUpWasDown;

    private KnockdownKeyMappings() {
    }

    public static void clientTick() {
        boolean standUpDown =
                STAND_UP.isDown();

        /*
         * Solo cuenta el flanco de pulsación:
         * suelta -> presionada.
         *
         * Mantener G no genera taps adicionales. Para avanzar la barra
         * el jugador debe soltar y volver a pulsar la tecla.
         */
        if (standUpDown
                && !standUpWasDown
                && ClientKnockdownState.isLocalPlayerDown()) {

            PacketDistributor.sendToServer(
                    new StandUpPayload()
            );
        }

        standUpWasDown =
                standUpDown;
    }
}
