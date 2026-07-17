package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Solicitud del portero al pulsar clic derecho.
 * El servidor vuelve a validar absolutamente todo.
 */
public record GoalkeeperActionPayload(
        int ballEntityId
) implements CustomPacketPayload {

    public static final Type<GoalkeeperActionPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "goalkeeper_action"
                    )
            );

    public static final StreamCodec<ByteBuf, GoalkeeperActionPayload>
            STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    GoalkeeperActionPayload::ballEntityId,
                    GoalkeeperActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
