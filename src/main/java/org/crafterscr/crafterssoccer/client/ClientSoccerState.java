package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.network.KickBallPayload;
import org.crafterscr.crafterssoccer.network.GoalkeeperActionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;

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

        PacketDistributor.sendToServer(
                new KickBallPayload(
                        targetBallId,
                        finalCharge
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
    }
}