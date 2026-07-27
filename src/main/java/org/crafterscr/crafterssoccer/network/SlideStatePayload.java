package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sincroniza el inicio y final visual de un barrido.
 */
public record SlideStatePayload(
        int playerEntityId,
        boolean sliding
) implements CustomPacketPayload {

    public static final Type<SlideStatePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "slide_state"
                    )
            );

    public static final StreamCodec<ByteBuf, SlideStatePayload>
            STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SlideStatePayload::playerEntityId,
                    ByteBufCodecs.BOOL,
                    SlideStatePayload::sliding,
                    SlideStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
