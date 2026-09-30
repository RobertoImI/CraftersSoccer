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

    public static final int SLIDE_DURATION_TICKS = 20;
    public static final int SLIDE_COOLDOWN_TICKS = 100;

    /**
     * Perfil determinista del barrido, en bloques por tick.
     *
     * La distancia total es de aproximadamente 5.44 bloques cuando no
     * hay colisiones. No depende de sprint, cámara ni velocidad previa,
     * por lo que primera y tercera persona tienen exactamente el mismo
     * alcance real en el servidor.
     */
    private static final double[] SLIDE_SPEED_PROFILE = {
            0.35D, 0.40D, 0.42D, 0.42D, 0.40D,
            0.38D, 0.36D, 0.34D, 0.32D, 0.30D,
            0.28D, 0.26D, 0.24D, 0.22D, 0.20D,
            0.17D, 0.14D, 0.11D, 0.08D, 0.05D
    };

    /*
     * La hitbox del tackle es deliberadamente baja y contenida.
     * La animación puede inclinar el cuerpo, pero la detección competitiva
     * se concentra cerca de las piernas.
     */
    private static final double PLAYER_HIT_INFLATE = 0.20D;
    private static final double TACKLE_HEIGHT = 0.95D;
    private static final double BALL_HIT_INFLATE = 0.30D;
    private static final int BALL_CONTROL_TICKS = 14;
    private static final int OTHER_SLIDE_IMMUNITY_TICKS = 20;

    /*
     * El barrido sigue premiando entrar por detrás, pero ya no deja a la
     * víctima prácticamente derribada con un solo contacto.
     */
    private static final float BACK_IMPACT = 60.0F;
    private static final float SIDE_IMPACT = 35.0F;
    private static final float FRONT_IMPACT = 20.0F;

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
                        direction
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
        Vec3 previousPosition =
                player.position();

        AABB previousBox =
                player.getBoundingBox();

        player.move(
                MoverType.SELF,
                data.direction.scale(
                        getMovementSpeed(
                                data
                        )
                )
        );

        /*
         * El servidor es la única autoridad del desplazamiento del tackle.
         * Se elimina cualquier velocidad residual para que el resultado no
         * cambie por sprint, input local o la cámara utilizada.
         */
        player.setDeltaMovement(
                Vec3.ZERO
        );

        player.fallDistance = 0.0F;

        if (data.ticks % 5 == 0) {
            spawnDust(
                    player,
                    false
            );
        }

        AABB currentBox =
                player.getBoundingBox();

        AABB swept =
                currentBox
                        .minmax(previousBox)
                        .inflate(
                                PLAYER_HIT_INFLATE,
                                0.05D,
                                PLAYER_HIT_INFLATE
                        );

        /*
         * Limita verticalmente la zona activa del barrido. Así la hitbox
         * sigue la zona de las piernas y no todo el cuerpo de pie.
         */
        AABB tackleBox =
                new AABB(
                        swept.minX,
                        swept.minY,
                        swept.minZ,
                        swept.maxX,
                        Math.min(
                                swept.maxY,
                                player.getY()
                                        + TACKLE_HEIGHT
                        ),
                        swept.maxZ
                );

        /*
         * El balón se comprueba independientemente del jugador. Un tackle
         * que toca pelota limpiamente ya no necesita golpear antes a un
         * rival para obtener interacción con el balón.
         */
        if (!data.touchedBall) {
            tryTouchBall(
                    player,
                    data,
                    tackleBox
            );
        }

        if (!data.hitPlayer) {
            List<ServerPlayer> victims =
                    player.serverLevel()
                            .getEntitiesOfClass(
                                    ServerPlayer.class,
                                    tackleBox,
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
                                            candidate ->
                                                    previousPosition.distanceToSqr(
                                                            candidate.position()
                                                    )
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
        int index =
                Math.max(
                        0,
                        Math.min(
                                data.ticks,
                                SLIDE_SPEED_PROFILE.length - 1
                        )
                );

        return SLIDE_SPEED_PROFILE[index];
    }

    private static void tryTouchBall(
            ServerPlayer player,
            SlideData data,
            AABB tackleBox
    ) {
        AABB ballBox =
                tackleBox.inflate(
                        BALL_HIT_INFLATE,
                        0.20D,
                        BALL_HIT_INFLATE
                );

        List<SoccerBallEntity> balls =
                player.serverLevel()
                        .getEntitiesOfClass(
                                SoccerBallEntity.class,
                                ballBox,
                                ball ->
                                        ball.isAlive()
                                                && !ball.isHeldByReferee()
                                                && !ball.isHeldByGoalkeeper()
                        );

        SoccerBallEntity ball =
                balls.stream()
                        .min(
                                java.util.Comparator.comparingDouble(
                                        player::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (ball == null) {
            return;
        }

        data.touchedBall = true;

        ball.grantSlideControl(
                player,
                BALL_CONTROL_TICKS
        );
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

        private int ticks;

        private boolean hitPlayer;

        private boolean touchedBall;

        private SlideData(
                Vec3 direction
        ) {
            this.direction = direction;
        }
    }
}
