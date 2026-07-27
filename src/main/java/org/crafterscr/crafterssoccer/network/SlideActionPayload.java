package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Solicitud del cliente para iniciar un barrido.
 * El servidor valida todas las condiciones.
 */
public record SlideActionPayload()
        implements CustomPacketPayload {

    public static final Type<SlideActionPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "slide_action"
                    )
            );

    public static final StreamCodec<ByteBuf, SlideActionPayload>
            STREAM_CODEC =
            StreamCodec.unit(
                    new SlideActionPayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
