package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.network.BroadcastCameraCyclePayload;
import org.crafterscr.crafterssoccer.network.BroadcastCameraTogglePayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Controles de cámara TV:
 *
 * B = entrar / salir.
 * Flecha izquierda = cámara anterior.
 * Flecha derecha = cámara siguiente.
 */
public final class BroadcastCameraKeyMappings {

    public static final KeyMapping TOGGLE_BROADCAST =
            new KeyMapping(
                    "key.crafterssoccer.broadcast_camera",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_B,
                    "key.categories.crafterssoccer"
            );

    public static final KeyMapping PREVIOUS_CAMERA =
            new KeyMapping(
                    "key.crafterssoccer.broadcast_camera_previous",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT,
                    "key.categories.crafterssoccer"
            );

    public static final KeyMapping NEXT_CAMERA =
            new KeyMapping(
                    "key.crafterssoccer.broadcast_camera_next",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT,
                    "key.categories.crafterssoccer"
            );

    private BroadcastCameraKeyMappings() {
    }

    public static void clientTick() {
        Minecraft minecraft =
                Minecraft.getInstance();

        while (TOGGLE_BROADCAST.consumeClick()) {
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

        /*
         * Las flechas solo tienen efecto mientras el jugador ya está
         * dentro de la transmisión.
         */
        while (PREVIOUS_CAMERA.consumeClick()) {
            if (minecraft.player == null
                    || !ClientBroadcastCameraState.isWatching()) {
                continue;
            }

            PacketDistributor.sendToServer(
                    new BroadcastCameraCyclePayload(
                            -1
                    )
            );
        }

        while (NEXT_CAMERA.consumeClick()) {
            if (minecraft.player == null
                    || !ClientBroadcastCameraState.isWatching()) {
                continue;
            }

            PacketDistributor.sendToServer(
                    new BroadcastCameraCyclePayload(
                            1
                    )
            );
        }
    }
}
