package org.crafterscr.crafterssoccer.physics;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;

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
 * - Rebotes contra jugadores y entidades.
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
     * Rebote contra jugadores, mobs y NPC.
     */
    public static final double PLAYER_BOUNCE = 0.62D;

    /**
     * Velocidades mínimas antes de detener el balón.
     */
    public static final double STOP_HORIZONTAL_SPEED = 0.010D;
    public static final double STOP_VERTICAL_SPEED = 0.035D;

    /**
     * Velocidad máxima absoluta.
     */
    public static final double MAX_SPEED = 2.40D;

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
         * Comprobar la trayectoria completa contra
         * jugadores, mobs y otras entidades vivas.
         */
        SoccerBallEntity.PlayerCollisionResult entityCollision =
                ball.resolvePlayerCollision(
                        requestedVelocity
                );

        Vec3 movementAttempt =
                entityCollision.allowedMovement();

        Vec3 velocityAfterEntityCollision =
                entityCollision.resultingVelocity();

        Vec3 previousPosition =
                ball.position();

        /*
         * Minecraft resuelve aquí las colisiones
         * contra los bloques del mundo.
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

        Vec3 bouncedVelocity = BlockBounceResolver.resolve(
                movementAttempt,
                actualMovement,
                velocityAfterEntityCollision,
                ball.verticalCollision
        );

        Vec3 finalVelocity = SurfaceMotionResolver.resolve(
                bouncedVelocity,
                ball.onGround()
        );

        ball.setDeltaMovement(
                clampVelocity(finalVelocity)
        );

        /*
         * Sincronizar los cambios de velocidad.
         */
        if (entityCollision.collided()
                || actualMovement.lengthSqr()
                > 0.000001D) {

            ball.hasImpulse = true;
        }

        /*
         * Actualizar la rotación visual con el movimiento
         * que realmente consiguió realizar.
         */
        ball.updateVisualRolling(
                actualMovement
        );
    }

    /**
     * Limita la velocidad máxima del balón.
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
                .scale(
                        MAX_SPEED
                );
    }

    /**
     * Calcula la potencia horizontal de un tiro.
     */
    public static double getKickHorizontalPower(
            float charge
    ) {
        return KickResolver.horizontalPower(charge);
    }
}