package org.crafterscr.crafterssoccer.referee;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.match.SoccerMatchManager;
import org.crafterscr.crafterssoccer.registry.ModSounds;

import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;

/**
 * Acciones autoritativas del árbitro.
 *
 * En este bloque solamente controla el silbato manual.
 * El sonido no pausa, reanuda ni modifica el partido.
 */
public final class RefereeManager {

    /**
     * 30 ticks = 1.5 segundos.
     */
    private static final int WHISTLE_COOLDOWN_TICKS = 30;

    private static final Map<UUID, Long> WHISTLE_COOLDOWNS =
            new HashMap<>();

    private RefereeManager() {
    }

    /**
     * Solicitud enviada al pulsar R.
     *
     * El servidor valida el rol y el cooldown.
     */
    public static void handleWhistle(
            MinecraftServer server,
            ServerPlayer player
    ) {
        if (server == null || player == null) {
            return;
        }

        if (!SoccerMatchManager.isReferee(
                server,
                player.getUUID()
        )) {
            return;
        }

        long gameTime =
                player.level().getGameTime();

        long cooldownUntil =
                WHISTLE_COOLDOWNS.getOrDefault(
                        player.getUUID(),
                        0L
                );

        if (gameTime < cooldownUntil) {
            return;
        }

        WHISTLE_COOLDOWNS.put(
                player.getUUID(),
                gameTime + WHISTLE_COOLDOWN_TICKS
        );

        playWhistleForEveryone(server);
    }

    /**
     * Envía el sonido desde la posición de cada jugador.
     * Así todos lo escuchan claramente sin depender de la distancia.
     */
    private static void playWhistleForEveryone(
            MinecraftServer server
    ) {
        for (ServerPlayer listener
                : server.getPlayerList().getPlayers()) {

            listener.connection.send(
                    new ClientboundSoundPacket(
                            ModSounds.WHISTLE_START,
                            SoundSource.PLAYERS,
                            listener.getX(),
                            listener.getY(),
                            listener.getZ(),
                            0.65F,
                            1.0F,
                            listener.getRandom().nextLong()
                    )
            );
        }
    }

    /**
     * Distancia máxima para agarrar el balón.
     */
    private static final double MAX_BALL_GRAB_DISTANCE = 3.25D;

    /**
     * El árbitro puede agarrar cualquier balón del mod,
     * incluso cuando no existe un partido activo.
     */
    public static void handleBallAction(
            MinecraftServer server,
            ServerPlayer player,
            int requestedBallEntityId,
            boolean holding
    ) {
        if (server == null || player == null) {
            return;
        }

        if (!SoccerMatchManager.isReferee(
                server,
                player.getUUID()
        )) {
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Entity entity =
                level.getEntity(requestedBallEntityId);

        if (!(entity instanceof SoccerBallEntity ball)) {
            return;
        }

        if (!holding) {
            if (ball.isHeldByReferee(player.getUUID())) {
                ball.releaseFromReferee(player);
            }
            return;
        }

        /*
         * No puede tomar un balón que ya esté en posesión
         * del portero o de otro árbitro.
         */
        if (ball.isHeldByGoalkeeper()
                || ball.isHeldByReferee()) {
            return;
        }

        double maximumDistanceSquared =
                MAX_BALL_GRAB_DISTANCE
                        * MAX_BALL_GRAB_DISTANCE;

        if (player.distanceToSqr(ball)
                > maximumDistanceSquared) {
            return;
        }

        Vec3 eyeToBall =
                ball.position()
                        .add(
                                0.0D,
                                SoccerBallEntity.BALL_RADIUS,
                                0.0D
                        )
                        .subtract(
                                player.getEyePosition()
                        );

        if (eyeToBall.lengthSqr() < 0.0001D) {
            return;
        }

        Vec3 look =
                player.getLookAngle();

        if (look.lengthSqr() < 0.0001D) {
            return;
        }

        /*
         * Debe estar mirando razonablemente hacia el balón.
         */
        if (look.normalize().dot(eyeToBall.normalize())
                < 0.20D) {
            return;
        }

        ball.holdByReferee(player);
    }

    public static int getRemainingCooldownTicks(
            ServerPlayer player
    ) {
        if (player == null) {
            return 0;
        }

        long remaining =
                WHISTLE_COOLDOWNS.getOrDefault(
                        player.getUUID(),
                        0L
                ) - player.level().getGameTime();

        return (int) Math.max(0L, remaining);
    }
}
