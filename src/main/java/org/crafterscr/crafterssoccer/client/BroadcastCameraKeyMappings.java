package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.network.BroadcastCameraTogglePayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Tecla B para entrar o salir de la cámara TV.
 */
public final class BroadcastCameraKeyMappings {

    public static final KeyMapping TOGGLE_BROADCAST =
            new KeyMapping(
                    "key.crafterssoccer.broadcast_camera",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_B,
                    "key.categories.crafterssoccer"
            );

    private BroadcastCameraKeyMappings() {
    }

    public static void clientTick() {
        while (TOGGLE_BROADCAST.consumeClick()) {
            Minecraft minecraft =
                    Minecraft.getInstance();

            if (minecraft.player == null) {
                continue;
            }

            if (!ClientBroadcastCameraState.isWatching()
                    && (!ClientBroadcastCameraState.isAvailable()
                    || !ClientBroadcastCameraState.isAllowed())) {

                minecraft.player.displayClientMessage(
                        Component.literal(
                                "§cLa cámara TV no está disponible "
                                        + "para este jugador."
                        ),
                        false
                );

                continue;
            }

            PacketDistributor.sendToServer(
                    new BroadcastCameraTogglePayload()
            );
        }
    }
}
