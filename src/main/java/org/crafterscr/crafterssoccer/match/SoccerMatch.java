package org.crafterscr.crafterssoccer.match;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.field.SoccerField;
import org.crafterscr.crafterssoccer.registry.ModEntities;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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

/**
 * Representa un partido activo.
 */
public final class SoccerMatch {

    private static final int INITIAL_COUNTDOWN_TICKS = 60;
    private static final int GOAL_PAUSE_TICKS = 80;
    private static final int FINISH_DISPLAY_TICKS = 200;

    private final String fieldId;
    private final int initialDurationTicks;

    private SoccerMatchState state =
            SoccerMatchState.COUNTDOWN;

    private int redScore;
    private int blueScore;

    private int remainingTicks;
    private int stateTicks;

    private UUID officialBallUuid;
    private Vec3 previousBallCenter;

    private String message = "COMIENZA EN 3";

    public SoccerMatch(
            String fieldId,
            int durationTicks
    ) {
        this.fieldId = fieldId;
        this.initialDurationTicks = durationTicks;
        this.remainingTicks = durationTicks;
        this.stateTicks = INITIAL_COUNTDOWN_TICKS;
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

    public String getMessage() {
        return message;
    }

    public UUID getOfficialBallUuid() {
        return officialBallUuid;
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
                Math.max(
                        1,
                        (stateTicks + 19) / 20
                );

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

        broadcast(
                server,
                "§a¡Comienza el partido!"
        );
    }

    private void tickPlaying(
            MinecraftServer server,
            SoccerField field,
            SoccerBallEntity ball
    ) {
        if (remainingTicks > 0) {
            remainingTicks--;
        }

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

        previousBallCenter =
                currentCenter;

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

        message =
                "¡GOL DEL EQUIPO "
                        + scoringTeam.getDisplayName()
                        + "!";

        freezeBall(
                ball,
                field
        );

        broadcast(
                server,
                "§6§l¡GOL DEL EQUIPO "
                        + scoringTeam.getDisplayName()
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

        broadcast(
                server,
                "§6§lFINAL DEL PARTIDO §f"
                        + redScore
                        + " - "
                        + blueScore
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
        if (goal == null
                || previous == null
                || current == null) {
            return false;
        }

        if (goal.contains(current)) {
            return true;
        }

        Optional<Vec3> intersection =
                goal.clip(
                        previous,
                        current
                );

        return intersection.isPresent();
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
                field.getRedSpawnPosition()
        );

        teleportPlayers(
                server,
                level,
                bluePlayers,
                field.getBlueSpawnPosition()
        );
    }

    private static void teleportPlayers(
            MinecraftServer server,
            ServerLevel level,
            Set<UUID> playerIds,
            Vec3 position
    ) {
        if (position == null) {
            return;
        }

        for (UUID playerId : playerIds) {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(playerId);

            if (player == null) {
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