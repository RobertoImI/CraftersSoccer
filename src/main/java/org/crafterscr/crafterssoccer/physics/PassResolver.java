package org.crafterscr.crafterssoccer.physics;

import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.match.SoccerMatchManager;
import org.crafterscr.crafterssoccer.match.SoccerTeamSide;
import org.crafterscr.crafterssoccer.referee.RefereeCardManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Resuelve pases rasos con asistencia suave a compañeros.
 *
 * El servidor elige al receptor; el cliente únicamente solicita
 * "pase" o "tiro". Si no existe un compañero válido en el cono,
 * el balón sigue la dirección manual del jugador.
 */
public final class PassResolver {

    public static final double MAX_ASSIST_DISTANCE = 25.0D;
    public static final double ASSIST_CONE_DEGREES = 22.0D;

    private static final double MIN_MANUAL_PASS_POWER = 0.55D;
    private static final double MAX_MANUAL_PASS_POWER = 0.95D;
    private static final double MIN_ASSISTED_PASS_POWER = 0.48D;
    private static final double MAX_ASSISTED_PASS_POWER = 2.05D;
    private static final double PASS_POWER_PER_BLOCK = 0.085D;
    private static final double DIRECTION_ASSIST = 0.75D;
    private static final double LEAD_TICKS = 6.0D;
    private static final float MAX_PASS_CHARGE = 0.30F;

    private static final double MIN_ASSIST_DOT =
            Math.cos(
                    Math.toRadians(
                            ASSIST_CONE_DEGREES
                    )
            );

    private PassResolver() {
    }

    public static boolean isValidPassRequest(
            float charge
    ) {
        return Mth.clamp(
                charge,
                0.0F,
                1.0F
        ) <= MAX_PASS_CHARGE;
    }

    public static PassSolution resolve(
            SoccerBallEntity ball,
            ServerPlayer passer,
            Vec3 manualDirection,
            float charge
    ) {
        Vec3 safeManual =
                horizontalNormalize(
                        manualDirection
                );

        if (safeManual.lengthSqr() < 0.0001D) {
            safeManual =
                    new Vec3(
                            0.0D,
                            0.0D,
                            1.0D
                    );
        }

        ServerPlayer receiver =
                findBestReceiver(
                        ball,
                        passer,
                        safeManual
                );

        double tapStrength =
                Mth.clamp(
                        charge / MAX_PASS_CHARGE,
                        0.0F,
                        1.0F
                );

        if (receiver == null) {
            double power =
                    Mth.lerp(
                            tapStrength,
                            MIN_MANUAL_PASS_POWER,
                            MAX_MANUAL_PASS_POWER
                    );

            return new PassSolution(
                    safeManual,
                    power,
                    false
            );
        }

        Vec3 receiverVelocity =
                receiver.getDeltaMovement()
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        Vec3 predictedTarget =
                receiver.position()
                        .add(
                                receiverVelocity.scale(
                                        LEAD_TICKS
                                )
                        );

        Vec3 assistedDirection =
                horizontalNormalize(
                        predictedTarget.subtract(
                                ball.position()
                        )
                );

        if (assistedDirection.lengthSqr() < 0.0001D) {
            assistedDirection = safeManual;
        }

        Vec3 blendedDirection =
                horizontalNormalize(
                        safeManual.scale(
                                1.0D - DIRECTION_ASSIST
                        ).add(
                                assistedDirection.scale(
                                        DIRECTION_ASSIST
                                )
                        )
                );

        if (blendedDirection.lengthSqr() < 0.0001D) {
            blendedDirection = assistedDirection;
        }

        double distance =
                Math.sqrt(
                        horizontalDistanceSqr(
                                ball.position(),
                                predictedTarget
                        )
                );

        /*
         * La fricción del balón reduce aproximadamente 8.4 % de la
         * velocidad por tick al rodar. Por eso la potencia de un pase
         * asistido se escala con la distancia al receptor en lugar de
         * usar una fuerza fija que se quedaría corta en pases largos.
         */
        double tapMultiplier =
                Mth.lerp(
                        tapStrength,
                        0.95D,
                        1.05D
                );

        double finalPower =
                Mth.clamp(
                        distance
                                * PASS_POWER_PER_BLOCK
                                * tapMultiplier,
                        MIN_ASSISTED_PASS_POWER,
                        MAX_ASSISTED_PASS_POWER
                );

        return new PassSolution(
                blendedDirection,
                finalPower,
                true
        );
    }

    private static ServerPlayer findBestReceiver(
            SoccerBallEntity ball,
            ServerPlayer passer,
            Vec3 manualDirection
    ) {
        MinecraftServer server =
                passer.getServer();

        if (server == null) {
            return null;
        }

        SoccerTeamSide side =
                SoccerMatchManager.getPlayerTeam(
                        server,
                        passer.getUUID()
                );

        if (side == null) {
            return null;
        }

        Set<UUID> teammates =
                side == SoccerTeamSide.RED
                        ? SoccerMatchManager.getRedPlayers(
                        server
                )
                        : SoccerMatchManager.getBluePlayers(
                        server
                );

        ServerPlayer best = null;
        double bestScore = -Double.MAX_VALUE;

        for (UUID teammateId : teammates) {
            if (teammateId.equals(
                    passer.getUUID()
            )) {
                continue;
            }

            ServerPlayer candidate =
                    server.getPlayerList()
                            .getPlayer(
                                    teammateId
                            );

            if (candidate == null
                    || candidate.level() != ball.level()
                    || !candidate.isAlive()
                    || candidate.isSpectator()
                    || RefereeCardManager.isExpelled(
                    candidate.getUUID()
            )) {
                continue;
            }

            Vec3 toCandidate =
                    candidate.position()
                            .subtract(
                                    ball.position()
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
                    > MAX_ASSIST_DISTANCE
                    * MAX_ASSIST_DISTANCE) {
                continue;
            }

            Vec3 direction =
                    toCandidate.normalize();

            double dot =
                    manualDirection.dot(
                            direction
                    );

            if (dot < MIN_ASSIST_DOT) {
                continue;
            }

            /*
             * Prioridad principal: quién está más cerca de la mira.
             * Una pequeña penalización por distancia evita escoger a un
             * compañero lejano cuando dos están prácticamente alineados.
             */
            double distance =
                    Math.sqrt(
                            distanceSqr
                    );

            double score =
                    dot
                            - distance
                            / MAX_ASSIST_DISTANCE
                            * 0.04D;

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    private static Vec3 horizontalNormalize(
            Vec3 vector
    ) {
        Vec3 horizontal =
                vector.multiply(
                        1.0D,
                        0.0D,
                        1.0D
                );

        if (horizontal.lengthSqr()
                < 0.0001D) {
            return Vec3.ZERO;
        }

        return horizontal.normalize();
    }

    private static double horizontalDistanceSqr(
            Vec3 first,
            Vec3 second
    ) {
        double x =
                first.x - second.x;

        double z =
                first.z - second.z;

        return x * x + z * z;
    }

    public record PassSolution(
            Vec3 direction,
            double power,
            boolean assisted
    ) {
    }
}
