package org.crafterscr.crafterssoccer.knockdown;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.crafterscr.crafterssoccer.match.SoccerMatchManager;
import org.crafterscr.crafterssoccer.network.KnockdownStatePayload;
import org.crafterscr.crafterssoccer.network.RecoveryProgressPayload;
import org.crafterscr.crafterssoccer.referee.RefereeCardManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Acumulador de impactos entre jugadores.
 *
 * Solamente recibe puntos cuando un jugador golpea cuerpo a cuerpo
 * a otro jugador. El balón no participa en este acumulador.
 */
public final class PlayerImpactManager {

    public static final float MAX_IMPACT = 100.0F;

    private static final float BASE_IMPACT_PER_HIT = 18.0F;
    private static final float IMPACT_PER_DAMAGE_POINT = 4.0F;
    private static final float MAX_IMPACT_PER_NORMAL_HIT = 38.0F;

    /**
     * Protección después de levantarse:
     * 60 ticks = 3 segundos.
     */
    private static final int STAND_UP_IMMUNITY_TICKS = 60;

    /**
     * Recuperación pulsando repetidamente G.
     */
    private static final int RECOVERY_MAX = 100;
    private static final int RECOVERY_PER_TAP = 12;
    private static final int RECOVERY_DECAY_GRACE_TICKS = 8;
    private static final int RECOVERY_DECAY_INTERVAL_TICKS = 2;
    private static final int RECOVERY_DECAY_PER_INTERVAL = 2;
    private static final int MIN_TICKS_BETWEEN_TAPS = 1;

    /*
     * Protección anti-gank para golpes normales.
     */
    private static final int RAPID_HIT_WINDOW_TICKS = 10;
    private static final float RAPID_HIT_MULTIPLIER = 0.40F;
    private static final int NORMAL_CAP_WINDOW_TICKS = 30;
    private static final float NORMAL_IMPACT_CAP_PER_WINDOW = 48.0F;

    /*
     * Recuperación pasiva del impacto.
     */
    private static final int PASSIVE_DECAY_DELAY_TICKS = 60;
    private static final int PASSIVE_DECAY_INTERVAL_TICKS = 4;
    private static final float PASSIVE_DECAY_AMOUNT = 1.0F;

    private static final Map<UUID, Long> LAST_NORMAL_HIT =
            new HashMap<>();

    private static final Map<UUID, ImpactWindow> NORMAL_WINDOWS =
            new HashMap<>();

    private static final Map<UUID, Long> LAST_ANY_IMPACT =
            new HashMap<>();

    private static final Map<UUID, Long> LAST_PASSIVE_DECAY =
            new HashMap<>();

    private static final Map<UUID, Long> SLIDE_IMMUNE_UNTIL =
            new HashMap<>();

    private static final Map<UUID, Float> IMPACT =
            new HashMap<>();

    private static final Map<UUID, DownData> DOWN =
            new HashMap<>();

    private static final Map<UUID, Long> IMMUNE_UNTIL =
            new HashMap<>();

    private PlayerImpactManager() {
    }

    /**
     * Entrada general reutilizable.
     *
     * El futuro slide_soccer podrá llamar este método con una
     * cantidad alta, utilizando exactamente el mismo acumulador.
     */
    public static boolean addImpact(
            ServerPlayer victim,
            ServerPlayer attacker,
            float amount
    ) {
        if (victim == null
                || attacker == null
                || victim == attacker
                || !victim.isAlive()
                || victim.isSpectator()
                || isKnockedDown(victim.getUUID())
                || RefereeCardManager.isExpelled(
                victim.getUUID()
        )) {
            return false;
        }

        /*
         * El árbitro nunca acumula impacto y nunca puede
         * entrar al estado de derribo.
         */
        if (isReferee(victim)) {
            IMPACT.remove(
                    victim.getUUID()
            );
            return false;
        }

        long gameTime =
                victim.level().getGameTime();

        if (gameTime < IMMUNE_UNTIL.getOrDefault(
                victim.getUUID(),
                0L
        )) {
            return false;
        }

        float safeAmount =
                Math.max(
                        0.0F,
                        amount
                );

        if (safeAmount <= 0.0F) {
            return false;
        }

        LAST_ANY_IMPACT.put(
                victim.getUUID(),
                gameTime
        );

        LAST_PASSIVE_DECAY.put(
                victim.getUUID(),
                gameTime
        );

        float newImpact =
                Math.min(
                        MAX_IMPACT,
                        getImpact(victim.getUUID())
                                + safeAmount
                );

        IMPACT.put(
                victim.getUUID(),
                newImpact
        );

        if (newImpact >= MAX_IMPACT) {
            knockDown(victim);
            return true;
        }

        int percent =
                Math.round(
                        newImpact
                                / MAX_IMPACT
                                * 100.0F
                );

        victim.displayClientMessage(
                Component.literal(
                        "§6Impacto acumulado: §f"
                                + percent
                                + "%"
                ),
                true
        );

        return true;
    }

    public static boolean addNormalPlayerHit(
            ServerPlayer victim,
            ServerPlayer attacker,
            float finalDamage
    ) {
        long gameTime =
                victim.level().getGameTime();

        float amount =
                BASE_IMPACT_PER_HIT
                        + Math.max(
                        0.0F,
                        finalDamage
                )
                        * IMPACT_PER_DAMAGE_POINT;

        amount =
                Math.min(
                        MAX_IMPACT_PER_NORMAL_HIT,
                        amount
                );

        long previousHit =
                LAST_NORMAL_HIT.getOrDefault(
                        victim.getUUID(),
                        Long.MIN_VALUE
                );

        if (gameTime - previousHit
                <= RAPID_HIT_WINDOW_TICKS) {
            amount *=
                    RAPID_HIT_MULTIPLIER;
        }

        LAST_NORMAL_HIT.put(
                victim.getUUID(),
                gameTime
        );

        ImpactWindow window =
                NORMAL_WINDOWS.computeIfAbsent(
                        victim.getUUID(),
                        ignored -> new ImpactWindow(
                                gameTime
                        )
                );

        if (gameTime - window.startedAt
                >= NORMAL_CAP_WINDOW_TICKS) {
            window.startedAt =
                    gameTime;
            window.accumulated =
                    0.0F;
        }

        float remaining =
                Math.max(
                        0.0F,
                        NORMAL_IMPACT_CAP_PER_WINDOW
                                - window.accumulated
                );

        amount =
                Math.min(
                        amount,
                        remaining
                );

        if (amount <= 0.0F) {
            return false;
        }

        window.accumulated +=
                amount;

        return addImpact(
                victim,
                attacker,
                amount
        );
    }

    /**
     * Impacto especial del barrido.
     * Tiene una inmunidad breve independiente para impedir
     * varios barridos simultáneos sobre la misma víctima.
     */
    public static boolean addSlideImpact(
            ServerPlayer victim,
            ServerPlayer attacker,
            float amount,
            int immunityTicks
    ) {
        long gameTime =
                victim.level().getGameTime();

        if (gameTime < SLIDE_IMMUNE_UNTIL.getOrDefault(
                victim.getUUID(),
                0L
        )) {
            return false;
        }

        boolean accepted =
                addImpact(
                        victim,
                        attacker,
                        amount
                );

        if (accepted) {
            SLIDE_IMMUNE_UNTIL.put(
                    victim.getUUID(),
                    gameTime
                            + Math.max(
                            1,
                            immunityTicks
                    )
            );
        }

        return accepted;
    }

    private static void knockDown(
            ServerPlayer player
    ) {
        if (isReferee(player)) {
            IMPACT.remove(
                    player.getUUID()
            );
            return;
        }

        long gameTime =
                player.level().getGameTime();

        IMPACT.put(
                player.getUUID(),
                MAX_IMPACT
        );

        DOWN.put(
                player.getUUID(),
                new DownData(
                        player.position(),
                        gameTime
                )
        );

        player.setDeltaMovement(
                Vec3.ZERO
        );

        player.displayClientMessage(
                Component.literal(
                        "§c§l¡DERRIBADO! §fPulsa §eG §frepetidamente."
                ),
                true
        );

        synchronizeKnockdown(
                player,
                true
        );

        synchronizeRecovery(
                player,
                0
        );
    }

    /**
     * Registra una pulsación de G.
     *
     * El cliente no decide cuándo levantarse: únicamente avisa
     * de una pulsación y el servidor controla el progreso real.
     */
    public static boolean handleRecoveryTap(
            ServerPlayer player
    ) {
        if (player == null) {
            return false;
        }

        DownData data =
                DOWN.get(
                        player.getUUID()
                );

        if (data == null) {
            return false;
        }

        /*
         * Si recibió el rol de árbitro mientras estaba en el suelo,
         * se limpia el derribo inmediatamente.
         */
        if (isReferee(player)) {
            forceClearDownState(
                    player,
                    false
            );
            return false;
        }

        long gameTime =
                player.level().getGameTime();

        if (gameTime - data.lastAcceptedTapGameTime
                < MIN_TICKS_BETWEEN_TAPS) {
            return false;
        }

        data.lastAcceptedTapGameTime =
                gameTime;

        data.lastTapGameTime =
                gameTime;

        data.recovery =
                Math.min(
                        RECOVERY_MAX,
                        data.recovery
                                + RECOVERY_PER_TAP
                );

        synchronizeRecovery(
                player,
                data.recovery
        );

        if (data.recovery >= RECOVERY_MAX) {
            completeStandUp(
                    player
            );
        }

        return true;
    }

    private static void completeStandUp(
            ServerPlayer player
    ) {
        if (DOWN.remove(
                player.getUUID()
        ) == null) {
            return;
        }

        IMPACT.remove(
                player.getUUID()
        );

        IMMUNE_UNTIL.put(
                player.getUUID(),
                player.level().getGameTime()
                        + STAND_UP_IMMUNITY_TICKS
        );

        player.setDeltaMovement(
                Vec3.ZERO
        );

        player.displayClientMessage(
                Component.literal(
                        "§aTe levantaste. Tu acumulador volvió a cero."
                ),
                true
        );

        synchronizeRecovery(
                player,
                0
        );

        synchronizeKnockdown(
                player,
                false
        );
    }

    public static void tick(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, DownData>> iterator =
                DOWN.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, DownData> entry =
                    iterator.next();

            ServerPlayer player =
                    server.getPlayerList().getPlayer(
                            entry.getKey()
                    );

            if (player == null) {
                continue;
            }

            /*
             * El árbitro es inmune. Si recibe el rol estando
             * derribado, recupera el control en el siguiente tick.
             */
            if (isReferee(player)) {
                IMPACT.remove(
                        player.getUUID()
                );

                IMMUNE_UNTIL.remove(
                        player.getUUID()
                );

                synchronizeRecovery(
                        player,
                        0
                );

                synchronizeKnockdown(
                        player,
                        false
                );

                iterator.remove();
                continue;
            }

            if (!player.isAlive()
                    || RefereeCardManager.isExpelled(
                    player.getUUID()
            )) {
                IMPACT.remove(
                        player.getUUID()
                );

                synchronizeRecovery(
                        player,
                        0
                );

                synchronizeKnockdown(
                        player,
                        false
                );

                iterator.remove();
                continue;
            }

            DownData data =
                    entry.getValue();

            long gameTime =
                    player.level().getGameTime();

            /*
             * Si deja de pulsar G, después de una pequeña gracia
             * la barra comienza a bajar.
             */
            if (data.recovery > 0
                    && gameTime - data.lastTapGameTime
                    > RECOVERY_DECAY_GRACE_TICKS
                    && gameTime - data.lastDecayGameTime
                    >= RECOVERY_DECAY_INTERVAL_TICKS) {

                data.lastDecayGameTime =
                        gameTime;

                data.recovery =
                        Math.max(
                                0,
                                data.recovery
                                        - RECOVERY_DECAY_PER_INTERVAL
                        );

                synchronizeRecovery(
                        player,
                        data.recovery
                );
            }

            Vec3 anchor =
                    data.anchor;

            /*
             * Puede girar la cámara, pero no desplazarse ni saltar.
             */
            if (player.position().distanceToSqr(anchor)
                    > 0.0004D) {
                player.teleportTo(
                        anchor.x,
                        anchor.y,
                        anchor.z
                );
            }

            player.setDeltaMovement(
                    Vec3.ZERO
            );

            player.fallDistance = 0.0F;
        }

        /*
         * Recuperación pasiva: después de 3 segundos sin recibir
         * impacto, el acumulador baja 5 puntos por segundo.
         */
        Iterator<Map.Entry<UUID, Float>> impactIterator =
                IMPACT.entrySet().iterator();

        while (impactIterator.hasNext()) {
            Map.Entry<UUID, Float> entry =
                    impactIterator.next();

            UUID playerId =
                    entry.getKey();

            if (DOWN.containsKey(playerId)) {
                continue;
            }

            ServerPlayer player =
                    server.getPlayerList().getPlayer(
                            playerId
                    );

            if (player == null) {
                continue;
            }

            long gameTime =
                    player.level().getGameTime();

            long lastImpact =
                    LAST_ANY_IMPACT.getOrDefault(
                            playerId,
                            gameTime
                    );

            long lastDecay =
                    LAST_PASSIVE_DECAY.getOrDefault(
                            playerId,
                            gameTime
                    );

            if (gameTime - lastImpact
                    < PASSIVE_DECAY_DELAY_TICKS
                    || gameTime - lastDecay
                    < PASSIVE_DECAY_INTERVAL_TICKS) {
                continue;
            }

            float newValue =
                    Math.max(
                            0.0F,
                            entry.getValue()
                                    - PASSIVE_DECAY_AMOUNT
                    );

            LAST_PASSIVE_DECAY.put(
                    playerId,
                    gameTime
            );

            if (newValue <= 0.0F) {
                impactIterator.remove();
                LAST_NORMAL_HIT.remove(playerId);
                NORMAL_WINDOWS.remove(playerId);
                LAST_ANY_IMPACT.remove(playerId);
                LAST_PASSIVE_DECAY.remove(playerId);
            } else {
                entry.setValue(newValue);
            }
        }
    }

    public static float getImpact(
            UUID playerId
    ) {
        return IMPACT.getOrDefault(
                playerId,
                0.0F
        );
    }

    public static boolean isKnockedDown(
            UUID playerId
    ) {
        return DOWN.containsKey(
                playerId
        );
    }

    public static void handleLogin(
            ServerPlayer player
    ) {
        DownData data =
                DOWN.get(
                        player.getUUID()
                );

        if (data == null) {
            return;
        }

        if (isReferee(player)) {
            forceClearDownState(
                    player,
                    false
            );
            return;
        }

        synchronizeKnockdown(
                player,
                true
        );

        synchronizeRecovery(
                player,
                data.recovery
        );
    }

    public static void clearPlayer(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        IMPACT.remove(
                player.getUUID()
        );

        IMMUNE_UNTIL.remove(
                player.getUUID()
        );

        LAST_NORMAL_HIT.remove(
                player.getUUID()
        );

        NORMAL_WINDOWS.remove(
                player.getUUID()
        );

        LAST_ANY_IMPACT.remove(
                player.getUUID()
        );

        LAST_PASSIVE_DECAY.remove(
                player.getUUID()
        );

        SLIDE_IMMUNE_UNTIL.remove(
                player.getUUID()
        );

        if (DOWN.remove(
                player.getUUID()
        ) != null) {

            synchronizeRecovery(
                    player,
                    0
            );

            synchronizeKnockdown(
                    player,
                    false
            );
        }
    }

    private static void forceClearDownState(
            ServerPlayer player,
            boolean grantImmunity
    ) {
        boolean wasDown =
                DOWN.remove(
                        player.getUUID()
                ) != null;

        IMPACT.remove(
                player.getUUID()
        );

        if (grantImmunity) {
            IMMUNE_UNTIL.put(
                    player.getUUID(),
                    player.level().getGameTime()
                            + STAND_UP_IMMUNITY_TICKS
            );
        } else {
            IMMUNE_UNTIL.remove(
                    player.getUUID()
            );
        }

        if (!wasDown) {
            return;
        }

        player.setDeltaMovement(
                Vec3.ZERO
        );

        synchronizeRecovery(
                player,
                0
        );

        synchronizeKnockdown(
                player,
                false
        );
    }

    private static boolean isReferee(
            ServerPlayer player
    ) {
        MinecraftServer server =
                player.getServer();

        return server != null
                && SoccerMatchManager.isReferee(
                server,
                player.getUUID()
        );
    }

    private static void synchronizeKnockdown(
            ServerPlayer player,
            boolean knockedDown
    ) {
        PacketDistributor.sendToAllPlayers(
                new KnockdownStatePayload(
                        player.getId(),
                        knockedDown
                )
        );
    }

    private static void synchronizeRecovery(
            ServerPlayer player,
            int progress
    ) {
        PacketDistributor.sendToPlayer(
                player,
                new RecoveryProgressPayload(
                        Math.max(
                                0,
                                Math.min(
                                        RECOVERY_MAX,
                                        progress
                                )
                        )
                )
        );
    }

    private static final class ImpactWindow {

        private long startedAt;
        private float accumulated;

        private ImpactWindow(
                long startedAt
        ) {
            this.startedAt = startedAt;
        }
    }

    private static final class DownData {

        private final Vec3 anchor;

        private int recovery;

        private long lastTapGameTime;

        private long lastAcceptedTapGameTime;

        private long lastDecayGameTime;

        private DownData(
                Vec3 anchor,
                long gameTime
        ) {
            this.anchor = anchor;
            this.recovery = 0;
            this.lastTapGameTime = gameTime;
            this.lastAcceptedTapGameTime =
                    gameTime - MIN_TICKS_BETWEEN_TAPS;
            this.lastDecayGameTime = gameTime;
        }
    }
}
