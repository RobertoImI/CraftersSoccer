package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Paquete enviado desde el cliente al servidor
 * cuando se suelta el botón de tiro.
 */
public record KickBallPayload(
        int entityId,
        float charge,
        boolean passRequested
) implements CustomPacketPayload {

    public static final Type<KickBallPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "kick_ball"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            KickBallPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    KickBallPayload::entityId,

                    ByteBufCodecs.FLOAT,
                    KickBallPayload::charge,

                    ByteBufCodecs.BOOL,
                    KickBallPayload::passRequested,

                    KickBallPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}