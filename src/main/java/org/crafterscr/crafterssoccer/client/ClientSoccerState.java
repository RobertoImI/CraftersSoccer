package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.network.KickBallPayload;
import org.crafterscr.crafterssoccer.network.GoalkeeperActionPayload;
import org.crafterscr.crafterssoccer.network.RefereeBallActionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Controla el estado local del tiro.
 *
 * Permite:
 * - Toques rápidos con un clic.
 * - Pases y tiros manteniendo el clic.
 * - Levantar la cámara mientras se carga.
 */
public final class ClientSoccerState {

    /**
     * Tiempo para alcanzar el 100% de potencia.
     *
     * 24 ticks son aproximadamente 1.2 segundos.
     * Esto hace que el juego sea más rápido y fluido.
     */
    public static final int MAX_CHARGE_TICKS = 24;

    /**
     * Hasta este tick el clic se interpreta como pase.
     * Desde el tick siguiente comienza el tiro cargado.
     */
    public static final int PASS_MAX_TICKS = 6;

    private static final double PASS_ASSIST_DISTANCE = 25.0D;
    private static final double PASS_ASSIST_DOT =
            Math.cos(
                    Math.toRadians(
                            22.0D
                    )
            );

    /**
     * Indica si actualmente se está cargando un tiro.
     */
    private static boolean charging;

    /**
     * Ticks acumulados durante la carga.
     */
    private static int chargeTicks;

    /**
     * ID del balón que fue seleccionado al comenzar la carga.
     */
    private static int targetBallId = -1;

    /**
     * Evita comenzar otra carga antes de soltar completamente
     * el botón después de un tiro.
     */
    private static boolean waitForRelease;

    private static boolean goalkeeperUseWasDown;

    /**
     * Estado local del clic derecho usado por el árbitro.
     */
    private static boolean refereeUseWasDown;
    private static int refereeTargetBallId = -1;

    private ClientSoccerState() {
    }

    /**
     * Actualización principal del cliente.
     */
    public static void clientTick(
            Minecraft minecraft
    ) {
        /*
         * Reiniciar al salir del mundo.
         */
        if (minecraft.player == null
                || minecraft.level == null) {
            resetEverything();
            return;
        }

        /*
         * No se puede patear ni agarrar el balón mientras
         * está derribado o ejecutando un barrido.
         */
        if (ClientKnockdownState.isLocalPlayerDown()
                || ClientSlideState.isLocalPlayerSliding()) {
            resetEverything();
            return;
        }

        handleRefereeBallUse(minecraft);
        handleGoalkeeperUse(minecraft);

        boolean attackButtonDown =
                minecraft.options.keyAttack.isDown();

        /*
         * Esperar a que el clic se suelte completamente después
         * de haber realizado un tiro.
         */
        if (waitForRelease) {
            if (!attackButtonDown) {
                waitForRelease = false;
            }

            return;
        }

        /*
         * Intentar comenzar una nueva carga.
         */
        if (!charging) {
            tryStartCharging(
                    minecraft,
                    attackButtonDown
            );

            return;
        }

        /*
         * Continuar cargando.
         */
        if (attackButtonDown) {
            continueCharging(minecraft);
            return;
        }

        /*
         * El jugador soltó el botón.
         */
        releaseKick();
    }

    /**
     * Mantener clic derecho sobre un balón intenta agarrarlo.
     * Soltar el botón intenta dejarlo caer.
     *
     * El cliente envía la solicitud para cualquier jugador;
     * el servidor solamente la acepta si realmente es árbitro.
     */
    private static void handleRefereeBallUse(
            Minecraft minecraft
    ) {
        boolean useDown =
                minecraft.options.keyUse.isDown();

        if (useDown && !refereeUseWasDown) {
            if (minecraft.hitResult
                    instanceof EntityHitResult entityHitResult
                    && entityHitResult.getEntity()
                    instanceof SoccerBallEntity ball) {

                refereeTargetBallId =
                        ball.getId();

                PacketDistributor.sendToServer(
                        new RefereeBallActionPayload(
                                refereeTargetBallId,
                                true
                        )
                );
            }
        }

        if (!useDown
                && refereeUseWasDown
                && refereeTargetBallId >= 0) {

            PacketDistributor.sendToServer(
                    new RefereeBallActionPayload(
                            refereeTargetBallId,
                            false
                    )
            );

            refereeTargetBallId = -1;
        }

        refereeUseWasDown = useDown;
    }

    /**
     * Clic derecho del portero.
     *
     * El cliente solamente manda el ID del balón observado.
     * El servidor valida equipo, área, distancia y cooldown.
     */
    private static void handleGoalkeeperUse(
            Minecraft minecraft
    ) {
        boolean useDown =
                minecraft.options.keyUse.isDown();

        if (!ClientMatchState.isGoalkeeper()) {
            goalkeeperUseWasDown = useDown;
            return;
        }

        if (useDown && !goalkeeperUseWasDown) {
            int ballId = -1;

            if (minecraft.hitResult
                    instanceof EntityHitResult entityHitResult
                    && entityHitResult.getEntity()
                    instanceof SoccerBallEntity ball) {

                ballId = ball.getId();
            }

            /*
             * Si ya sostiene el balón, el servidor conoce
             * cuál es el oficial; aun así enviamos -1 cuando
             * no se está apuntando a una entidad.
             */
            PacketDistributor.sendToServer(
                    new GoalkeeperActionPayload(
                            ballId
                    )
            );
        }

        goalkeeperUseWasDown = useDown;
    }

    /**
     * Intenta comenzar una carga.
     */
    private static void tryStartCharging(
            Minecraft minecraft,
            boolean attackButtonDown
    ) {
        if (!attackButtonDown) {
            return;
        }

        /*
         * El jugador debe estar apuntando al balón
         * para comenzar el movimiento.
         */
        if (!(minecraft.hitResult
                instanceof EntityHitResult entityHitResult)) {
            return;
        }

        if (!(entityHitResult.getEntity()
                instanceof SoccerBallEntity ball)) {
            return;
        }

        double maximumDistanceSquared =
                SoccerBallEntity.MAX_KICK_DISTANCE
                        * SoccerBallEntity.MAX_KICK_DISTANCE;

        if (minecraft.player.distanceToSqr(ball)
                > maximumDistanceSquared) {
            return;
        }

        charging = true;
        chargeTicks = 0;
        targetBallId = ball.getId();
    }

    /**
     * Continúa aumentando la carga.
     */
    private static void continueCharging(
            Minecraft minecraft
    ) {
        chargeTicks = Math.min(
                chargeTicks + 1,
                MAX_CHARGE_TICKS
        );

        /*
         * Cancelar si el balón desaparece.
         */
        if (!(minecraft.level.getEntity(targetBallId)
                instanceof SoccerBallEntity ball)) {
            cancelCharge();
            return;
        }

        /*
         * El jugador puede dejar de mirar directamente el balón,
         * pero no puede alejarse demasiado.
         */
        double maximumDistanceSquared =
                SoccerBallEntity.MAX_KICK_DISTANCE
                        * SoccerBallEntity.MAX_KICK_DISTANCE;

        if (minecraft.player.distanceToSqr(ball)
                > maximumDistanceSquared) {
            cancelCharge();
        }
    }

    /**
     * Envía el tiro al servidor.
     */
    private static void releaseKick() {
        if (targetBallId < 0) {
            cancelCharge();
            return;
        }

        float finalCharge =
                getCharge();

        boolean passRequested =
                chargeTicks <= PASS_MAX_TICKS;

        PacketDistributor.sendToServer(
                new KickBallPayload(
                        targetBallId,
                        finalCharge,
                        passRequested
                )
        );

        charging = false;
        chargeTicks = 0;
        targetBallId = -1;
        waitForRelease = true;
    }

    /**
     * Devuelve la carga entre 0 y 1.
     */
    public static float getCharge() {
        return Math.min(
                1.0F,
                chargeTicks
                        / (float) MAX_CHARGE_TICKS
        );
    }

    public static boolean isCharging() {
        return charging;
    }

    /**
     * Devuelve true durante la ventana de clic rápido reservada al pase.
     */
    public static boolean isPassWindow() {
        return charging
                && chargeTicks <= PASS_MAX_TICKS;
    }

    /**
     * Progreso visual de la ventana de pase, de 0 a 1.
     */
    public static float getPassWindowProgress() {
        if (!charging) {
            return 0.0F;
        }

        return Math.min(
                1.0F,
                chargeTicks
                        / (float) PASS_MAX_TICKS
        );
    }

    /**
     * Indicador local aproximado del receptor que el servidor podría
     * escoger. Solo sirve para HUD: el servidor vuelve a calcular todo.
     */
    public static boolean hasLikelyPassTarget(
            Minecraft minecraft
    ) {
        if (minecraft.player == null
                || minecraft.level == null
                || !isPassWindow()) {
            return false;
        }

        String side =
                ClientMatchState.getPlayerTeamSide();

        if (!"RED".equals(side)
                && !"BLUE".equals(side)) {
            return false;
        }

        Vec3 look =
                minecraft.player.getLookAngle()
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        if (look.lengthSqr() < 0.0001D) {
            return false;
        }

        look = look.normalize();

        for (AbstractClientPlayer candidate
                : minecraft.level.players()) {

            if (candidate == minecraft.player
                    || !candidate.isAlive()
                    || candidate.isSpectator()
                    || !isSameSoccerSide(
                    candidate,
                    side
            )) {
                continue;
            }

            Vec3 toCandidate =
                    candidate.position()
                            .subtract(
                                    minecraft.player.position()
                            )
                            .multiply(
                                    1.0D,
                                    0.0D,
                                    1.0D
                            );

            double distanceSqr =
                    toCandidate.lengthSqr();

            if (distanceSqr < 0.25D
                    || distanceSqr
                    > PASS_ASSIST_DISTANCE
                    * PASS_ASSIST_DISTANCE) {
                continue;
            }

            double dot =
                    look.dot(
                            toCandidate.normalize()
                    );

            if (dot >= PASS_ASSIST_DOT) {
                return true;
            }
        }

        return false;
    }

    private static boolean isSameSoccerSide(
            AbstractClientPlayer candidate,
            String side
    ) {
        if (!(candidate.getTeam()
                instanceof PlayerTeam team)) {
            return false;
        }

        String teamName =
                team.getName();

        if ("RED".equals(side)) {
            return "csoccer_red".equals(
                    teamName
            )
                    || "csoccer_red_gk".equals(
                    teamName
            );
        }

        return "csoccer_blue".equals(
                teamName
        )
                || "csoccer_blue_gk".equals(
                teamName
        );
    }

    /**
     * Indica si el jugador está mirando un balón que puede patear.
     */
    public static boolean isLookingAtKickableBall(
            Minecraft minecraft
    ) {
        if (minecraft.player == null
                || minecraft.level == null) {
            return false;
        }

        if (!(minecraft.hitResult
                instanceof EntityHitResult entityHitResult)) {
            return false;
        }

        if (!(entityHitResult.getEntity()
                instanceof SoccerBallEntity ball)) {
            return false;
        }

        double maximumDistanceSquared =
                SoccerBallEntity.MAX_KICK_DISTANCE
                        * SoccerBallEntity.MAX_KICK_DISTANCE;

        return minecraft.player.distanceToSqr(ball)
                <= maximumDistanceSquared;
    }

    /**
     * Cancela la carga actual.
     */
    public static void cancelCharge() {
        charging = false;
        chargeTicks = 0;
        targetBallId = -1;
    }

    /**
     * Restablece todo el sistema local.
     */
    public static void resetEverything() {
        charging = false;
        chargeTicks = 0;
        targetBallId = -1;
        waitForRelease = false;
        goalkeeperUseWasDown = false;
        refereeUseWasDown = false;
        refereeTargetBallId = -1;
    }
}