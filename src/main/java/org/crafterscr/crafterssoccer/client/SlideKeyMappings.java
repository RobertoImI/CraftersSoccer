package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.network.SlideActionPayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Tecla V para ejecutar el barrido.
 */
public final class SlideKeyMappings {

    public static final KeyMapping SLIDE =
            new KeyMapping(
                    "key.crafterssoccer.slide",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.crafterssoccer"
            );

    private SlideKeyMappings() {
    }

    public static void clientTick() {
        while (SLIDE.consumeClick()) {
            if (ClientKnockdownState.isLocalPlayerDown()
                    || ClientSlideState.isLocalPlayerSliding()
                    || ClientSlideState.hasLocalCooldown()) {
                continue;
            }

            PacketDistributor.sendToServer(
                    new SlideActionPayload()
            );

            /*
             * Prevención visual inmediata de spam.
             * El servidor sigue siendo la autoridad.
             */
            ClientSlideState.startLocalCooldown();
        }
    }
}
