package org.crafterscr.crafterssoccer.match;

import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.field.SoccerField;
import org.crafterscr.crafterssoccer.field.SoccerFieldManager;
import org.crafterscr.crafterssoccer.physics.SoccerBallPhysics;
import org.crafterscr.crafterssoccer.registry.ModEntities;
import org.crafterscr.crafterssoccer.registry.ModSounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Representa un partido activo.
 *
 * Cambios nuevos:
 * - Goles anuncian el nombre personalizado del equipo.
 * - La bola oficial rebota dentro del field.
 * - Soporta múltiples spawns por equipo.
 */
public final class SoccerMatch {

    private static final int INITIAL_COUNTDOWN_TICKS = 60;
    private static final int GOAL_PAUSE_TICKS = 80;
    private static final int FINISH_DISPLAY_TICKS = 200;

    /**
     * Rebote del "muro invisible" del field.
     */
    private static final double FIELD_WALL_BOUNCE = 0.78D;

    private final String fieldId;
    private final int initialDurationTicks;

    private SoccerMatchState state =
            SoccerMatchState.COUNTDOWN;

    private int redScore;
    private int blueScore;

    /**
     * Minutos de reposición configurados por el árbitro o un administrador.
     * El valor se muestra en el HUD como +5, +7, etc.
     */
    private int addedTimeMinutes;

    private int remainingTicks;
    private int stateTicks;

    private UUID officialBallUuid;
    private Vec3 previousBallCenter;

    /**
     * Solo el primer conteo del partido anuncia
     * "¡Comienza el partido!" en el chat.
     */
    private boolean firstKickoff = true;

    private String message = "COMIENZA EN 3";

    public SoccerMatch(
            String fieldId,
            int durationTicks
    ) {
        this.fieldId = fieldId;
        this.initialDurationTicks = durationTicks;
        this.remainingTicks = durationTicks;
        this.stateTicks = INITIAL_COUNTDOWN_TICKS;
        this.firstKickoff = true;
    }

    public String getFieldId() {
        return fieldId;
    }

    public SoccerMatchState getState() {
        return state;
    }

    public int getRedScore() {
        return redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public int getAddedTimeMinutes() {
        return addedTimeMinutes;
    }

    public String getMessage() {
        return message;
    }

    public UUID getOfficialBallUuid() {
        return officialBallUuid;
    }

    /**
     * Expone únicamente el balón oficial del partido activo para
     * acciones autoritativas del árbitro.
     */
    public SoccerBallEntity getOfficialBall(
            MinecraftServer server
    ) {
        SoccerField field =
                SoccerFieldManager.getField(
                        server,
                        fieldId
                );

        if (field == null) {
            return null;
        }

        return getOfficialBall(
                server,
                field
        );
    }

    public void start(
            MinecraftServer server,
            SoccerField field,
            Set<UUID> redPlayers,
            Set<UUID> bluePlayers
    ) {
        teleportTeams(
                server,
                field,
                redPlayers,
                bluePlayers
        );

        spawnOfficialBall(
                server,
                field
        );

        this.state =
                SoccerMatchState.COUNTDOWN;

        this.stateTicks =
                INITIAL_COUNTDOWN_TICKS;

        this.message =
                "COMIENZA EN 3";
    }

    public void tick(
            MinecraftServer server,
            SoccerField field,
            Set<UUID> redPlayers,
            Set<UUID> bluePlayers
    ) {
        ServerLevel level =
                getFieldLevel(server, field);

        if (level == null) {
            return;
        }

        SoccerBallEntity ball =
                getOrCreateOfficialBall(
                        server,
                        field
                );

        if (ball == null) {
            return;
        }

        switch (state) {
            case COUNTDOWN -> tickCountdown(
                    server,
                    field,
                    ball,
                    redPlayers,
                    bluePlayers
            );

            case PLAYING -> tickPlaying(
                    server,
                    field,
                    ball
            );

            case PAUSED -> freezeBall(
                    ball,
                    field
            );

            case GOAL_PAUSE -> tickGoalPause(
                    server,
                    field,
                    ball,
                    redPlayers,
                    bluePlayers
            );

            case FINISHED -> tickFinished(
                    ball
            );
        }
    }

    private void tickCountdown(
            MinecraftServer server,
            SoccerField field,
            SoccerBallEntity ball,
            Set<UUID> redPlayers,
            Set<UUID> bluePlayers
    ) {
        freezeBall(
                ball,
                field
        );

        stateTicks--;

        int seconds =
                MatchClock.countdownSeconds(stateTicks);

        message =
                "COMIENZA EN " + seconds;

        if (stateTicks > 0) {
            return;
        }

        state =
                SoccerMatchState.PLAYING;

        message = "";

        previousBallCenter =
                getBallCenter(ball);

        playWhistleForEveryone(
                server,
                ModSounds.WHISTLE_START
        );

        if (firstKickoff) {
            broadcast(
                    server,
                    "§a¡Comienza el partido!"
            );

            firstKickoff = false;
        }
    }

    private void tickPlaying(
            MinecraftServer server,
            SoccerField field,
            SoccerBallEntity ball
    ) {
        remainingTicks = MatchClock.tickRemaining(remainingTicks);

        Vec3 currentCenter =
                getBallCenter(ball);

        if (previousBallCenter == null) {
            previousBallCenter =
                    currentCenter;
        }

        if (crossedGoal(
                field.getRedGoalBounds(),
                previousBallCenter,
                currentCenter
        )) {
            scoreGoal(
                    server,
                    field,
                    ball,
                    SoccerTeamSide.BLUE
            );
            return;
        }

        if (crossedGoal(
                field.getBlueGoalBounds(),
                previousBallCenter,
                currentCenter
        )) {
            scoreGoal(
                    server,
                    field,
                    ball,
                    SoccerTeamSide.RED
            );
            return;
        }

        keepBallInsideField(
                ball,
                field
        );

        previousBallCenter =
                getBallCenter(ball);

        if (remainingTicks <= 0) {
            finish(server, ball);
        }
    }

    private void tickGoalPause(
            MinecraftServer server,
            SoccerField field,
            SoccerBallEntity ball,
            Set<UUID> redPlayers,
            Set<UUID> bluePlayers
    ) {
        freezeBall(
                ball,
                field
        );

        stateTicks--;

        if (stateTicks > 0) {
            return;
        }

        teleportTeams(
                server,
                field,
                redPlayers,
                bluePlayers
        );

        resetBall(
                ball,
                field
        );

        state =
                SoccerMatchState.COUNTDOWN;

        stateTicks =
                INITIAL_COUNTDOWN_TICKS;

        message =
                "COMIENZA EN 3";
    }

    private void tickFinished(
            SoccerBallEntity ball
    ) {
        ball.setDeltaMovement(
                Vec3.ZERO
        );

        ball.hasImpulse = true;

        stateTicks--;
    }

    private void scoreGoal(
            MinecraftServer server,
            SoccerField field,
            SoccerBallEntity ball,
            SoccerTeamSide scoringTeam
    ) {
        if (scoringTeam == SoccerTeamSide.RED) {
            redScore++;
        } else {
            blueScore++;
        }

        state =
                SoccerMatchState.GOAL_PAUSE;

        stateTicks =
                GOAL_PAUSE_TICKS;

        String scoringTeamName =
                SoccerMatchManager.getTeamName(
                        server,
                        scoringTeam
                );

        message =
                "¡GOL DE " + scoringTeamName + "!";

        freezeBall(
                ball,
                field
        );

        broadcast(
                server,
                "§6§l¡GOL DE "
                        + scoringTeamName
                        + "! §f"
                        + redScore
                        + " - "
                        + blueScore
        );

        ServerLevel level =
                getFieldLevel(server, field);

        if (level != null) {
            Vec3 position =
                    field.getBallSpawnPosition();

            if (position != null) {
                level.playSound(
                        null,
                        position.x,
                        position.y,
                        position.z,
                        SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.PLAYERS,
                        1.4F,
                        1.0F
                );
            }
        }
    }

    private void finish(
            MinecraftServer server,
            SoccerBallEntity ball
    ) {
        state =
                SoccerMatchState.FINISHED;

        stateTicks =
                FINISH_DISPLAY_TICKS;

        message =
                "FINAL DEL PARTIDO";

        ball.setDeltaMovement(
                Vec3.ZERO
        );

        ball.hasImpulse = true;

        playWhistleForEveryone(
                server,
                ModSounds.WHISTLE_END
        );

        broadcast(
                server,
                "§6§lFINAL DEL PARTIDO §f"
                        + redScore
                        + " - "
                        + blueScore
        );
    }

    /**
     * Corrige manualmente el marcador sin provocar pausa de gol,
     * teletransporte ni reinicio del balón.
     */
    public int adjustScore(
            SoccerTeamSide side,
            int amount
    ) {
        if (side == SoccerTeamSide.RED) {
            redScore =
                    Math.max(
                            0,
                            redScore + amount
                    );

            return redScore;
        }

        blueScore =
                Math.max(
                        0,
                        blueScore + amount
                );

        return blueScore;
    }

    /**
     * Establece el total de reposición.
     *
     * La diferencia respecto al valor anterior se suma o se resta
     * del tiempo restante para que cambiar +5 por +7 agregue
     * únicamente dos minutos adicionales.
     */
    public int setAddedTimeMinutes(
            int requestedMinutes
    ) {
        int safeMinutes =
                Math.max(
                        0,
                        Math.min(
                                120,
                                requestedMinutes
                        )
                );

        int difference =
                safeMinutes
                        - addedTimeMinutes;

        addedTimeMinutes =
                safeMinutes;

        remainingTicks =
                Math.max(
                        0,
                        remainingTicks
                                + difference
                                * 60
                                * 20
                );

        return addedTimeMinutes;
    }

    public int addAddedTimeMinutes(
            int minutes
    ) {
        return setAddedTimeMinutes(
                addedTimeMinutes
                        + Math.max(
                        0,
                        minutes
                )
        );
    }

    public void pause() {
        if (state != SoccerMatchState.PLAYING
                && state != SoccerMatchState.COUNTDOWN) {
            return;
        }

        state =
                SoccerMatchState.PAUSED;

        message =
                "PARTIDO PAUSADO";
    }

    public void resume() {
        if (state != SoccerMatchState.PAUSED) {
            return;
        }

        state =
                SoccerMatchState.COUNTDOWN;

        stateTicks =
                INITIAL_COUNTDOWN_TICKS;

        message =
                "COMIENZA EN 3";
    }

    public boolean shouldBeRemoved() {
        return state == SoccerMatchState.FINISHED
                && stateTicks <= 0;
    }

    public void removeOfficialBall(
            MinecraftServer server,
            SoccerField field
    ) {
        SoccerBallEntity ball =
                getOfficialBall(
                        server,
                        field
                );

        if (ball != null) {
            ball.discard();
        }

        officialBallUuid = null;
        previousBallCenter = null;
    }

    private SoccerBallEntity getOrCreateOfficialBall(
            MinecraftServer server,
            SoccerField field
    ) {
        SoccerBallEntity ball =
                getOfficialBall(
                        server,
                        field
                );

        if (ball != null) {
            return ball;
        }

        return spawnOfficialBall(
                server,
                field
        );
    }

    private SoccerBallEntity getOfficialBall(
            MinecraftServer server,
            SoccerField field
    ) {
        if (officialBallUuid == null) {
            return null;
        }

        ServerLevel level =
                getFieldLevel(server, field);

        if (level == null) {
            return null;
        }

        if (level.getEntity(officialBallUuid)
                instanceof SoccerBallEntity ball) {
            return ball;
        }

        return null;
    }

    private SoccerBallEntity spawnOfficialBall(
            MinecraftServer server,
            SoccerField field
    ) {
        ServerLevel level =
                getFieldLevel(server, field);

        Vec3 position =
                field.getBallSpawnPosition();

        if (level == null || position == null) {
            return null;
        }

        SoccerBallEntity ball =
                new SoccerBallEntity(
                        ModEntities.SOCCER_BALL.get(),
                        level
                );

        ball.setPos(
                position.x,
                position.y,
                position.z
        );

        ball.setDeltaMovement(
                Vec3.ZERO
        );

        level.addFreshEntity(ball);

        officialBallUuid =
                ball.getUUID();

        previousBallCenter =
                getBallCenter(ball);

        return ball;
    }

    private void freezeBall(
            SoccerBallEntity ball,
            SoccerField field
    ) {
        Vec3 position =
                field.getBallSpawnPosition();

        if (position != null) {
            ball.setPos(
                    position.x,
                    position.y,
                    position.z
            );
        }

        ball.setDeltaMovement(
                Vec3.ZERO
        );

        ball.hasImpulse = true;

        previousBallCenter =
                getBallCenter(ball);
    }

    private void resetBall(
            SoccerBallEntity ball,
            SoccerField field
    ) {
        freezeBall(
                ball,
                field
        );
    }

    /**
     * Mantiene el balón oficial dentro de los límites
     * horizontales del field.
     *
     * Los jugadores pueden salir del área si quieren,
     * pero el balón rebotará como si existiera una pared
     * invisible.
     */
    private void keepBallInsideField(
            SoccerBallEntity ball,
            SoccerField field
    ) {
        AABB bounds =
                field.getFieldBounds();

        if (bounds == null) {
            return;
        }

        double radius =
                SoccerBallEntity.BALL_RADIUS;

        double minX =
                bounds.minX + radius;

        double maxX =
                bounds.maxX - radius;

        double minZ =
                bounds.minZ + radius;

        double maxZ =
                bounds.maxZ - radius;

        Vec3 position =
                ball.position();

        double x = position.x;
        double y = position.y;
        double z = position.z;

        Vec3 velocity =
                ball.getDeltaMovement();

        double velocityX =
                velocity.x;

        double velocityY =
                velocity.y;

        double velocityZ =
                velocity.z;

        boolean bounced = false;

        if (x < minX) {
            x = minX;
            velocityX =
                    Math.abs(velocityX)
                            * FIELD_WALL_BOUNCE;
            bounced = true;
        } else if (x > maxX) {
            x = maxX;
            velocityX =
                    -Math.abs(velocityX)
                            * FIELD_WALL_BOUNCE;
            bounced = true;
        }

        if (z < minZ) {
            z = minZ;
            velocityZ =
                    Math.abs(velocityZ)
                            * FIELD_WALL_BOUNCE;
            bounced = true;
        } else if (z > maxZ) {
            z = maxZ;
            velocityZ =
                    -Math.abs(velocityZ)
                            * FIELD_WALL_BOUNCE;
            bounced = true;
        }

        if (!bounced) {
            return;
        }

        ball.setPos(
                x,
                y,
                z
        );

        ball.setDeltaMovement(
                SoccerBallPhysics.clampVelocity(
                        new Vec3(
                                velocityX,
                                velocityY,
                                velocityZ
                        )
                )
        );

        ball.hasImpulse = true;
    }

    private static Vec3 getBallCenter(
            SoccerBallEntity ball
    ) {
        return ball.position().add(
                0.0D,
                SoccerBallEntity.BALL_RADIUS,
                0.0D
        );
    }

    private static boolean crossedGoal(
            AABB goal,
            Vec3 previous,
            Vec3 current
    ) {
        return GoalDetector.crossed(goal, previous, current);
    }

    private static void teleportTeams(
            MinecraftServer server,
            SoccerField field,
            Set<UUID> redPlayers,
            Set<UUID> bluePlayers
    ) {
        ServerLevel level =
                getFieldLevel(server, field);

        if (level == null) {
            return;
        }

        teleportPlayers(
                server,
                level,
                redPlayers,
                field,
                SoccerTeamSide.RED
        );

        teleportPlayers(
                server,
                level,
                bluePlayers,
                field,
                SoccerTeamSide.BLUE
        );
    }

    private static void teleportPlayers(
            MinecraftServer server,
            ServerLevel level,
            Set<UUID> playerIds,
            SoccerField field,
            SoccerTeamSide side
    ) {
        int playerIndex = 0;

        for (UUID playerId : playerIds) {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(playerId);

            if (player == null) {
                continue;
            }

            Vec3 position =
                    side == SoccerTeamSide.RED
                            ? field.getRedSpawnPosition(
                            playerIndex
                    )
                            : field.getBlueSpawnPosition(
                            playerIndex
                    );

            playerIndex++;

            if (position == null) {
                continue;
            }

            player.teleportTo(
                    level,
                    position.x,
                    position.y,
                    position.z,
                    Set.<RelativeMovement>of(),
                    player.getYRot(),
                    player.getXRot()
            );

            player.setDeltaMovement(
                    Vec3.ZERO
            );
        }
    }

    private static ServerLevel getFieldLevel(
            MinecraftServer server,
            SoccerField field
    ) {
        ResourceLocation dimensionLocation;

        try {
            dimensionLocation =
                    ResourceLocation.parse(
                            field.getDimensionId()
                    );
        } catch (Exception exception) {
            return null;
        }

        ResourceKey<Level> dimensionKey =
                ResourceKey.create(
                        Registries.DIMENSION,
                        dimensionLocation
                );

        return server.getLevel(
                dimensionKey
        );
    }

    private static void playWhistleForEveryone(
            MinecraftServer server,
            DeferredHolder<
                    net.minecraft.sounds.SoundEvent,
                    net.minecraft.sounds.SoundEvent
                    > sound
    ) {
        for (ServerPlayer player
                : server.getPlayerList().getPlayers()) {

            player.connection.send(
                    new ClientboundSoundPacket(
                            sound,
                            SoundSource.PLAYERS,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            0.52F,
                            1.0F,
                            player.getRandom().nextLong()
                    )
            );
        }
    }

    private static void broadcast(
            MinecraftServer server,
            String text
    ) {
        server.getPlayerList()
                .broadcastSystemMessage(
                        Component.literal(text),
                        false
                );
    }
}