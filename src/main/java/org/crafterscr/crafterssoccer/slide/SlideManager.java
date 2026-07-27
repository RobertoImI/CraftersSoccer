package org.crafterscr.crafterssoccer.slide;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.knockdown.PlayerImpactManager;
import org.crafterscr.crafterssoccer.match.SoccerMatchManager;
import org.crafterscr.crafterssoccer.network.SlideStatePayload;
import org.crafterscr.crafterssoccer.referee.RefereeCardManager;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Barrido de fútbol controlado completamente por el servidor.
 */
public final class SlideManager {

    public static final int SLIDE_DURATION_TICKS = 36;
    public static final int SLIDE_COOLDOWN_TICKS = 100;

    /*
     * La velocidad no es constante. El inicio tiene un impulso fuerte,
     * luego el jugador desliza y finalmente frena.
     */
    private static final double START_SPEED = 0.24D;
    private static final double FAST_GLIDE_SPEED = 0.18D;
    private static final double SLOW_GLIDE_SPEED = 0.11D;
    private static final double END_SPEED = 0.045D;

    /**
     * Si el jugador comienza el barrido corriendo, conserva esa sensación
     * con un impulso adicional durante toda la acción.
     */
    private static final double SPRINT_SLIDE_MULTIPLIER = 1.28D;
    private static final double PLAYER_HIT_INFLATE = 0.32D;
    private static final double BALL_SEARCH_RADIUS = 1.85D;
    private static final int BALL_CONTROL_TICKS = 30;
    private static final int OTHER_SLIDE_IMMUNITY_TICKS = 15;

    private static final float BACK_IMPACT = 80.0F;
    private static final float SIDE_IMPACT = 50.0F;
    private static final float FRONT_IMPACT = 25.0F;

    private static final Map<UUID, SlideData> ACTIVE =
            new HashMap<>();

    private static final Map<UUID, Long> COOLDOWN_UNTIL =
            new HashMap<>();

    private SlideManager() {
    }

    public static void tryStart(
            ServerPlayer player
    ) {
        MinecraftServer server =
                player.getServer();

        if (server == null
                || !player.isAlive()
                || player.isSpectator()
                || !player.onGround()
                || RefereeCardManager.isExpelled(
                player.getUUID()
        )
                || PlayerImpactManager.isKnockedDown(
                player.getUUID()
        )
                || SoccerMatchManager.isReferee(
                server,
                player.getUUID()
        )
                || ACTIVE.containsKey(
                player.getUUID()
        )) {
            return;
        }

        long gameTime =
                player.level().getGameTime();

        long cooldownUntil =
                COOLDOWN_UNTIL.getOrDefault(
                        player.getUUID(),
                        0L
                );

        if (gameTime < cooldownUntil) {
            int remainingTicks =
                    (int) (cooldownUntil - gameTime);

            player.displayClientMessage(
                    Component.literal(
                            "§cBarrido disponible en §f"
                                    + String.format(
                                    "%.1f",
                                    remainingTicks / 20.0F
                            )
                                    + "s"
                    ),
                    true
            );
            return;
        }

        Vec3 direction =
                player.getLookAngle()
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        if (direction.lengthSqr() < 0.0001D) {
            return;
        }

        direction =
                direction.normalize();

        ACTIVE.put(
                player.getUUID(),
                new SlideData(
                        direction,
                        player.isSprinting()
                )
        );

        COOLDOWN_UNTIL.put(
                player.getUUID(),
                gameTime + SLIDE_COOLDOWN_TICKS
        );

        synchronize(
                player,
                true
        );

        spawnDust(
                player,
                true
        );

        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.65F,
                0.82F
        );
    }

    public static void tick(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, SlideData>> iterator =
                ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, SlideData> entry =
                    iterator.next();

            ServerPlayer player =
                    server.getPlayerList().getPlayer(
                            entry.getKey()
                    );

            if (player == null) {
                iterator.remove();
                continue;
            }

            SlideData data =
                    entry.getValue();

            if (!player.isAlive()
                    || player.isSpectator()
                    || RefereeCardManager.isExpelled(
                    player.getUUID()
            )
                    || PlayerImpactManager.isKnockedDown(
                    player.getUUID()
            )
                    || SoccerMatchManager.isReferee(
                    server,
                    player.getUUID()
            )
                    || data.ticks >= SLIDE_DURATION_TICKS) {

                finish(
                        player
                );

                iterator.remove();
                continue;
            }

            tickSlide(
                    player,
                    data
            );

            data.ticks++;

            if (data.ticks >= SLIDE_DURATION_TICKS) {
                finish(player);
                iterator.remove();
            }
        }

        COOLDOWN_UNTIL.entrySet().removeIf(
                entry -> {
                    ServerPlayer player =
                            server.getPlayerList().getPlayer(
                                    entry.getKey()
                            );

                    return player == null
                            || player.level().getGameTime()
                            >= entry.getValue();
                }
        );
    }

    private static void tickSlide(
            ServerPlayer player,
            SlideData data
    ) {
        Vec3 previous =
                player.position();

        player.move(
                MoverType.SELF,
                data.direction.scale(
                        getMovementSpeed(
                                data
                        )
                )
        );

        player.setDeltaMovement(
                Vec3.ZERO
        );

        player.fallDistance = 0.0F;

        if (data.ticks % 6 == 0) {
            spawnDust(
                    player,
                    false
            );
        }

        if (!data.hitPlayer) {
            AABB swept =
                    player.getBoundingBox()
                            .minmax(
                                    player.getBoundingBox()
                                            .move(
                                                    previous.subtract(
                                                            player.position()
                                                    )
                                            )
                            )
                            .inflate(
                                    PLAYER_HIT_INFLATE,
                                    0.15D,
                                    PLAYER_HIT_INFLATE
                            );

            List<ServerPlayer> victims =
                    player.serverLevel()
                            .getEntitiesOfClass(
                                    ServerPlayer.class,
                                    swept,
                                    victim ->
                                            victim != player
                                                    && victim.isAlive()
                                                    && !victim.isSpectator()
                                                    && !RefereeCardManager.isExpelled(
                                                    victim.getUUID()
                                            )
                            );

            ServerPlayer victim =
                    victims.stream()
                            .min(
                                    java.util.Comparator.comparingDouble(
                                            player::distanceToSqr
                                    )
                            )
                            .orElse(null);

            if (victim != null) {
                data.hitPlayer = true;

                handleVictim(
                        player,
                        victim
                );
            }
        }
    }

    private static double getMovementSpeed(
            SlideData data
    ) {
        double speed;

        if (data.ticks < 6) {
            speed = START_SPEED;

        } else if (data.ticks < 17) {
            speed = FAST_GLIDE_SPEED;

        } else if (data.ticks < 26) {
            speed = SLOW_GLIDE_SPEED;

        } else {
            speed = END_SPEED;
        }

        if (data.startedSprinting) {
            speed *= SPRINT_SLIDE_MULTIPLIER;
        }

        return speed;
    }

    private static void handleVictim(
            ServerPlayer attacker,
            ServerPlayer victim
    ) {
        MinecraftServer server =
                attacker.getServer();

        if (server == null
                || SoccerMatchManager.isReferee(
                server,
                victim.getUUID()
        )) {
            return;
        }

        ImpactSide side =
                classifyImpact(
                        attacker,
                        victim
                );

        float impact =
                switch (side) {
                    case BACK -> BACK_IMPACT;
                    case SIDE -> SIDE_IMPACT;
                    case FRONT -> FRONT_IMPACT;
                };

        PlayerImpactManager.addSlideImpact(
                victim,
                attacker,
                impact,
                OTHER_SLIDE_IMMUNITY_TICKS
        );

        victim.serverLevel().sendParticles(
                ParticleTypes.CRIT,
                victim.getX(),
                victim.getY() + 0.70D,
                victim.getZ(),
                7,
                0.24D,
                0.34D,
                0.24D,
                0.08D
        );

        victim.level().playSound(
                null,
                victim.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS,
                0.75F,
                side == ImpactSide.BACK
                        ? 0.72F
                        : 0.95F
        );

        tryStealBall(
                attacker,
                victim
        );
    }

    private static ImpactSide classifyImpact(
            ServerPlayer attacker,
            ServerPlayer victim
    ) {
        Vec3 victimForward =
                victim.getLookAngle()
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        Vec3 victimToAttacker =
                attacker.position()
                        .subtract(
                                victim.position()
                        )
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        if (victimForward.lengthSqr() < 0.0001D
                || victimToAttacker.lengthSqr() < 0.0001D) {
            return ImpactSide.SIDE;
        }

        double dot =
                victimForward.normalize()
                        .dot(
                                victimToAttacker.normalize()
                        );

        if (dot <= -0.45D) {
            return ImpactSide.BACK;
        }

        if (dot >= 0.45D) {
            return ImpactSide.FRONT;
        }

        return ImpactSide.SIDE;
    }

    private static void tryStealBall(
            ServerPlayer attacker,
            ServerPlayer victim
    ) {
        AABB search =
                victim.getBoundingBox()
                        .inflate(
                                BALL_SEARCH_RADIUS
                        );

        List<SoccerBallEntity> balls =
                victim.serverLevel()
                        .getEntitiesOfClass(
                                SoccerBallEntity.class,
                                search,
                                ball ->
                                        ball.isAlive()
                                                && !ball.isHeldByReferee()
                                                && !ball.isHeldByGoalkeeper()
                        );

        SoccerBallEntity ball =
                balls.stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        attacker::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (ball == null) {
            return;
        }

        Vec3 attackerLook =
                attacker.getLookAngle()
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        Vec3 attackerToBall =
                ball.position()
                        .subtract(
                                attacker.position()
                        )
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        if (attackerLook.lengthSqr() < 0.0001D
                || attackerToBall.lengthSqr() < 0.0001D
                || attackerLook.normalize()
                .dot(
                        attackerToBall.normalize()
                ) < 0.20D
                || attacker.distanceToSqr(ball)
                > BALL_SEARCH_RADIUS
                * BALL_SEARCH_RADIUS) {
            return;
        }

        ball.grantSlideControl(
                attacker,
                BALL_CONTROL_TICKS
        );

        attacker.displayClientMessage(
                Component.literal(
                        "§a¡Robo limpio! §fControl temporal del balón."
                ),
                true
        );
    }

    private static void spawnDust(
            ServerPlayer player,
            boolean stronger
    ) {
        ServerLevel level =
                player.serverLevel();

        /*
         * Partículas discretas del bloque bajo los pies.
         * Se evita CLOUD porque producía nubes blancas demasiado grandes.
         */
        net.minecraft.core.BlockPos floorPosition =
                player.blockPosition().below();

        net.minecraft.world.level.block.state.BlockState floorState =
                level.getBlockState(
                        floorPosition
                );

        if (!floorState.isAir()) {
            level.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            floorState
                    ),
                    player.getX(),
                    player.getY() + 0.06D,
                    player.getZ(),
                    stronger ? 4 : 1,
                    0.16D,
                    0.025D,
                    0.16D,
                    0.018D
            );
        }

        if (stronger) {
            level.sendParticles(
                    ParticleTypes.POOF,
                    player.getX(),
                    player.getY() + 0.08D,
                    player.getZ(),
                    2,
                    0.12D,
                    0.025D,
                    0.12D,
                    0.012D
            );
        }
    }

    private static void finish(
            ServerPlayer player
    ) {
        player.setDeltaMovement(
                Vec3.ZERO
        );

        synchronize(
                player,
                false
        );
    }

    private static void synchronize(
            ServerPlayer player,
            boolean sliding
    ) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SlideStatePayload(
                        player.getId(),
                        sliding
                )
        );
    }

    public static boolean isSliding(
            UUID playerId
    ) {
        return ACTIVE.containsKey(
                playerId
        );
    }

    public static void clearPlayer(
            ServerPlayer player
    ) {
        if (ACTIVE.remove(
                player.getUUID()
        ) != null) {
            synchronize(
                    player,
                    false
            );
        }

        COOLDOWN_UNTIL.remove(
                player.getUUID()
        );
    }

    private enum ImpactSide {
        BACK,
        SIDE,
        FRONT
    }

    private static final class SlideData {

        private final Vec3 direction;

        private final boolean startedSprinting;

        private int ticks;

        private boolean hitPlayer;

        private SlideData(
                Vec3 direction,
                boolean startedSprinting
        ) {
            this.direction = direction;
            this.startedSprinting = startedSprinting;
        }
    }
}
