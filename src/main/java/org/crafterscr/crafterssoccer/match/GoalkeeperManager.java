package org.crafterscr.crafterssoccer.match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.field.SoccerField;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Sistema autoritativo del portero.
 *
 * El cliente solo solicita la acción. El servidor valida:
 * - partido activo;
 * - estado PLAYING;
 * - portero asignado;
 * - área correcta;
 * - balón oficial;
 * - distancia;
 * - dirección de mirada;
 * - cooldown.
 */
public final class GoalkeeperManager {

    private static final double MAX_ACTION_DISTANCE = 2.35D;
    private static final double MIN_LOOK_DOT = 0.18D;

    private static final double MAX_CATCH_SPEED = 1.02D;
    private static final double REJECT_POWER = 0.92D;

    private static final int SUCCESS_COOLDOWN_TICKS = 50;
    private static final int REJECT_COOLDOWN_TICKS = 30;
    private static final int FAILED_COOLDOWN_TICKS = 14;

    private static final Map<UUID, Long> COOLDOWN_UNTIL =
            new HashMap<>();

    private GoalkeeperManager() {
    }

    public static void handleAction(
            MinecraftServer server,
            ServerPlayer player,
            int requestedBallEntityId
    ) {
        SoccerMatch match =
                SoccerMatchManager.getActiveMatch(server);

        if (match == null
                || match.getState() != SoccerMatchState.PLAYING) {
            return;
        }

        SoccerTeamSide side =
                SoccerMatchManager.getPlayerTeam(
                        server,
                        player.getUUID()
                );

        if (side == null
                || !player.getUUID().equals(
                SoccerMatchManager.getGoalkeeper(server, side))) {
            return;
        }

        SoccerField field =
                org.crafterscr.crafterssoccer.field
                        .SoccerFieldManager.getField(
                                server,
                                match.getFieldId()
                        );

        if (field == null
                || !isInsideOwnArea(field, side, player.position())) {
            return;
        }

        if (!(player.level()
                instanceof net.minecraft.server.level.ServerLevel level)
                || match.getOfficialBallUuid() == null) {
            return;
        }

        Entity officialEntity =
                level.getEntity(match.getOfficialBallUuid());

        if (!(officialEntity instanceof SoccerBallEntity ball)) {
            return;
        }

        /*
         * Segundo clic derecho mientras sostiene:
         * no necesita estar apuntando exactamente al balón.
         */
        if (ball.isHeldBy(player.getUUID())) {
            Vec3 direction =
                    normalizedLook(player);

            ball.releaseFromGoalkeeper(
                    direction.scale(0.32D)
                            .add(0.0D, 0.08D, 0.0D)
            );

            applyCooldown(player, SUCCESS_COOLDOWN_TICKS);
            return;
        }

        if (requestedBallEntityId != ball.getId()) {
            applyCooldown(player, FAILED_COOLDOWN_TICKS);
            return;
        }

        if (isOnCooldown(player)) {
            return;
        }

        if (player.distanceToSqr(ball)
                > MAX_ACTION_DISTANCE * MAX_ACTION_DISTANCE) {
            applyCooldown(player, FAILED_COOLDOWN_TICKS);
            return;
        }

        Vec3 eyeToBall =
                ball.position()
                        .add(0.0D, SoccerBallEntity.BALL_RADIUS, 0.0D)
                        .subtract(player.getEyePosition());

        if (eyeToBall.lengthSqr() < 0.0001D) {
            applyCooldown(player, FAILED_COOLDOWN_TICKS);
            return;
        }

        double lookDot =
                normalizedLook(player)
                        .dot(eyeToBall.normalize());

        if (lookDot < MIN_LOOK_DOT) {
            applyCooldown(player, FAILED_COOLDOWN_TICKS);
            return;
        }

        double speed =
                ball.getDeltaMovement().length();

        if (speed <= MAX_CATCH_SPEED) {
            ball.holdByGoalkeeper(
                    player,
                    60
            );

            player.displayClientMessage(
                    Component.literal("§a¡Balón atrapado!"),
                    true
            );

            applyCooldown(player, SUCCESS_COOLDOWN_TICKS);
            return;
        }

        /*
         * Un disparo fuerte no queda pegado:
         * el portero lo rechaza y deja un rebote jugable.
         */
        Vec3 rejectDirection =
                normalizedLook(player)
                        .multiply(1.0D, 0.25D, 1.0D)
                        .normalize()
                        .scale(REJECT_POWER)
                        .add(0.0D, 0.16D, 0.0D);

        ball.setDeltaMovement(rejectDirection);
        ball.hasImpulse = true;

        player.displayClientMessage(
                Component.literal("§e¡Atajada con rechazo!"),
                true
        );

        applyCooldown(player, REJECT_COOLDOWN_TICKS);
    }

    public static void tick(
            MinecraftServer server,
            SoccerMatch match,
            SoccerField field
    ) {
        UUID ballId = match.getOfficialBallUuid();

        if (ballId == null) {
            return;
        }

        /*
         * El balón controla su posición mientras está sostenido.
         * Aquí solo comprobamos que el portero no abandone su área.
         */
        for (ServerPlayer player
                : server.getPlayerList().getPlayers()) {

            SoccerTeamSide side =
                    SoccerMatchManager.getPlayerTeam(
                            server,
                            player.getUUID()
                    );

            if (side == null
                    || !player.getUUID().equals(
                    SoccerMatchManager.getGoalkeeper(server, side))) {
                continue;
            }

            Entity possibleBall =
                    player.level().getEntity(
                            findOfficialBallEntityId(player, ballId)
                    );

            if (!(possibleBall instanceof SoccerBallEntity ball)
                    || !ball.isHeldBy(player.getUUID())) {
                continue;
            }

            if (match.getState() != SoccerMatchState.PLAYING
                    || !isInsideOwnArea(
                    field,
                    side,
                    player.position()
            )) {
                ball.releaseFromGoalkeeper(Vec3.ZERO);
            }
        }

        long gameTime = server.overworld().getGameTime();

        COOLDOWN_UNTIL.entrySet().removeIf(
                entry -> entry.getValue() <= gameTime
                        && server.getPlayerList()
                        .getPlayer(entry.getKey()) == null
        );
    }

    private static int findOfficialBallEntityId(
            ServerPlayer player,
            UUID officialBallUuid
    ) {
        Entity entity =
                ((net.minecraft.server.level.ServerLevel) player.level())
                        .getEntity(officialBallUuid);

        return entity == null ? -1 : entity.getId();
    }

    public static ClientStatus getClientStatus(
            MinecraftServer server,
            SoccerMatch match,
            ServerPlayer player
    ) {
        SoccerTeamSide side =
                SoccerMatchManager.getPlayerTeam(
                        server,
                        player.getUUID()
                );

        boolean goalkeeper =
                side != null
                        && player.getUUID().equals(
                        SoccerMatchManager.getGoalkeeper(server, side)
                );

        if (!goalkeeper) {
            return ClientStatus.NONE;
        }

        SoccerField field =
                org.crafterscr.crafterssoccer.field
                        .SoccerFieldManager.getField(
                                server,
                                match.getFieldId()
                        );

        boolean inArea =
                field != null
                        && isInsideOwnArea(
                        field,
                        side,
                        player.position()
                );

        SoccerBallEntity ball = null;

        if (match.getOfficialBallUuid() != null
                && player.level()
                instanceof net.minecraft.server.level.ServerLevel level) {

            Entity entity =
                    level.getEntity(match.getOfficialBallUuid());

            if (entity instanceof SoccerBallEntity soccerBall) {
                ball = soccerBall;
            }
        }

        boolean holding =
                ball != null
                        && ball.isHeldBy(player.getUUID());

        int cooldown =
                getCooldownTicks(player);

        boolean available =
                match.getState() == SoccerMatchState.PLAYING
                        && inArea
                        && cooldown <= 0;

        return new ClientStatus(
                inArea,
                available,
                holding,
                cooldown
        );
    }

    private static boolean isInsideOwnArea(
            SoccerField field,
            SoccerTeamSide side,
            Vec3 position
    ) {
        AABB area =
                side == SoccerTeamSide.RED
                        ? field.getRedGoalkeeperAreaBounds()
                        : field.getBlueGoalkeeperAreaBounds();

        return area != null
                && area.inflate(0.001D).contains(position);
    }

    private static Vec3 normalizedLook(
            ServerPlayer player
    ) {
        Vec3 look = player.getLookAngle();

        if (look.lengthSqr() < 0.0001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }

        return look.normalize();
    }

    private static void applyCooldown(
            ServerPlayer player,
            int ticks
    ) {
        COOLDOWN_UNTIL.put(
                player.getUUID(),
                player.level().getGameTime() + ticks
        );
    }

    private static boolean isOnCooldown(
            ServerPlayer player
    ) {
        return getCooldownTicks(player) > 0;
    }

    private static int getCooldownTicks(
            ServerPlayer player
    ) {
        long remaining =
                COOLDOWN_UNTIL.getOrDefault(
                        player.getUUID(),
                        0L
                ) - player.level().getGameTime();

        return (int) Math.max(0L, remaining);
    }

    public record ClientStatus(
            boolean inArea,
            boolean available,
            boolean holdingBall,
            int cooldownTicks
    ) {
        public static final ClientStatus NONE =
                new ClientStatus(
                        false,
                        false,
                        false,
                        0
                );
    }
}
