package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sincroniza el estado derribado de un jugador.
 */
public record KnockdownStatePayload(
        int playerEntityId,
        boolean knockedDown
) implements CustomPacketPayload {

    public static final Type<KnockdownStatePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "knockdown_state"
                    )
            );

    public static final StreamCodec<ByteBuf, KnockdownStatePayload>
            STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    KnockdownStatePayload::playerEntityId,
                    ByteBufCodecs.BOOL,
                    KnockdownStatePayload::knockedDown,
                    KnockdownStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
