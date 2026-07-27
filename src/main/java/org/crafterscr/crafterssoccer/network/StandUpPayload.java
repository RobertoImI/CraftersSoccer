package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Solicitud del cliente para levantarse con G.
 */
public record StandUpPayload()
        implements CustomPacketPayload {

    public static final Type<StandUpPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "stand_up"
                    )
            );

    public static final StreamCodec<ByteBuf, StandUpPayload>
            STREAM_CODEC =
            StreamCodec.unit(
                    new StandUpPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
