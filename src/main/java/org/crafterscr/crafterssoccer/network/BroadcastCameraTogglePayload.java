package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * El cliente solicita avanzar en la transmisión TV.
 *
 * El servidor decide si debe entrar a la primera cámara,
 * cambiar a la siguiente disponible o salir después de la última.
 */
public record BroadcastCameraTogglePayload()
        implements CustomPacketPayload {

    public static final Type<BroadcastCameraTogglePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "broadcast_camera_toggle"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            BroadcastCameraTogglePayload
            > STREAM_CODEC =
            StreamCodec.unit(
                    new BroadcastCameraTogglePayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
