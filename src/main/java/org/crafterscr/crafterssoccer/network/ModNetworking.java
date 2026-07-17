package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.client.ClientMatchState;
import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.match.GoalkeeperManager;
import org.crafterscr.crafterssoccer.referee.RefereeManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registro de paquetes.
 */
public final class ModNetworking {

    private static final String NETWORK_VERSION = "6";

    private ModNetworking() {
    }

    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        PayloadRegistrar registrar =
                event.registrar(
                        NETWORK_VERSION
                );

        registrar.playToServer(
                KickBallPayload.TYPE,
                KickBallPayload.STREAM_CODEC,
                ModNetworking::handleKickBall
        );

        registrar.playToServer(
                GoalkeeperActionPayload.TYPE,
                GoalkeeperActionPayload.STREAM_CODEC,
                ModNetworking::handleGoalkeeperAction
        );

        registrar.playToServer(
                RefereeWhistlePayload.TYPE,
                RefereeWhistlePayload.STREAM_CODEC,
                ModNetworking::handleRefereeWhistle
        );

        registrar.playToServer(
                RefereeBallActionPayload.TYPE,
                RefereeBallActionPayload.STREAM_CODEC,
                ModNetworking::handleRefereeBallAction
        );

        registrar.playToClient(
                MatchStatePayload.TYPE,
                MatchStatePayload.STREAM_CODEC,
                ModNetworking::handleMatchState
        );
    }

    private static void handleKickBall(
            KickBallPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player()
                instanceof ServerPlayer player)) {
            return;
        }

        Entity entity =
                player.level().getEntity(
                        payload.entityId()
                );

        if (!(entity
                instanceof SoccerBallEntity ball)) {
            return;
        }

        ball.kick(
                player,
                payload.charge()
        );
    }

    private static void handleGoalkeeperAction(
            GoalkeeperActionPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player()
                instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(
                () -> GoalkeeperManager.handleAction(
                        player.getServer(),
                        player,
                        payload.ballEntityId()
                )
        );
    }

    private static void handleRefereeWhistle(
            RefereeWhistlePayload payload,
            IPayloadContext context
    ) {
        if (!(context.player()
                instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(
                () -> RefereeManager.handleWhistle(
                        player.getServer(),
                        player
                )
        );
    }

    private static void handleRefereeBallAction(
            RefereeBallActionPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player()
                instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(
                () -> RefereeManager.handleBallAction(
                        player.getServer(),
                        player,
                        payload.ballEntityId(),
                        payload.holding()
                )
        );
    }

    private static void handleMatchState(
            MatchStatePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () -> ClientMatchState.apply(
                        payload
                )
        );
    }
}