package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;
import org.crafterscr.crafterssoccer.network.RefereeWhistlePayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Controles exclusivos del árbitro.
 */
public final class RefereeKeyMappings {

    public static final KeyMapping WHISTLE =
            new KeyMapping(
                    "key.crafterssoccer.referee_whistle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_R,
                    "key.categories.crafterssoccer"
            );

    private RefereeKeyMappings() {
    }

    /**
     * Consume una pulsación por vez.
     * Mantener R no genera paquetes continuamente.
     */
    public static void clientTick() {
        while (WHISTLE.consumeClick()) {
            PacketDistributor.sendToServer(
                    new RefereeWhistlePayload()
            );
        }
    }
}
