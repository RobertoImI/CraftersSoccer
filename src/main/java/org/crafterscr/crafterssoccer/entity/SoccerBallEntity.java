package org.crafterscr.crafterssoccer.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.physics.PassResolver;
import org.crafterscr.crafterssoccer.physics.SoccerBallPhysics;
import org.crafterscr.crafterssoccer.referee.RefereeCardManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Entidad física del balón de fútbol.
 *
 * Funciones:
 * - Toque sencillo al chocar con las piernas.
 * - Mayor impulso al correr.
 * - Toque suave al agacharse.
 * - Rebote contra jugadores, mobs y NPC.
 * - Tiros y pases mediante clic.
 * - Rotación visual.
 */
public class SoccerBallEntity extends Entity {

    /**
     * Radio físico aproximado del balón.
     */
    public static final float BALL_RADIUS = 0.25F;

    /**
     * Distancia máxima para ejecutar un tiro mediante clic.
     */
    public static final double MAX_KICK_DISTANCE = 4.25D;

    /**
     * Potencia de un toque caminando.
     *
     * Es equivalente aproximadamente a un clic rápido.
     */
    private static final double WALK_LEG_TOUCH_POWER = 0.38D;

    /**
     * Potencia de un toque corriendo.
     */
    private static final double SPRINT_LEG_TOUCH_POWER = 0.58D;

    /**
     * Potencia de un toque agachado.
     */
    private static final double CROUCH_LEG_TOUCH_POWER = 0.21D;

    /**
     * Tiempo mínimo entre contactos del mismo jugador.
     */
    private static final int LEG_TOUCH_COOLDOWN_TICKS = 3;

    /**
     * Tiempo durante el cual se ignora la colisión corporal
     * del jugador que acaba de tocar el balón con las piernas.
     *
     * Esto evita que el rebote corporal anule el impulso
     * aplicado durante el mismo contacto.
     */
    private static final int LEG_TOUCH_IGNORE_TICKS = 2;

    /**
     * Tiempo durante el cual se ignora al jugador que
     * acaba de ejecutar un tiro mediante clic.
     */
    private static final int KICKER_IGNORE_TICKS = 5;

    /**
     * Altura aproximada de las piernas.
     */
    private static final double LEG_HEIGHT = 1.05D;

    /**
     * Margen alrededor de la caja de las piernas.
     */
    private static final double LEG_CONTACT_MARGIN = 0.12D;

    /**
     * Velocidad máxima del balón para aplicar un toque
     * automático de piernas.
     *
     * Un balón que viaje más rápido debe rebotar
     * normalmente contra el jugador.
     */
    private static final double MAX_LEG_TOUCH_BALL_SPEED = 0.78D;

    /**
     * Distancia alrededor del balón en la que se buscan
     * jugadores que puedan tocarlo.
     */
    private static final double PLAYER_SEARCH_DISTANCE = 1.40D;

    /**
     * Rotación visual.
     */
    private float previousRollDegrees;
    private float rollDegrees;
    private float rollingHeadingDegrees;

    /**
     * Posición que cada jugador tenía durante la última
     * actualización de este balón.
     *
     * Esto permite medir el movimiento real del jugador
     * sin depender del orden interno de ticks de Minecraft.
     */
    private final Map<UUID, Vec3> previousPlayerPositions =
            new HashMap<>();

    /**
     * Jugadores que terminaron el tick anterior tocando
     * físicamente el balón.
     *
     * Impide aplicar el impulso muchas veces durante
     * un único contacto continuo.
     */
    private final Set<UUID> playersTouchingLastTick =
            new HashSet<>();

    /**
     * Último tick en el que cada jugador produjo
     * un contacto válido de piernas.
     */
    private final Map<UUID, Long> lastLegTouchTimes =
            new HashMap<>();

    /**
     * Entidad ignorada temporalmente después de un toque
     * de piernas o un tiro.
     */
    private UUID temporarilyIgnoredEntity;

    /**
     * Tick hasta el que se ignora la entidad.
     */
    private long ignoredEntityUntilGameTime;

    /*
     * Posesión temporal del portero.
     * Solo el servidor decide quién sostiene el balón.
     */
    private UUID goalkeeperHolder;
    private long goalkeeperHoldUntilGameTime;

    /*
     * Posesión del árbitro.
     * No tiene límite de tiempo y termina únicamente cuando
     * el árbitro suelta el clic derecho o deja de ser válido.
     */
    private UUID refereeHolder;

    /**
     * Breve estabilización después de que el árbitro coloca el balón.
     * Evita que la gravedad produzca un rebote inmediato.
     */
    private long refereeSettleUntilGameTime;

    /*
     * Control temporal ganado mediante un barrido limpio.
     */
    private UUID slideController;
    private long slideControlUntilGameTime;

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
         * Todavía no se requieren datos personalizados
         * sincronizados.
         */
    }

    @Override
    public void tick() {
        super.tick();

        this.previousRollDegrees =
                this.rollDegrees;

        if (!this.level().isClientSide()
                && tickSlideControl()) {
            return;
        }

        if (!this.level().isClientSide()
                && tickRefereeHold()) {
            return;
        }

        if (!this.level().isClientSide()
                && tickGoalkeeperHold()) {
            return;
        }

        if (!this.level().isClientSide()
                && tickRefereePlacementSettle()) {
            return;
        }

        /*
         * Primero detectar las piernas moviéndose
         * contra el balón.
         */
        if (!this.level().isClientSide()) {
            detectPlayerLegTouches();
        }

        /*
         * Después dejar que el balón continúe libremente
         * con gravedad, fricción y rebotes.
         */
        SoccerBallPhysics.tick(this);

        if (!this.level().isClientSide()) {
            if (this.getY()
                    < this.level().getMinBuildHeight() - 32) {

                this.discard();
            }
        }
    }

    /**
     * Otorga control temporal del balón después de un robo
     * realizado con barrido.
     */
    public void grantSlideControl(
            ServerPlayer player,
            int maximumTicks
    ) {
        if (isHeldByReferee()
                || isHeldByGoalkeeper()) {
            return;
        }

        this.slideController =
                player.getUUID();

        this.slideControlUntilGameTime =
                this.level().getGameTime()
                        + Math.max(
                        1,
                        maximumTicks
                );

        this.setDeltaMovement(
                Vec3.ZERO
        );

        this.hasImpulse = true;
    }

    public boolean isControlledBySlide() {
        return slideController != null;
    }

    public boolean isControlledBySlide(
            UUID playerId
    ) {
        return slideController != null
                && slideController.equals(
                playerId
        );
    }

    public void releaseSlideControl() {
        this.slideController = null;
        this.slideControlUntilGameTime = 0L;
    }

    /**
     * Mantiene el balón cerca de los pies, pero con una transición
     * suave para que no parezca rígidamente pegado.
     */
    private boolean tickSlideControl() {
        if (slideController == null) {
            return false;
        }

        if (!(this.level()
                instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }

        Player found =
                serverLevel.getPlayerByUUID(
                        slideController
                );

        if (!(found instanceof ServerPlayer controller)
                || !controller.isAlive()
                || RefereeCardManager.isExpelled(
                slideController
        )
                || this.level().getGameTime()
                >= slideControlUntilGameTime) {

            releaseSlideControl();
            return false;
        }

        Vec3 forward =
                controller.getLookAngle()
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        if (forward.lengthSqr() < 0.0001D) {
            forward =
                    new Vec3(
                            0.0D,
                            0.0D,
                            1.0D
                    );
        } else {
            forward =
                    forward.normalize();
        }

        Vec3 target =
                controller.position()
                        .add(
                                forward.scale(
                                        0.72D
                                )
                        )
                        .add(
                                0.0D,
                                0.18D,
                                0.0D
                        );

        Vec3 difference =
                target.subtract(
                        this.position()
                );

        /*
         * Seguimiento elástico: avanza hacia el objetivo
         * sin teletransportarse completamente cada tick.
         */
        Vec3 assistedVelocity =
                difference.scale(
                                0.42D
                        )
                        .add(
                                controller.getDeltaMovement()
                                        .multiply(
                                                0.55D,
                                                0.0D,
                                                0.55D
                                        )
                        );

        this.setDeltaMovement(
                SoccerBallPhysics.clampVelocity(
                        assistedVelocity
                )
        );

        this.move(
                net.minecraft.world.entity.MoverType.SELF,
                this.getDeltaMovement()
        );

        this.hasImpulse = true;
        return true;
    }

    /**
     * Vincula el balón al árbitro sin límite de tiempo.
     */
    public void holdByReferee(
            ServerPlayer player
    ) {
        releaseSlideControl();

        this.refereeHolder =
                player.getUUID();

        this.setDeltaMovement(
                Vec3.ZERO
        );

        this.hasImpulse = true;
    }

    public boolean isHeldByReferee() {
        return refereeHolder != null;
    }

    public boolean isHeldByReferee(
            UUID playerId
    ) {
        return refereeHolder != null
                && refereeHolder.equals(playerId);
    }

    public UUID getRefereeHolder() {
        return refereeHolder;
    }

    /**
     * El árbitro deja caer el balón sin lanzarlo.
     */
    public void releaseFromReferee(
            ServerPlayer referee
    ) {
        this.refereeHolder = null;

        /*
         * El servidor lanza un raycast desde los ojos del árbitro.
         * El balón se coloca exactamente sobre la cara superior
         * del bloque sólido que está mirando.
         */
        Vec3 eyePosition =
                referee.getEyePosition();

        Vec3 lookDirection =
                referee.getLookAngle();

        if (lookDirection.lengthSqr() < 0.0001D) {
            lookDirection =
                    new Vec3(
                            0.0D,
                            0.0D,
                            1.0D
                    );
        } else {
            lookDirection =
                    lookDirection.normalize();
        }

        Vec3 rayEnd =
                eyePosition.add(
                        lookDirection.scale(
                                8.0D
                        )
                );

        net.minecraft.world.phys.BlockHitResult hitResult =
                this.level().clip(
                        new net.minecraft.world.level.ClipContext(
                                eyePosition,
                                rayEnd,
                                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                net.minecraft.world.level.ClipContext.Fluid.NONE,
                                referee
                        )
                );

        double targetX =
                this.getX();

        double targetY =
                this.getY();

        double targetZ =
                this.getZ();

        if (hitResult.getType()
                == net.minecraft.world.phys.HitResult.Type.BLOCK) {

            net.minecraft.core.BlockPos blockPos =
                    hitResult.getBlockPos();

            net.minecraft.world.level.block.state.BlockState state =
                    this.level().getBlockState(
                            blockPos
                    );

            net.minecraft.world.phys.shapes.VoxelShape shape =
                    state.getCollisionShape(
                            this.level(),
                            blockPos
                    );

            if (!shape.isEmpty()) {
                /*
                 * Centro horizontal del bloque observado.
                 * La altura usa la superficie real del collision shape,
                 * por lo que funciona también con losas y bloques bajos.
                 */
                targetX =
                        blockPos.getX() + 0.5D;

                targetY =
                        blockPos.getY()
                                + shape.max(
                                net.minecraft.core.Direction.Axis.Y
                        )
                                + 0.02D;

                targetZ =
                        blockPos.getZ() + 0.5D;
            }
        }

        this.setPos(
                targetX,
                targetY,
                targetZ
        );

        this.setDeltaMovement(
                Vec3.ZERO
        );

        /*
         * 20 ticks = 1 segundo completamente inmóvil.
         */
        this.refereeSettleUntilGameTime =
                this.level().getGameTime() + 20L;

        this.hasImpulse = true;
    }

    /**
     * Liberación de seguridad cuando el árbitro desaparece,
     * muere o pierde el rol.
     */
    public void releaseFromReferee() {
        this.refereeHolder = null;
        this.refereeSettleUntilGameTime =
                this.level().getGameTime() + 4L;

        this.setDeltaMovement(
                Vec3.ZERO
        );

        this.hasImpulse = true;
    }

    /**
     * Mantiene el balón completamente quieto durante un segundo
     * después de colocarlo.
     */
    private boolean tickRefereePlacementSettle() {
        if (this.level().getGameTime()
                >= refereeSettleUntilGameTime) {
            return false;
        }

        this.setDeltaMovement(
                Vec3.ZERO
        );

        this.hasImpulse = true;
        return true;
    }

    /**
     * Mantiene el balón delante del árbitro.
     *
     * No limita velocidad ni tiempo de posesión.
     */
    private boolean tickRefereeHold() {
        if (refereeHolder == null) {
            return false;
        }

        if (!(this.level()
                instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }

        Player foundPlayer =
                serverLevel.getPlayerByUUID(
                        refereeHolder
                );

        if (!(foundPlayer instanceof ServerPlayer holder)
                || !holder.isAlive()
                || !org.crafterscr.crafterssoccer.match
                .SoccerMatchManager.isReferee(
                        serverLevel.getServer(),
                        refereeHolder
                )) {

            releaseFromReferee();
            return false;
        }

        Vec3 look =
                holder.getLookAngle();

        if (look.lengthSqr() < 0.0001D) {
            look = new Vec3(
                    0.0D,
                    0.0D,
                    1.0D
            );
        } else {
            look = look.normalize();
        }

        Vec3 heldPosition =
                holder.getEyePosition()
                        .add(
                                look.scale(0.78D)
                        )
                        .add(
                                0.0D,
                                -0.46D,
                                0.0D
                        );

        this.setPos(
                heldPosition.x,
                heldPosition.y,
                heldPosition.z
        );

        this.setDeltaMovement(
                Vec3.ZERO
        );

        this.hasImpulse = true;

        return true;
    }

    /**
     * Vincula el balón al portero durante un máximo
     * de ticks determinado.
     */
    public void holdByGoalkeeper(
            ServerPlayer player,
            int maximumTicks
    ) {
        if (isHeldByReferee()) {
            return;
        }

        releaseSlideControl();

        this.goalkeeperHolder = player.getUUID();
        this.goalkeeperHoldUntilGameTime =
                this.level().getGameTime()
                        + Math.max(1, maximumTicks);

        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
    }

    public boolean isHeldByGoalkeeper() {
        return goalkeeperHolder != null;
    }

    public boolean isHeldBy(UUID playerId) {
        return goalkeeperHolder != null
                && goalkeeperHolder.equals(playerId);
    }

    public UUID getGoalkeeperHolder() {
        return goalkeeperHolder;
    }

    public void releaseFromGoalkeeper(
            Vec3 releaseVelocity
    ) {
        this.goalkeeperHolder = null;
        this.goalkeeperHoldUntilGameTime = 0L;

        this.setDeltaMovement(
                SoccerBallPhysics.clampVelocity(
                        releaseVelocity
                )
        );

        this.hasImpulse = true;
    }

    /**
     * Devuelve true cuando el balón fue actualizado
     * como balón sostenido y no debe ejecutar físicas.
     */
    private boolean tickGoalkeeperHold() {
        if (goalkeeperHolder == null) {
            return false;
        }

        if (!(this.level()
                instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }

        Player foundPlayer =
                serverLevel.getPlayerByUUID(
                        goalkeeperHolder
                );

        if (!(foundPlayer instanceof ServerPlayer holder)) {
            releaseFromGoalkeeper(Vec3.ZERO);
            return false;
        }

        if (!holder.isAlive()) {
            releaseFromGoalkeeper(Vec3.ZERO);
            return false;
        }

        Vec3 look =
                holder.getLookAngle();

        if (look.lengthSqr() < 0.0001D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            look = look.normalize();
        }

        if (this.level().getGameTime()
                >= goalkeeperHoldUntilGameTime) {

            releaseFromGoalkeeper(
                    look.scale(0.30D)
                            .add(0.0D, 0.06D, 0.0D)
            );

            return false;
        }

        Vec3 heldPosition =
                holder.getEyePosition()
                        .add(look.scale(0.72D))
                        .add(0.0D, -0.42D, 0.0D);

        this.setPos(
                heldPosition.x,
                heldPosition.y,
                heldPosition.z
        );

        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;

        return true;
    }

    /**
     * Resultado de una colisión contra una entidad.
     *
     * Se conserva el nombre anterior para no necesitar
     * cambiar SoccerBallPhysics.
     */
    public record PlayerCollisionResult(
            Vec3 allowedMovement,
            Vec3 resultingVelocity,
            boolean collided
    ) {
    }

    /**
     * Detecta el recorrido real de las piernas de jugadores
     * próximos al balón.
     *
     * Cada balón guarda la posición anterior de los jugadores.
     * De esta forma no dependemos de player.xo ni de que
     * getDeltaMovement() esté correctamente actualizado.
     */
    private void detectPlayerLegTouches() {
        Vec3 currentBallVelocity =
                this.getDeltaMovement();

        double ballHorizontalSpeed =
                currentBallVelocity.multiply(
                        1.0D,
                        0.0D,
                        1.0D
                ).length();

        AABB searchArea =
                this.getBoundingBox().inflate(
                        PLAYER_SEARCH_DISTANCE,
                        0.70D,
                        PLAYER_SEARCH_DISTANCE
                );

        List<Player> nearbyPlayers =
                this.level().getEntitiesOfClass(
                        Player.class,
                        searchArea,
                        player ->
                                player.isAlive()
                                        && !player.isSpectator()
                                        && !RefereeCardManager.isExpelled(
                                        player.getUUID()
                                )
                );

        Set<UUID> playersSeenThisTick =
                new HashSet<>();

        Set<UUID> playersTouchingNow =
                new HashSet<>();

        for (Player player : nearbyPlayers) {
            UUID playerId =
                    player.getUUID();

            playersSeenThisTick.add(
                    playerId
            );

            Vec3 currentPosition =
                    player.position();

            Vec3 previousPosition =
                    this.previousPlayerPositions.get(
                            playerId
                    );

            /*
             * La primera vez que vemos al jugador solamente
             * guardamos su posición.
             */
            if (previousPosition == null) {
                this.previousPlayerPositions.put(
                        playerId,
                        currentPosition
                );

                if (buildLegBoxAt(
                        player,
                        currentPosition
                ).intersects(
                        this.getBoundingBox()
                )) {

                    playersTouchingNow.add(
                            playerId
                    );
                }

                continue;
            }

            Vec3 playerMovement =
                    currentPosition.subtract(
                            previousPosition
                    ).multiply(
                            1.0D,
                            0.0D,
                            1.0D
                    );

            AABB previousLegBox =
                    buildLegBoxAt(
                            player,
                            previousPosition
                    );

            AABB currentLegBox =
                    buildLegBoxAt(
                            player,
                            currentPosition
                    );

            /*
             * Caja que cubre todo el recorrido de las piernas
             * entre la posición anterior y la actual.
             */
            AABB sweptLegBox =
                    previousLegBox.minmax(
                            currentLegBox
                    );

            boolean currentlyTouching =
                    currentLegBox.intersects(
                            this.getBoundingBox()
                    );

            boolean crossedBall =
                    sweptLegBox.intersects(
                            this.getBoundingBox()
                    );

            if (currentlyTouching) {
                playersTouchingNow.add(
                        playerId
                );
            }

            boolean wasAlreadyTouching =
                    this.playersTouchingLastTick.contains(
                            playerId
                    );

            if (ballHorizontalSpeed
                    <= MAX_LEG_TOUCH_BALL_SPEED
                    && crossedBall
                    && !wasAlreadyTouching) {

                tryApplyLegTouch(
                        player,
                        playerMovement
                );
            }

            this.previousPlayerPositions.put(
                    playerId,
                    currentPosition
            );
        }

        /*
         * Eliminar jugadores que dejaron de estar
         * cerca del balón.
         */
        this.previousPlayerPositions
                .keySet()
                .removeIf(
                        uuid ->
                                !playersSeenThisTick.contains(
                                        uuid
                                )
                );

        this.playersTouchingLastTick.clear();

        this.playersTouchingLastTick.addAll(
                playersTouchingNow
        );
    }

    /**
     * Construye la caja correspondiente a las piernas
     * de un jugador en una posición determinada.
     */
    private static AABB buildLegBoxAt(
            Player player,
            Vec3 position
    ) {
        double halfWidth =
                player.getBbWidth() * 0.5D;

        double minX =
                position.x - halfWidth;

        double maxX =
                position.x + halfWidth;

        double minY =
                position.y - 0.07D;

        double maxY =
                position.y
                        + Math.min(
                        LEG_HEIGHT,
                        player.getBbHeight()
                );

        double minZ =
                position.z - halfWidth;

        double maxZ =
                position.z + halfWidth;

        return new AABB(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        ).inflate(
                LEG_CONTACT_MARGIN,
                0.04D,
                LEG_CONTACT_MARGIN
        );
    }

    /**
     * Intenta aplicar un contacto de piernas.
     */
    private void tryApplyLegTouch(
            Player player,
            Vec3 playerMovement
    ) {
        double playerSpeed =
                playerMovement.length();

        /*
         * El jugador debe haberse desplazado realmente.
         */
        if (playerSpeed < 0.015D) {
            return;
        }

        Vec3 movementDirection =
                playerMovement.normalize();

        Vec3 playerToBall =
                new Vec3(
                        this.getX() - player.getX(),
                        0.0D,
                        this.getZ() - player.getZ()
                );

        /*
         * El jugador debe haberse movido hacia el balón.
         */
        if (playerToBall.lengthSqr()
                > 0.0001D) {

            Vec3 directionToBall =
                    playerToBall.normalize();

            double movementTowardBall =
                    movementDirection.dot(
                            directionToBall
                    );

            if (movementTowardBall < 0.02D) {
                return;
            }
        }

        long gameTime =
                this.level().getGameTime();

        long previousTouchTime =
                this.lastLegTouchTimes.getOrDefault(
                        player.getUUID(),
                        Long.MIN_VALUE
                );

        if (gameTime - previousTouchTime
                < LEG_TOUCH_COOLDOWN_TICKS) {

            return;
        }

        /*
         * La dirección principal es el desplazamiento real
         * del jugador.
         *
         * Una pequeña influencia de la cámara permite
         * orientar naturalmente el toque.
         */
        Vec3 lookDirection =
                player.getLookAngle()
                        .multiply(
                                1.0D,
                                0.0D,
                                1.0D
                        );

        Vec3 finalDirection =
                movementDirection;

        if (lookDirection.lengthSqr()
                > 0.0001D) {

            lookDirection =
                    lookDirection.normalize();

            finalDirection =
                    movementDirection.scale(0.88D)
                            .add(
                                    lookDirection.scale(
                                            0.12D
                                    )
                            );

            if (finalDirection.lengthSqr()
                    > 0.0001D) {

                finalDirection =
                        finalDirection.normalize();

            } else {
                finalDirection =
                        movementDirection;
            }
        }

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
         * Añadir una variación pequeña según la velocidad
         * real con la que se desplazó el jugador.
         */
        touchPower +=
                Math.min(
                        playerSpeed
                                * (player.isSprinting()
                                ? 0.35D
                                : 0.22D),
                        player.isSprinting()
                                ? 0.08D
                                : 0.04D
                );

        Vec3 currentVelocity =
                this.getDeltaMovement();

        double verticalVelocity =
                this.onGround()
                        ? 0.0D
                        : Math.max(
                        0.0D,
                        currentVelocity.y
                ) * 0.60D;

        Vec3 newVelocity =
                new Vec3(
                        finalDirection.x
                                * touchPower,
                        verticalVelocity,
                        finalDirection.z
                                * touchPower
                );

        this.setDeltaMovement(
                SoccerBallPhysics.clampVelocity(
                        newVelocity
                )
        );

        this.hasImpulse = true;

        this.lastLegTouchTimes.put(
                player.getUUID(),
                gameTime
        );

        /*
         * Evitar que el mismo contacto sea procesado
         * inmediatamente como un rebote corporal.
         */
        this.temporarilyIgnoredEntity =
                player.getUUID();

        this.ignoredEntityUntilGameTime =
                gameTime
                        + LEG_TOUCH_IGNORE_TICKS;
    }

    /**
     * Comprueba la trayectoria del balón contra jugadores,
     * mobs, aldeanos, NPC y otras entidades vivas.
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
                getBallCenter();

        Vec3 endCenter =
                startCenter.add(
                        requestedVelocity
                );

        AABB sweptArea =
                this.getBoundingBox()
                        .expandTowards(
                                requestedVelocity
                        )
                        .inflate(0.18D);

        List<LivingEntity> candidates =
                this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        sweptArea,
                        this::shouldCollideWithEntity
                );

        LivingEntity closestEntity = null;

        Vec3 closestHitPosition = null;

        AABB closestExpandedBox = null;

        Vec3 closestOverlapNormal = null;

        boolean startedOverlapping = false;

        double closestDistanceSquared =
                Double.MAX_VALUE;

        for (LivingEntity livingEntity : candidates) {
            AABB expandedEntityBox =
                    livingEntity.getBoundingBox()
                            .inflate(
                                    BALL_RADIUS * 0.94D
                            );

            boolean overlapping =
                    expandedEntityBox.contains(
                            startCenter
                    )
                            || expandedEntityBox.intersects(
                            this.getBoundingBox()
                    );

            if (overlapping) {
                double distanceSquared =
                        this.distanceToSqr(
                                livingEntity
                        );

                if (distanceSquared
                        < closestDistanceSquared) {

                    closestDistanceSquared =
                            distanceSquared;

                    closestEntity =
                            livingEntity;

                    closestHitPosition =
                            startCenter;

                    closestExpandedBox =
                            expandedEntityBox;

                    closestOverlapNormal =
                            calculateOverlapNormal(
                                    livingEntity,
                                    startCenter,
                                    requestedVelocity
                            );

                    startedOverlapping = true;
                }

                continue;
            }

            Optional<Vec3> possibleHit =
                    expandedEntityBox.clip(
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

                closestEntity =
                        livingEntity;

                closestHitPosition =
                        hitPosition;

                closestExpandedBox =
                        expandedEntityBox;

                closestOverlapNormal = null;

                startedOverlapping = false;
            }
        }

        if (closestEntity == null
                || closestHitPosition == null
                || closestExpandedBox == null) {

            return new PlayerCollisionResult(
                    requestedVelocity,
                    requestedVelocity,
                    false
            );
        }

        Vec3 collisionNormal;

        Vec3 allowedMovement;

        if (startedOverlapping
                && closestOverlapNormal != null) {

            collisionNormal =
                    closestOverlapNormal;

            allowedMovement =
                    calculateOverlapEscapeMovement(
                            closestExpandedBox,
                            startCenter,
                            collisionNormal
                    );

        } else {
            collisionNormal =
                    calculateCollisionNormal(
                            closestExpandedBox,
                            closestHitPosition
                    );

            allowedMovement =
                    closestHitPosition
                            .subtract(
                                    startCenter
                            )
                            .add(
                                    collisionNormal.scale(
                                            0.018D
                                    )
                            );
        }

        Vec3 entityMovement =
                getEntityMovement(
                        closestEntity
                );

        boolean impactAtLegHeight =
                startCenter.y
                        <= closestEntity.getY()
                        + Math.min(
                        LEG_HEIGHT,
                        closestEntity.getBbHeight()
                                * 0.65D
                );

        Vec3 resultingVelocity =
                calculateBounceVelocity(
                        requestedVelocity,
                        entityMovement,
                        collisionNormal,
                        impactAtLegHeight
                );

        return new PlayerCollisionResult(
                allowedMovement,
                SoccerBallPhysics.clampVelocity(
                        resultingVelocity
                ),
                true
        );
    }

    /**
     * Calcula el rebote contra una entidad sólida.
     */
    private static Vec3 calculateBounceVelocity(
            Vec3 ballVelocity,
            Vec3 entityVelocity,
            Vec3 collisionNormal,
            boolean impactAtLegHeight
    ) {
        Vec3 relativeVelocity =
                ballVelocity.subtract(
                        entityVelocity
                );

        double speedIntoEntity =
                relativeVelocity.dot(
                        collisionNormal
                );

        Vec3 bouncedVelocity =
                ballVelocity;

        if (speedIntoEntity < 0.0D) {
            Vec3 reflectedRelativeVelocity =
                    relativeVelocity.subtract(
                            collisionNormal.scale(
                                    (1.0D
                                            + SoccerBallPhysics.PLAYER_BOUNCE)
                                            * speedIntoEntity
                            )
                    );

            bouncedVelocity =
                    reflectedRelativeVelocity.add(
                            entityVelocity
                    );
        }

        /*
         * Si la entidad se mueve hacia el balón,
         * su movimiento también lo impulsa.
         */
        double entitySpeedTowardBall =
                entityVelocity.dot(
                        collisionNormal
                );

        if (entitySpeedTowardBall > 0.0D) {
            double additionalPower =
                    Math.min(
                            entitySpeedTowardBall
                                    * (impactAtLegHeight
                                    ? 1.45D
                                    : 0.95D),
                            impactAtLegHeight
                                    ? 0.36D
                                    : 0.22D
                    );

            double existingNormalSpeed =
                    bouncedVelocity.dot(
                            collisionNormal
                    );

            if (existingNormalSpeed
                    < additionalPower) {

                Vec3 tangentVelocity =
                        bouncedVelocity.subtract(
                                collisionNormal.scale(
                                        existingNormalSpeed
                                )
                        );

                bouncedVelocity =
                        tangentVelocity.scale(0.88D)
                                .add(
                                        collisionNormal.scale(
                                                additionalPower
                                        )
                                );
            }
        }

        /*
         * Un golpe contra el torso absorbe más energía.
         */
        if (!impactAtLegHeight) {
            bouncedVelocity =
                    new Vec3(
                            bouncedVelocity.x * 0.82D,
                            bouncedVelocity.y * 0.76D,
                            bouncedVelocity.z * 0.82D
                    );
        }

        /*
         * Evitar que un impacto en piernas clave
         * el balón contra el suelo.
         */
        if (impactAtLegHeight
                && bouncedVelocity.y < 0.0D) {

            bouncedVelocity =
                    new Vec3(
                            bouncedVelocity.x,
                            0.0D,
                            bouncedVelocity.z
                    );
        }

        /*
         * Todo impacto real debe producir un rebote visible.
         */
        if (bouncedVelocity.multiply(
                1.0D,
                0.0D,
                1.0D
        ).lengthSqr() < 0.0016D) {

            bouncedVelocity =
                    new Vec3(
                            collisionNormal.x * 0.15D,
                            Math.max(
                                    0.0D,
                                    bouncedVelocity.y
                            ),
                            collisionNormal.z * 0.15D
                    );
        }

        return bouncedVelocity;
    }

    /**
     * Decide si el balón debe colisionar contra una entidad.
     */
    private boolean shouldCollideWithEntity(
            LivingEntity livingEntity
    ) {
        if (!livingEntity.isAlive()
                || livingEntity.isSpectator()) {

            return false;
        }

        long gameTime =
                this.level().getGameTime();

        return this.temporarilyIgnoredEntity == null
                || !livingEntity.getUUID().equals(
                this.temporarilyIgnoredEntity
        )
                || gameTime
                > this.ignoredEntityUntilGameTime;
    }

    /**
     * Obtiene el movimiento de una entidad para calcular
     * el rebote.
     */
    private static Vec3 getEntityMovement(
            LivingEntity livingEntity
    ) {
        Vec3 movement =
                livingEntity.getDeltaMovement();

        if (livingEntity instanceof Player) {
            Vec3 positionMovement =
                    new Vec3(
                            livingEntity.getX()
                                    - livingEntity.xo,
                            livingEntity.getY()
                                    - livingEntity.yo,
                            livingEntity.getZ()
                                    - livingEntity.zo
                    );

            if (positionMovement.multiply(
                    1.0D,
                    0.0D,
                    1.0D
            ).lengthSqr()
                    > movement.multiply(
                    1.0D,
                    0.0D,
                    1.0D
            ).lengthSqr()) {

                movement =
                        positionMovement;
            }
        }

        Vec3 horizontalMovement =
                movement.multiply(
                        1.0D,
                        0.0D,
                        1.0D
                );

        if (horizontalMovement.length()
                > 1.20D) {

            horizontalMovement =
                    horizontalMovement.normalize()
                            .scale(
                                    1.20D
                            );
        }

        return new Vec3(
                horizontalMovement.x,
                movement.y,
                horizontalMovement.z
        );
    }

    /**
     * Calcula la normal de una colisión normal.
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
            minimumDistance =
                    distanceMaxX;

            normal =
                    new Vec3(
                            1.0D,
                            0.0D,
                            0.0D
                    );
        }

        if (distanceMinY < minimumDistance) {
            minimumDistance =
                    distanceMinY;

            normal =
                    new Vec3(
                            0.0D,
                            -1.0D,
                            0.0D
                    );
        }

        if (distanceMaxY < minimumDistance) {
            minimumDistance =
                    distanceMaxY;

            normal =
                    new Vec3(
                            0.0D,
                            1.0D,
                            0.0D
                    );
        }

        if (distanceMinZ < minimumDistance) {
            minimumDistance =
                    distanceMinZ;

            normal =
                    new Vec3(
                            0.0D,
                            0.0D,
                            -1.0D
                    );
        }

        if (distanceMaxZ < minimumDistance) {
            normal =
                    new Vec3(
                            0.0D,
                            0.0D,
                            1.0D
                    );
        }

        return normal;
    }

    /**
     * Calcula una dirección estable cuando el balón
     * comienza dentro de una hitbox.
     */
    private static Vec3 calculateOverlapNormal(
            LivingEntity livingEntity,
            Vec3 ballCenter,
            Vec3 requestedVelocity
    ) {
        Vec3 fromEntityToBall =
                new Vec3(
                        ballCenter.x
                                - livingEntity.getX(),
                        0.0D,
                        ballCenter.z
                                - livingEntity.getZ()
                );

        if (fromEntityToBall.lengthSqr()
                > 0.0001D) {

            return fromEntityToBall.normalize();
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
     * Saca el balón de una hitbox cuando comenzó
     * el tick dentro de ella.
     */
    private static Vec3 calculateOverlapEscapeMovement(
            AABB expandedBox,
            Vec3 ballCenter,
            Vec3 collisionNormal
    ) {
        double safetyMargin =
                0.022D;

        if (Math.abs(collisionNormal.x)
                >= Math.abs(collisionNormal.z)) {

            if (collisionNormal.x >= 0.0D) {
                return new Vec3(
                        expandedBox.maxX
                                - ballCenter.x
                                + safetyMargin,
                        0.0D,
                        0.0D
                );
            }

            return new Vec3(
                    expandedBox.minX
                            - ballCenter.x
                            - safetyMargin,
                    0.0D,
                    0.0D
            );
        }

        if (collisionNormal.z >= 0.0D) {
            return new Vec3(
                    0.0D,
                    0.0D,
                    expandedBox.maxZ
                            - ballCenter.z
                            + safetyMargin
            );
        }

        return new Vec3(
                0.0D,
                0.0D,
                expandedBox.minZ
                        - ballCenter.z
                        - safetyMargin
        );
    }

    /**
     * Patea el balón mediante clic.
     */
    /**
     * Mantiene compatibilidad con llamadas anteriores al sistema de pase.
     */
    public void kick(
            ServerPlayer player,
            float charge
    ) {
        kick(
                player,
                charge,
                false
        );
    }

    public void kick(
            ServerPlayer player,
            float charge,
            boolean passRequested
    ) {
        if (!this.isAlive()) {
            return;
        }

        if (RefereeCardManager.isExpelled(
                player.getUUID()
        )) {
            return;
        }

        if (isHeldByReferee()) {
            return;
        }

        if (isHeldByGoalkeeper()
                && !isHeldBy(player.getUUID())) {
            return;
        }

        if (isControlledBySlide()
                && !isControlledBySlide(
                player.getUUID()
        )) {
            return;
        }

        if (isControlledBySlide(
                player.getUUID()
        )) {
            releaseSlideControl();
        }

        /*
         * El portero puede despejar con clic izquierdo
         * mientras sostiene la pelota.
         */
        if (isHeldBy(player.getUUID())) {
            this.goalkeeperHolder = null;
            this.goalkeeperHoldUntilGameTime = 0L;
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

        /*
         * Un clic rápido solicita un pase. El servidor vuelve a validar
         * la carga y decide si existe un compañero elegible dentro del
         * cono de asistencia. Si no lo hay, el pase sigue manualmente la
         * dirección de la cámara.
         */
        if (passRequested
                && PassResolver.isValidPassRequest(
                safeCharge
        )) {
            PassResolver.PassSolution pass =
                    PassResolver.resolve(
                            this,
                            player,
                            horizontalForward,
                            safeCharge
                    );

            Vec3 passVelocity =
                    pass.direction()
                            .scale(
                                    pass.power()
                            )
                            .add(
                                    0.0D,
                                    0.035D,
                                    0.0D
                            );

            this.setDeltaMovement(
                    SoccerBallPhysics.clampVelocity(
                            passVelocity
                    )
            );

            this.hasImpulse = true;

            this.temporarilyIgnoredEntity =
                    player.getUUID();

            this.ignoredEntityUntilGameTime =
                    this.level().getGameTime()
                            + KICKER_IGNORE_TICKS;

            this.lastLegTouchTimes.put(
                    player.getUUID(),
                    this.level().getGameTime()
            );

            this.level().playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS,
                    pass.assisted()
                            ? 0.72F
                            : 0.62F,
                    pass.assisted()
                            ? 1.14F
                            : 1.04F
            );

            return;
        }

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

        this.temporarilyIgnoredEntity =
                player.getUUID();

        this.ignoredEntityUntilGameTime =
                this.level().getGameTime()
                        + KICKER_IGNORE_TICKS;

        this.lastLegTouchTimes.put(
                player.getUUID(),
                this.level().getGameTime()
        );

        this.level().playSound(
                null,
                this.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS,
                0.82F,
                0.90F
                        + safeCharge * 0.25F
        );
    }

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

        if (possibleHit.isEmpty()) {
            return new KickContact(
                    0.0D,
                    0.0D
            );
        }

        Vec3 relativeHit =
                possibleHit.get()
                        .subtract(
                                getBallCenter()
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
     * Centro físico del balón.
     */
    private Vec3 getBallCenter() {
        return this.position().add(
                0.0D,
                BALL_RADIUS,
                0.0D
        );
    }

    /**
     * Actualiza la rotación visual.
     */
    public void updateVisualRolling(
            Vec3 actualMovement
    ) {
        double horizontalDistance =
                Math.sqrt(
                        actualMovement.x
                                * actualMovement.x
                                + actualMovement.z
                                * actualMovement.z
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
        return !isHeldByGoalkeeper()
                && !isHeldByReferee()
                && !isControlledBySlide();
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
         * El mod controla manualmente los contactos.
         */
    }
}