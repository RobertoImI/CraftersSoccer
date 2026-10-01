package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Solicita cambiar de cámara TV mientras el espectador ya está viendo
 * la transmisión.
 *
 * direction:
 * -1 = cámara anterior
 *  1 = cámara siguiente
 */
public record BroadcastCameraCyclePayload(
        int direction
) implements CustomPacketPayload {

    public static final Type<BroadcastCameraCyclePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "broadcast_camera_cycle"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            BroadcastCameraCyclePayload
            > STREAM_CODEC =
            StreamCodec.of(
                    BroadcastCameraCyclePayload::encode,
                    BroadcastCameraCyclePayload::decode
            );

    private static void encode(
            ByteBuf buffer,
            BroadcastCameraCyclePayload payload
    ) {
        ByteBufCodecs.VAR_INT.encode(
                buffer,
                payload.direction()
        );
    }

    private static BroadcastCameraCyclePayload decode(
            ByteBuf buffer
    ) {
        return new BroadcastCameraCyclePayload(
                ByteBufCodecs.VAR_INT.decode(
                        buffer
                )
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
