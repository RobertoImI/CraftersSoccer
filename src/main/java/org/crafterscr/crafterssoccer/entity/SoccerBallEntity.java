package org.crafterscr.crafterssoccer.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.crafterscr.crafterssoccer.physics.SoccerBallPhysics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Entidad física del balón de fútbol.
 *
 * Controla:
 * - Toques simples con las piernas.
 * - Pases y tiros con clic.
 * - Colisiones contra jugadores.
 * - Rebotes corporales.
 * - Rotación visual.
 */
public class SoccerBallEntity extends Entity {

    /**
     * Radio aproximado del balón.
     */
    public static final float BALL_RADIUS = 0.25F;

    /**
     * Distancia máxima para patear el balón con clic.
     */
    public static final double MAX_KICK_DISTANCE = 4.25D;

    /**
     * Tiempo entre contactos consecutivos de las piernas
     * del mismo jugador.
     *
     * Evita que un único contacto genere un impulso
     * durante cada tick.
     */
    private static final int LEG_TOUCH_COOLDOWN_TICKS = 5;

    /**
     * Potencia del contacto caminando.
     *
     * Es ligeramente menor que un clic rápido.
     */
    private static final double WALK_LEG_TOUCH_POWER = 0.26D;

    /**
     * Potencia del contacto corriendo.
     */
    private static final double SPRINT_LEG_TOUCH_POWER = 0.62D;

    /**
     * Potencia del contacto agachado.
     */
    private static final double CROUCH_LEG_TOUCH_POWER = 0.20D;

    /**
     * Distancia adicional para detectar las piernas.
     */
    private static final double LEG_TOUCH_MARGIN = 0.10D;

    /**
     * Altura máxima considerada como piernas.
     */
    private static final double LEG_HEIGHT = 1.05D;

    /**
     * Velocidad máxima a la que se aplica el toque automático.
     *
     * Si el balón ya viene rápido por un tiro o pase,
     * utiliza la colisión corporal normal.
     */
    private static final double MAX_LEG_TOUCH_BALL_SPEED = 0.72D;

    /**
     * Tiempo durante el cual se ignora al jugador que
     * acaba de patear mediante clic.
     *
     * Evita que el balón rebote inmediatamente contra
     * el cuerpo del propio tirador.
     */
    private static final int KICKER_IGNORE_COLLISION_TICKS = 5;

    /**
     * Rotación visual del balón.
     */
    private float previousRollDegrees;
    private float rollDegrees;
    private float rollingHeadingDegrees;

    /**
     * Último jugador que produjo un toque con las piernas.
     */
    private UUID lastLegTouchPlayer;

    /**
     * Tick del último toque con las piernas.
     */
    private long lastLegTouchGameTime;

    /**
     * Jugador ignorado brevemente después de un tiro.
     */
    private UUID temporarilyIgnoredPlayer;

    /**
     * Tick hasta el que se ignora al jugador que pateó.
     */
    private long ignoredPlayerUntilGameTime;

    public SoccerBallEntity(
            EntityType<? extends SoccerBallEntity> entityType,
            Level level
    ) {
        super(entityType, level);

        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData(
            net.minecraft.network.syncher.SynchedEntityData.Builder builder
    ) {
        /*
         * Todavía no se utilizan datos personalizados
         * sincronizados.
         */
    }

    @Override
    public void tick() {
        super.tick();

        this.previousRollDegrees =
                this.rollDegrees;

        /*
         * En el servidor, comprobar primero si un jugador
         * acaba de tocar el balón con las piernas.
         *
         * Esto solamente agrega un impulso.
         * No arrastra ni reposiciona el balón.
         */
        if (!this.level().isClientSide()) {
            applySimpleLegTouch();
        }

        /*
         * Después del toque, el balón continúa libremente
         * usando su física normal.
         */
        SoccerBallPhysics.tick(this);

        if (!this.level().isClientSide()) {
            /*
             * Eliminar el balón si cae fuera del mundo.
             */
            if (this.getY()
                    < this.level().getMinBuildHeight() - 32) {

                this.discard();
            }
        }
    }

    /**
     * Resultado de una colisión contra un jugador.
     */
    public record PlayerCollisionResult(
            Vec3 allowedMovement,
            Vec3 resultingVelocity,
            boolean collided
    ) {
    }

    /**
     * Comprueba la trayectoria completa del balón durante
     * el tick.
     *
     * Evita que pases y tiros rápidos atraviesen jugadores.
     */
    public PlayerCollisionResult resolvePlayerCollision(
            Vec3 requestedVelocity
    ) {
        if (requestedVelocity.lengthSqr()
                < 0.000001D) {

            return new PlayerCollisionResult(
                    requestedVelocity,
                    requestedVelocity,
                    false
            );
        }

        Vec3 startCenter =
                this.position().add(
                        0.0D,
                        BALL_RADIUS,
                        0.0D
                );

        Vec3 endCenter =
                startCenter.add(
                        requestedVelocity
                );

        /*
         * Área completa recorrida por el balón durante
         * este tick.
         */
        AABB sweptArea =
                this.getBoundingBox()
                        .expandTowards(
                                requestedVelocity
                        )
                        .inflate(0.15D);

        List<Player> candidates =
                this.level().getEntitiesOfClass(
                        Player.class,
                        sweptArea,
                        this::shouldCollideWithPlayer
                );

        Player closestPlayer = null;
        Vec3 closestHitPosition = null;
        AABB closestExpandedBox = null;
        Vec3 closestOverlapNormal = null;

        double closestDistanceSquared =
                Double.MAX_VALUE;

        for (Player player : candidates) {
            /*
             * Expandir la hitbox del jugador utilizando
             * el radio del balón.
             */
            AABB expandedPlayerBox =
                    player.getBoundingBox().inflate(
                            BALL_RADIUS * 0.92D
                    );

            if (expandedPlayerBox.intersects(
                    this.getBoundingBox()
            )) {
                Vec3 overlapNormal =
                        calculateSeparationNormal(
                                expandedPlayerBox,
                                startCenter,
                                requestedVelocity
                        );

                closestPlayer =
                        player;

                closestHitPosition =
                        startCenter;

                closestExpandedBox =
                        expandedPlayerBox;

                closestOverlapNormal =
                        overlapNormal;

                closestDistanceSquared =
                        0.0D;

                continue;
            }

            Optional<Vec3> possibleHit =
                    expandedPlayerBox.clip(
                            startCenter,
                            endCenter
                    );

            if (possibleHit.isEmpty()) {
                continue;
            }

            Vec3 hitPosition =
                    possibleHit.get();

            double distanceSquared =
                    startCenter.distanceToSqr(
                            hitPosition
                    );

            if (distanceSquared
                    < closestDistanceSquared) {

                closestDistanceSquared =
                        distanceSquared;

                closestPlayer =
                        player;

                closestHitPosition =
                        hitPosition;

                closestExpandedBox =
                        expandedPlayerBox;

                closestOverlapNormal =
                        null;
            }
        }

        if (closestPlayer == null
                || closestHitPosition == null
                || closestExpandedBox == null) {

            return new PlayerCollisionResult(
                    requestedVelocity,
                    requestedVelocity,
                    false
            );
        }

        Vec3 collisionNormal =
                closestOverlapNormal != null
                        ? closestOverlapNormal
                        : calculateCollisionNormal(
                                closestExpandedBox,
                                closestHitPosition
                        );

        /*
         * Permitir que el balón llegue hasta el punto
         * de impacto, pero sin atravesar al jugador.
         */
        Vec3 allowedMovement =
                closestHitPosition
                        .subtract(startCenter)
                        .add(
                                collisionNormal.scale(
                                        0.015D
                                )
                        );

        if (closestDistanceSquared == 0.0D
                && allowedMovement.dot(requestedVelocity) < 0.0D) {

            allowedMovement =
                    collisionNormal.scale(
                            0.015D
                    );
        }

        double velocityDotNormal =
                requestedVelocity.dot(
                        collisionNormal
                );

        Vec3 reflectedVelocity =
                requestedVelocity;

        /*
         * Reflejar la velocidad cuando el balón avanza
         * hacia dentro del jugador.
         */
        if (velocityDotNormal < 0.0D) {
            reflectedVelocity =
                    requestedVelocity.subtract(
                            collisionNormal.scale(
                                    (1.0D
                                            + SoccerBallPhysics.PLAYER_BOUNCE)
                                            * velocityDotNormal
                            )
                    );
        }

        Vec3 playerMovement =
                closestPlayer.getDeltaMovement();

        Vec3 playerHorizontalMovement =
                playerMovement.multiply(
                        1.0D,
                        0.0D,
                        1.0D
                );

        double playerHorizontalSpeed =
                playerHorizontalMovement.length();

        boolean impactAtLegHeight =
                startCenter.y
                        <= closestPlayer.getY()
                        + LEG_HEIGHT;

        if (impactAtLegHeight
                && playerHorizontalSpeed > 0.012D) {
            /*
             * Un contacto con las piernas debe comportarse como
             * un toque de fútbol: el balón sale hacia delante
             * según el avance real del jugador.
             *
             * No se refleja contra el cuerpo ni se queda pegado
             * a la hitbox.
             */
            Vec3 movementDirection =
                    playerHorizontalMovement.normalize();

            double forwardPower =
                    closestPlayer.isShiftKeyDown()
                            ? CROUCH_LEG_TOUCH_POWER
                            : closestPlayer.isSprinting()
                            ? SPRINT_LEG_TOUCH_POWER
                            : WALK_LEG_TOUCH_POWER;

            forwardPower += Math.min(
                    playerHorizontalSpeed * 0.45D,
                    closestPlayer.isSprinting()
                            ? 0.14D
                            : 0.06D
            );

            double carriedSpeed =
                    Math.max(
                            requestedVelocity.multiply(
                                    1.0D,
                                    0.0D,
                                    1.0D
                            ).length() * 0.58D,
                            forwardPower
                    );

            reflectedVelocity =
                    movementDirection.scale(
                            carriedSpeed
                    ).add(
                            0.0D,
                            Math.max(
                                    0.0D,
                                    requestedVelocity.y * 0.35D
                            ),
                            0.0D
                    );

        } else if (playerHorizontalSpeed > 0.012D) {
            /*
             * Si el cuerpo alcanza el balón, también lo empuja
             * hacia delante en vez de producir un rebote blando
             * tipo slime o una sensación pegajosa.
             */
            Vec3 movementDirection =
                    playerHorizontalMovement.normalize();

            double bodyPushPower =
                    Math.max(
                            playerHorizontalSpeed * 1.15D,
                            0.18D
                    );

            reflectedVelocity =
                    movementDirection.scale(
                            bodyPushPower
                    ).add(
                            0.0D,
                            Math.max(
                                    0.0D,
                                    requestedVelocity.y * 0.25D
                            ),
                            0.0D
                    );

        } else {
            /*
             * Jugador quieto: separar sin sonido y con un rebote
             * seco muy pequeño para evitar que el balón quede
             * dentro de la hitbox.
             */
            reflectedVelocity =
                    collisionNormal.scale(
                            0.08D
                    ).add(
                            0.0D,
                            Math.max(
                                    0.0D,
                                    requestedVelocity.y * 0.25D
                            ),
                            0.0D
                    );
        }

        reflectedVelocity =
                SoccerBallPhysics.clampVelocity(
                        reflectedVelocity
                );

        /*
         * Los contactos con jugadores no reproducen sonido.
         * El balón debe sentirse como una pelota de fútbol, no
         * como un slime.
         */
        return new PlayerCollisionResult(
                allowedMovement,
                reflectedVelocity,
                true
        );
    }

    /**
     * Determina si el balón debe colisionar contra
     * un jugador.
     */
    private boolean shouldCollideWithPlayer(
            Player player
    ) {
        if (!player.isAlive()
                || player.isSpectator()) {

            return false;
        }

        long gameTime =
                this.level().getGameTime();

        /*
         * Ignorar brevemente solamente al jugador que
         * acaba de ejecutar un tiro con clic.
         */
        if (this.temporarilyIgnoredPlayer != null
                && player.getUUID().equals(
                this.temporarilyIgnoredPlayer
        )
                && gameTime
                <= this.ignoredPlayerUntilGameTime) {

            return false;
        }

        return true;
    }

    /**
     * Calcula qué cara de la hitbox del jugador fue golpeada.
     */
    private static Vec3 calculateCollisionNormal(
            AABB box,
            Vec3 hitPosition
    ) {
        double distanceMinX =
                Math.abs(
                        hitPosition.x - box.minX
                );

        double distanceMaxX =
                Math.abs(
                        hitPosition.x - box.maxX
                );

        double distanceMinY =
                Math.abs(
                        hitPosition.y - box.minY
                );

        double distanceMaxY =
                Math.abs(
                        hitPosition.y - box.maxY
                );

        double distanceMinZ =
                Math.abs(
                        hitPosition.z - box.minZ
                );

        double distanceMaxZ =
                Math.abs(
                        hitPosition.z - box.maxZ
                );

        double minimumDistance =
                distanceMinX;

        Vec3 normal =
                new Vec3(
                        -1.0D,
                        0.0D,
                        0.0D
                );

        if (distanceMaxX < minimumDistance) {
            minimumDistance = distanceMaxX;

            normal = new Vec3(
                    1.0D,
                    0.0D,
                    0.0D
            );
        }

        if (distanceMinY < minimumDistance) {
            minimumDistance = distanceMinY;

            normal = new Vec3(
                    0.0D,
                    -1.0D,
                    0.0D
            );
        }

        if (distanceMaxY < minimumDistance) {
            minimumDistance = distanceMaxY;

            normal = new Vec3(
                    0.0D,
                    1.0D,
                    0.0D
            );
        }

        if (distanceMinZ < minimumDistance) {
            minimumDistance = distanceMinZ;

            normal = new Vec3(
                    0.0D,
                    0.0D,
                    -1.0D
            );
        }

        if (distanceMaxZ < minimumDistance) {
            normal = new Vec3(
                    0.0D,
                    0.0D,
                    1.0D
            );
        }

        return normal;
    }

    /**
     * Calcula una normal estable cuando el balón ya está
     * tocando la hitbox expandida del jugador al iniciar
     * el tick.
     */
    private static Vec3 calculateSeparationNormal(
            AABB box,
            Vec3 ballCenter,
            Vec3 requestedVelocity
    ) {
        double centerX =
                (box.minX + box.maxX) * 0.5D;

        double centerZ =
                (box.minZ + box.maxZ) * 0.5D;

        Vec3 fromPlayerToBall =
                new Vec3(
                        ballCenter.x - centerX,
                        0.0D,
                        ballCenter.z - centerZ
                );

        if (fromPlayerToBall.lengthSqr()
                > 0.0001D) {

            return fromPlayerToBall.normalize();
        }

        Vec3 oppositeVelocity =
                requestedVelocity.multiply(
                        -1.0D,
                        0.0D,
                        -1.0D
                );

        if (oppositeVelocity.lengthSqr()
                > 0.0001D) {

            return oppositeVelocity.normalize();
        }

        return new Vec3(
                1.0D,
                0.0D,
                0.0D
        );
    }

    /**
     * Aplica un único impulso cuando un jugador toca
     * el balón con las piernas.
     *
     * No existe asistencia de regate.
     * No existe posición objetivo.
     * No existe atracción hacia el jugador.
     *
     * El impulso sale hacia la dirección horizontal
     * en la que mira el jugador, como un clic rápido.
     */
    private void applySimpleLegTouch() {
        Vec3 currentVelocity =
                this.getDeltaMovement();

        Vec3 currentHorizontalVelocity =
                currentVelocity.multiply(
                        1.0D,
                        0.0D,
                        1.0D
                );

        /*
         * Si el balón ya viene rápido, no aplicar el toque
         * automático. En ese caso debe rebotar normalmente.
         */
        if (currentHorizontalVelocity.length()
                > MAX_LEG_TOUCH_BALL_SPEED) {

            return;
        }

        /*
         * Buscar jugadores muy cerca del balón.
         */
        AABB searchArea =
                this.getBoundingBox().inflate(
                        0.20D,
                        0.14D,
                        0.20D
                );

        List<Player> touchingPlayers =
                this.level().getEntitiesOfClass(
                        Player.class,
                        searchArea,
                        player ->
                                player.isAlive()
                                        && !player.isSpectator()
                                        && isTouchingBallWithLegs(
                                        player
                                )
                );

        if (touchingPlayers.isEmpty()) {
            return;
        }

        Player player =
                touchingPlayers.stream()
                        .min(
                                Comparator.comparingDouble(
                                        this::distanceToSqr
                                )
                        )
                        .orElse(null);

        if (player == null) {
            return;
        }

        /*
         * El jugador debe estar caminando o corriendo.
         *
         * Estar quieto junto al balón no lo mueve.
         */
        Vec3 playerHorizontalMovement =
                player.getDeltaMovement()
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        double playerHorizontalSpeed =
                playerHorizontalMovement.length();

        if (playerHorizontalSpeed < 0.012D) {
            return;
        }

        /*
         * Comprobar que el jugador se esté acercando
         * aproximadamente al balón.
         *
         * Evita que el contacto se active cuando ya se
         * está alejando.
         */
        Vec3 playerToBall =
                new Vec3(
                        this.getX() - player.getX(),
                        0.0D,
                        this.getZ() - player.getZ()
                );

        if (playerToBall.lengthSqr() > 0.0001D) {
            Vec3 directionToBall =
                    playerToBall.normalize();

            Vec3 movementDirection =
                    playerHorizontalMovement.normalize();

            double movingTowardBall =
                    movementDirection.dot(
                            directionToBall
                    );

            if (movingTowardBall < 0.05D) {
                return;
            }
        }

        long gameTime =
                this.level().getGameTime();

        /*
         * Evitar múltiples impulsos durante el mismo
         * contacto físico.
         */
        if (this.lastLegTouchPlayer != null
                && player.getUUID().equals(
                this.lastLegTouchPlayer
        )
                && gameTime
                - this.lastLegTouchGameTime
                < LEG_TOUCH_COOLDOWN_TICKS) {

            return;
        }

        /*
         * El balón sale hacia delante según el avance real
         * del jugador. Así caminar empuja poco y correr
         * empuja mucho más, sin atraer ni fijar el balón.
         */
        Vec3 forwardDirection =
                playerHorizontalMovement.normalize();

        double touchPower;

        if (player.isShiftKeyDown()) {
            touchPower =
                    CROUCH_LEG_TOUCH_POWER;

        } else if (player.isSprinting()) {
            touchPower =
                    SPRINT_LEG_TOUCH_POWER;

        } else {
            touchPower =
                    WALK_LEG_TOUCH_POWER;
        }

        /*
         * La velocidad real del jugador agrega una
         * influencia pequeña.
         */
        touchPower += Math.min(
                playerHorizontalSpeed * 0.35D,
                player.isSprinting()
                        ? 0.11D
                        : 0.045D
        );

        /*
         * Sustituir directamente el movimiento horizontal.
         *
         * No se mezcla gradualmente.
         * No se dirige el balón hacia una posición objetivo.
         * No se intenta mantenerlo cerca.
         */
        Vec3 newVelocity =
                new Vec3(
                        forwardDirection.x * touchPower,
                        currentVelocity.y,
                        forwardDirection.z * touchPower
                );

        /*
         * Evitar que un contacto en el suelo produzca
         * un pequeño salto artificial.
         */
        if (this.onGround()
                && newVelocity.y < 0.0D) {

            newVelocity =
                    new Vec3(
                            newVelocity.x,
                            0.0D,
                            newVelocity.z
                    );
        }

        this.setDeltaMovement(
                SoccerBallPhysics.clampVelocity(
                        newVelocity
                )
        );

        this.hasImpulse = true;

        this.lastLegTouchPlayer =
                player.getUUID();

        this.lastLegTouchGameTime =
                gameTime;

        /*
         * Los toques automáticos con piernas son silenciosos.
         */
    }

    /**
     * Comprueba si el balón está tocando la zona baja
     * de la hitbox del jugador.
     */
    private boolean isTouchingBallWithLegs(
            Player player
    ) {
        AABB playerBox =
                player.getBoundingBox();

        AABB legBox =
                new AABB(
                        playerBox.minX,
                        playerBox.minY - 0.05D,
                        playerBox.minZ,
                        playerBox.maxX,
                        Math.min(
                                playerBox.minY
                                        + LEG_HEIGHT,
                                playerBox.maxY
                        ),
                        playerBox.maxZ
                ).inflate(
                        LEG_TOUCH_MARGIN,
                        0.04D,
                        LEG_TOUCH_MARGIN
                );

        return legBox.intersects(
                this.getBoundingBox()
        );
    }

    /**
     * Patea el balón mediante clic.
     *
     * Controles:
     *
     * - Clic rápido: toque o pase corto.
     * - Mantener clic: pase o tiro largo.
     * - Mirar arriba: elevar el balón.
     * - Mirar casi verticalmente: levantarlo mucho.
     */
    public void kick(
            ServerPlayer player,
            float charge
    ) {
        if (!this.isAlive()) {
            return;
        }

        double maximumDistanceSquared =
                MAX_KICK_DISTANCE
                        * MAX_KICK_DISTANCE;

        if (player.distanceToSqr(this)
                > maximumDistanceSquared) {

            return;
        }

        float safeCharge =
                Mth.clamp(
                        charge,
                        0.0F,
                        1.0F
                );

        Vec3 playerLook =
                player.getLookAngle()
                        .normalize();

        Vec3 horizontalForward =
                new Vec3(
                        playerLook.x,
                        0.0D,
                        playerLook.z
                );

        if (horizontalForward.lengthSqr()
                < 0.0001D) {

            horizontalForward =
                    new Vec3(
                            -Mth.sin(
                                    player.getYRot()
                                            * Mth.DEG_TO_RAD
                            ),
                            0.0D,
                            Mth.cos(
                                    player.getYRot()
                                            * Mth.DEG_TO_RAD
                            )
                    );
        }

        horizontalForward =
                horizontalForward.normalize();

        Vec3 rightDirection =
                new Vec3(
                        -horizontalForward.z,
                        0.0D,
                        horizontalForward.x
                );

        KickContact contact =
                calculateKickContact(
                        player,
                        rightDirection
                );

        double horizontalPower =
                SoccerBallPhysics
                        .getKickHorizontalPower(
                                safeCharge
                        );

        double curvedCharge =
                Math.pow(
                        safeCharge,
                        1.22D
                );

        /*
         * Desviación lateral según el punto donde
         * se golpeó el balón.
         */
        double lateralInfluence =
                0.18D
                        + curvedCharge * 0.82D;

        double lateralDeflection =
                -contact.sideContact()
                        * 0.42D
                        * lateralInfluence;

        Vec3 finalHorizontalDirection =
                horizontalForward.add(
                        rightDirection.scale(
                                lateralDeflection
                        )
                );

        if (finalHorizontalDirection.lengthSqr()
                < 0.0001D) {

            finalHorizontalDirection =
                    horizontalForward;

        } else {
            finalHorizontalDirection =
                    finalHorizontalDirection.normalize();
        }

        /*
         * Elevación según la dirección vertical
         * de la cámara.
         */
        double upwardLook =
                Mth.clamp(
                        playerLook.y,
                        0.0D,
                        0.995D
                );

        double aimedLift =
                Math.pow(
                        upwardLook,
                        0.88D
                );

        /*
         * Factor adicional para tiros casi verticales.
         */
        double extremeUpwardFactor =
                Mth.clamp(
                        (upwardLook - 0.55D)
                                / 0.40D,
                        0.0D,
                        1.0D
                );

        extremeUpwardFactor =
                extremeUpwardFactor
                        * extremeUpwardFactor
                        * (3.0D
                        - 2.0D * extremeUpwardFactor);

        double maximumCameraLift =
                0.35D
                        + curvedCharge * 0.95D;

        double cameraLift =
                aimedLift
                        * maximumCameraLift;

        double extremeVerticalLift =
                extremeUpwardFactor
                        * (0.18D
                        + curvedCharge * 0.28D);

        double baseLift =
                Mth.lerp(
                        curvedCharge,
                        0.025D,
                        0.085D
                );

        double lowerContact =
                Mth.clamp(
                        -contact.verticalContact(),
                        0.0D,
                        1.0D
                );

        double contactLift =
                lowerContact
                        * (0.04D
                        + curvedCharge * 0.14D);

        double finalVerticalPower =
                baseLift
                        + cameraLift
                        + extremeVerticalLift
                        + contactLift;

        finalVerticalPower =
                Mth.clamp(
                        finalVerticalPower,
                        0.018D,
                        1.38D
                );

        /*
         * Los tiros muy verticales pierden avance horizontal.
         */
        double highShotReduction =
                1.0D
                        - aimedLift * 0.16D
                        - extremeUpwardFactor * 0.50D;

        highShotReduction =
                Mth.clamp(
                        highShotReduction,
                        0.28D,
                        1.0D
                );

        double finalHorizontalPower =
                horizontalPower
                        * highShotReduction;

        Vec3 kickVelocity =
                finalHorizontalDirection
                        .scale(
                                finalHorizontalPower
                        )
                        .add(
                                0.0D,
                                finalVerticalPower,
                                0.0D
                        );

        this.setDeltaMovement(
                SoccerBallPhysics.clampVelocity(
                        kickVelocity
                )
        );

        this.hasImpulse = true;

        /*
         * Ignorar brevemente al tirador para evitar que
         * el balón rebote contra él inmediatamente.
         */
        this.temporarilyIgnoredPlayer =
                player.getUUID();

        this.ignoredPlayerUntilGameTime =
                this.level().getGameTime()
                        + KICKER_IGNORE_COLLISION_TICKS;

        /*
         * Evitar que el mismo movimiento produzca además
         * un toque inmediato de piernas.
         */
        this.lastLegTouchPlayer =
                player.getUUID();

        this.lastLegTouchGameTime =
                this.level().getGameTime();

        this.level().playSound(
                null,
                this.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS,
                0.82F,
                0.90F + safeCharge * 0.25F
        );
    }

    /**
     * Punto de contacto del tiro.
     */
    private record KickContact(
            double verticalContact,
            double sideContact
    ) {
    }

    /**
     * Calcula el punto donde el jugador apuntó
     * dentro del balón.
     */
    private KickContact calculateKickContact(
            ServerPlayer player,
            Vec3 rightDirection
    ) {
        Vec3 eyePosition =
                player.getEyePosition();

        Vec3 rayEnd =
                eyePosition.add(
                        player.getLookAngle()
                                .scale(
                                        MAX_KICK_DISTANCE
                                                + 0.75D
                                )
                );

        AABB targetBox =
                this.getBoundingBox()
                        .inflate(0.08D);

        Optional<Vec3> possibleHit =
                targetBox.clip(
                        eyePosition,
                        rayEnd
                );

        /*
         * Cuando el jugador levanta la cámara durante
         * la carga, el rayo puede dejar de tocar el balón.
         */
        if (possibleHit.isEmpty()) {
            return new KickContact(
                    0.0D,
                    0.0D
            );
        }

        Vec3 ballCenter =
                this.position().add(
                        0.0D,
                        BALL_RADIUS,
                        0.0D
                );

        Vec3 relativeHit =
                possibleHit.get()
                        .subtract(
                                ballCenter
                        );

        double verticalContact =
                Mth.clamp(
                        relativeHit.y
                                / BALL_RADIUS,
                        -1.0D,
                        1.0D
                );

        double sideContact =
                Mth.clamp(
                        relativeHit.dot(
                                rightDirection
                        ) / BALL_RADIUS,
                        -1.0D,
                        1.0D
                );

        return new KickContact(
                verticalContact,
                sideContact
        );
    }

    /**
     * Actualiza la rotación visual según la distancia
     * recorrida.
     */
    public void updateVisualRolling(
            Vec3 actualMovement
    ) {
        double horizontalDistance =
                Math.sqrt(
                        actualMovement.x * actualMovement.x
                                + actualMovement.z * actualMovement.z
                );

        if (horizontalDistance
                < 0.00001D) {

            return;
        }

        float addedDegrees =
                (float) Math.toDegrees(
                        horizontalDistance
                                / BALL_RADIUS
                );

        this.rollDegrees =
                (this.rollDegrees
                        + addedDegrees)
                        % 360.0F;

        this.rollingHeadingDegrees =
                (float) Math.toDegrees(
                        Math.atan2(
                                actualMovement.x,
                                actualMovement.z
                        )
                );
    }

    public float getInterpolatedRoll(
            float partialTick
    ) {
        return Mth.lerp(
                partialTick,
                this.previousRollDegrees,
                this.rollDegrees
        );
    }

    public float getRollingHeadingDegrees() {
        return this.rollingHeadingDegrees;
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        this.rollDegrees =
                tag.getFloat(
                        "RollDegrees"
                );

        this.previousRollDegrees =
                this.rollDegrees;

        this.rollingHeadingDegrees =
                tag.getFloat(
                        "RollingHeading"
                );
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putFloat(
                "RollDegrees",
                this.rollDegrees
        );

        tag.putFloat(
                "RollingHeading",
                this.rollingHeadingDegrees
        );
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        return false;
    }

    @Override
    public void push(
            Entity entity
    ) {
        /*
         * El mod controla manualmente las colisiones.
         *
         * No utilizar el empuje genérico de Minecraft,
         * porque afectaría también al jugador.
         */
    }
}
