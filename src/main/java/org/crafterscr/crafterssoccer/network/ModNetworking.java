package org.crafterscr.crafterssoccer.network;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registro de paquetes del mod.
 */
public final class ModNetworking {

    private static final String NETWORK_VERSION = "1";

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
    }

    /**
     * Procesar el tiro en el servidor.
     */
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

        /*
         * El servidor vuelve a validar:
         * - La existencia del balón.
         * - La distancia.
         * - La potencia.
         */
        ball.kick(
                player,
                payload.charge()
        );
    }
}