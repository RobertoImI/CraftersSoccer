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

    private KnockdownKeyMappings() {
    }

    public static void clientTick() {
        while (STAND_UP.consumeClick()) {
            if (!ClientKnockdownState.isLocalPlayerDown()) {
                continue;
            }

            PacketDistributor.sendToServer(
                    new StandUpPayload()
            );
        }
    }
}
