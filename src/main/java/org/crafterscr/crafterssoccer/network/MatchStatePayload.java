package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sincroniza el partido con cada cliente.
 */
public record MatchStatePayload(
        boolean active,
        String fieldId,
        String redTeamName,
        String blueTeamName,
        int redScore,
        int blueScore,
        int remainingTicks,
        String state,
        String message,
        boolean participant,
        String playerTeamSide,
        boolean goalkeeper,
        boolean goalkeeperInArea,
        boolean goalkeeperAvailable,
        boolean goalkeeperHoldingBall,
        int goalkeeperCooldownTicks
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

        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                payload.redTeamName()
        );

        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                payload.blueTeamName()
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

        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.participant()
        );

        ByteBufCodecs.STRING_UTF8.encode(
                buffer,
                payload.playerTeamSide()
        );

        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.goalkeeper()
        );

        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.goalkeeperInArea()
        );

        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.goalkeeperAvailable()
        );

        ByteBufCodecs.BOOL.encode(
                buffer,
                payload.goalkeeperHoldingBall()
        );

        ByteBufCodecs.VAR_INT.encode(
                buffer,
                payload.goalkeeperCooldownTicks()
        );
    }

    private static MatchStatePayload decode(
            ByteBuf buffer
    ) {
        return new MatchStatePayload(
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer)
        );
    }

    public static MatchStatePayload inactive() {
        return new MatchStatePayload(
                false,
                "",
                "",
                "",
                0,
                0,
                0,
                "",
                "",
                false,
                "",
                false,
                false,
                false,
                false,
                0
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}