package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Indica al cliente si existe transmisión, si puede verla
 * y si actualmente está conectado a ella.
 */
public record BroadcastCameraStatePayload(
        boolean available,
        boolean allowed,
        boolean watching,
        int cameraEntityId,
        String cameraName
) implements CustomPacketPayload {

    public static final Type<BroadcastCameraStatePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "broadcast_camera_state"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            BroadcastCameraStatePayload
            > STREAM_CODEC =
            StreamCodec.of(
                    BroadcastCameraStatePayload::encode,
                    BroadcastCameraStatePayload::decode
            );

    private static void encode(
            ByteBuf buffer,
            BroadcastCameraStatePayload payload
    ) {
        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.available()
        );

        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.allowed()
        );

        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.watching()
        );

        ByteBufCodecs.VAR_INT.encode(
                buffer,
                payload.cameraEntityId()
        );

        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                payload.cameraName()
        );
    }

    private static BroadcastCameraStatePayload decode(
            ByteBuf buffer
    ) {
        return new BroadcastCameraStatePayload(
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
