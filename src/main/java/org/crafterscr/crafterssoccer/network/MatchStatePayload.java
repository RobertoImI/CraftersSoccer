package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sincroniza el estado del partido con los clientes.
 */
public record MatchStatePayload(
        boolean active,
        String fieldId,
        int redScore,
        int blueScore,
        int remainingTicks,
        String state,
        String message
) implements CustomPacketPayload {

    public static final Type<MatchStatePayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "match_state"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            MatchStatePayload
            > STREAM_CODEC =
            StreamCodec.of(
                    MatchStatePayload::encode,
                    MatchStatePayload::decode
            );

    private static void encode(
            ByteBuf buffer,
            MatchStatePayload payload
    ) {
        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.active()
        );

        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                payload.fieldId()
        );

        ByteBufCodecs.VAR_INT.encode(
                buffer,
                payload.redScore()
        );

        ByteBufCodecs.VAR_INT.encode(
                buffer,
                payload.blueScore()
        );

        ByteBufCodecs.VAR_INT.encode(
                buffer,
                payload.remainingTicks()
        );

        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                payload.state()
        );

        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                payload.message()
        );
    }

    private static MatchStatePayload decode(
            ByteBuf buffer
    ) {
        return new MatchStatePayload(
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer)
        );
    }

    public static MatchStatePayload inactive() {
        return new MatchStatePayload(
                false,
                "",
                0,
                0,
                0,
                "",
                ""
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}