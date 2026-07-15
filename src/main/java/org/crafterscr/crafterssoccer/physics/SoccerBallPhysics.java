package org.crafterscr.crafterssoccer.physics;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Sistema central de físicas del balón.
 *
 * Controla:
 * - Gravedad.
 * - Fricción.
 * - Resistencia del aire.
 * - Rebotes contra bloques.
 * - Colisiones contra jugadores.
 * - Potencia de pases y tiros.
 */
public final class SoccerBallPhysics {

    /**
     * Fuerza de gravedad.
     */
    public static final double GRAVITY = 0.045D;

    /**
     * Resistencia del aire.
     */
    public static final double AIR_DRAG = 0.985D;

    /**
     * Fricción mientras rueda por el suelo.
     */
    public static final double GROUND_FRICTION = 0.916D;

    /**
     * Rebote contra el suelo.
     */
    public static final double FLOOR_BOUNCE = 0.48D;

    /**
     * Rebote contra paredes y techo.
     */
    public static final double WALL_BOUNCE = 0.65D;

    /**
     * Rebote contra jugadores.
     */
    public static final double PLAYER_BOUNCE = 0.55D;

    /**
     * Velocidades mínimas antes de detener el balón.
     */
    public static final double STOP_HORIZONTAL_SPEED = 0.010D;
    public static final double STOP_VERTICAL_SPEED = 0.035D;

    /**
     * Velocidad máxima absoluta.
     */
    public static final double MAX_SPEED = 2.40D;

    /**
     * Fuerza mínima de un clic rápido.
     */
    private static final double MIN_KICK_POWER = 0.38D;

    /**
     * Fuerza máxima horizontal de un tiro cargado.
     */
    private static final double MAX_KICK_POWER = 1.45D;

    private SoccerBallPhysics() {
    }

    /**
     * Ejecuta un tick completo de física.
     */
    public static void tick(
            SoccerBallEntity ball
    ) {
        Vec3 requestedVelocity =
                ball.getDeltaMovement();

        /*
         * Aplicar gravedad cuando el balón está en el aire.
         */
        if (!ball.onGround()) {
            requestedVelocity =
                    requestedVelocity.add(
                            0.0D,
                            -GRAVITY,
                            0.0D
                    );
        }

        requestedVelocity =
                clampVelocity(
                        requestedVelocity
                );

        /*
         * Comprobar toda la trayectoria contra jugadores.
         *
         * Esto impide que tiros y pases rápidos atraviesen
         * sus hitboxes.
         */
        SoccerBallEntity.PlayerCollisionResult playerCollision =
                ball.resolvePlayerCollision(
                        requestedVelocity
                );

        Vec3 movementAttempt =
                playerCollision.allowedMovement();

        Vec3 velocityAfterPlayerCollision =
                playerCollision.resultingVelocity();

        Vec3 previousPosition =
                ball.position();

        /*
         * Minecraft resuelve las colisiones contra bloques.
         */
        ball.move(
                MoverType.SELF,
                movementAttempt
        );

        Vec3 actualMovement =
                ball.position()
                        .subtract(
                                previousPosition
                        );

        double velocityX =
                velocityAfterPlayerCollision.x;

        double velocityY =
                velocityAfterPlayerCollision.y;

        double velocityZ =
                velocityAfterPlayerCollision.z;

        /*
         * Comprobar si un bloque impidió el movimiento
         * en el eje X.
         */
        boolean blockedX =
                Math.abs(movementAttempt.x) > 0.001D
                        && Math.abs(actualMovement.x)
                        < Math.abs(movementAttempt.x) * 0.35D;

        /*
         * Comprobar si un bloque impidió el movimiento
         * en el eje Z.
         */
        boolean blockedZ =
                Math.abs(movementAttempt.z) > 0.001D
                        && Math.abs(actualMovement.z)
                        < Math.abs(movementAttempt.z) * 0.35D;

        /*
         * Rebote contra paredes.
         */
        if (blockedX) {
            velocityX =
                    -velocityAfterPlayerCollision.x
                            * WALL_BOUNCE;
        }

        if (blockedZ) {
            velocityZ =
                    -velocityAfterPlayerCollision.z
                            * WALL_BOUNCE;
        }

        /*
         * Rebote contra suelo o techo.
         */
        if (ball.verticalCollision) {
            if (velocityAfterPlayerCollision.y
                    < -STOP_VERTICAL_SPEED) {

                /*
                 * Rebote contra el suelo.
                 */
                velocityY =
                        -velocityAfterPlayerCollision.y
                                * FLOOR_BOUNCE;

            } else if (velocityAfterPlayerCollision.y
                    > STOP_VERTICAL_SPEED) {

                /*
                 * Rebote contra el techo.
                 */
                velocityY =
                        -velocityAfterPlayerCollision.y
                                * WALL_BOUNCE;

            } else {
                velocityY = 0.0D;
            }
        }

        /*
         * Aplicar fricción o resistencia del aire.
         */
        if (ball.onGround()) {
            velocityX *= GROUND_FRICTION;
            velocityZ *= GROUND_FRICTION;

        } else {
            velocityX *= AIR_DRAG;
            velocityY *= AIR_DRAG;
            velocityZ *= AIR_DRAG;
        }

        /*
         * Detener velocidades horizontales demasiado pequeñas.
         */
        if (ball.onGround()
                && Math.abs(velocityX)
                < STOP_HORIZONTAL_SPEED) {

            velocityX = 0.0D;
        }

        if (ball.onGround()
                && Math.abs(velocityZ)
                < STOP_HORIZONTAL_SPEED) {

            velocityZ = 0.0D;
        }

        /*
         * Detener pequeños rebotes verticales.
         */
        if (ball.onGround()
                && Math.abs(velocityY)
                < STOP_VERTICAL_SPEED) {

            velocityY = 0.0D;
        }

        ball.setDeltaMovement(
                clampVelocity(
                        new Vec3(
                                velocityX,
                                velocityY,
                                velocityZ
                        )
                )
        );

        /*
         * Indicar al servidor que la velocidad cambió.
         */
        if (playerCollision.collided()
                || actualMovement.lengthSqr()
                > 0.000001D) {

            ball.hasImpulse = true;
        }

        /*
         * Actualizar la rotación visual utilizando
         * el movimiento real.
         */
        ball.updateVisualRolling(
                actualMovement
        );
    }

    /**
     * Limita la velocidad máxima absoluta.
     */
    public static Vec3 clampVelocity(
            Vec3 velocity
    ) {
        double speed =
                velocity.length();

        if (speed <= MAX_SPEED) {
            return velocity;
        }

        return velocity.normalize()
                .scale(MAX_SPEED);
    }

    /**
     * Calcula la potencia horizontal de un tiro.
     *
     * La curva diferencia claramente entre:
     * - Clic rápido.
     * - Pase corto.
     * - Pase largo.
     * - Tiro completamente cargado.
     */
    public static double getKickHorizontalPower(
            float charge
    ) {
        float safeCharge =
                Mth.clamp(
                        charge,
                        0.0F,
                        1.0F
                );

        double curvedCharge =
                Math.pow(
                        safeCharge,
                        1.85D
                );

        return Mth.lerp(
                curvedCharge,
                MIN_KICK_POWER,
                MAX_KICK_POWER
        );
    }
}