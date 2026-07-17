package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Solicitud del cliente para hacer sonar el silbato del árbitro.
 *
 * No contiene datos porque el servidor obtiene y valida al jugador
 * directamente desde el contexto de red.
 */
public record RefereeWhistlePayload()
        implements CustomPacketPayload {

    public static final Type<RefereeWhistlePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "referee_whistle"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            RefereeWhistlePayload
            > STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        // Paquete sin contenido.
                    },
                    buffer ->
                            new RefereeWhistlePayload()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
