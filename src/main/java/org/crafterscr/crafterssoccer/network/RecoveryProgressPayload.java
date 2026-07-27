package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Progreso autoritativo de la barra para levantarse.
 *
 * Solamente se envía al jugador que está derribado.
 */
public record RecoveryProgressPayload(
        int progressPercent
) implements CustomPacketPayload {

    public static final Type<RecoveryProgressPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "recovery_progress"
                    )
            );

    public static final StreamCodec<ByteBuf, RecoveryProgressPayload>
            STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RecoveryProgressPayload::progressPercent,
                    RecoveryProgressPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
