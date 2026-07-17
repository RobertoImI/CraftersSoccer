package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Acción de clic derecho del árbitro sobre el balón.
 *
 * holding = true:
 * intenta agarrar el balón indicado.
 *
 * holding = false:
 * intenta soltar el balón indicado.
 */
public record RefereeBallActionPayload(
        int ballEntityId,
        boolean holding
) implements CustomPacketPayload {

    public static final Type<RefereeBallActionPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "referee_ball_action"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            RefereeBallActionPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RefereeBallActionPayload::ballEntityId,
                    ByteBufCodecs.BOOL,
                    RefereeBallActionPayload::holding,
                    RefereeBallActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
