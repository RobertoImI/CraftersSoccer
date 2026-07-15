package org.crafterscr.crafterssoccer.field;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Representa una cancha registrada.
 */
public final class SoccerField {

    private final String id;
    private String dimensionId;

    private BlockPos fieldPosition1;
    private BlockPos fieldPosition2;

    private BlockPos center;
    private BlockPos ballSpawn;

    private BlockPos redSpawn;
    private BlockPos blueSpawn;

    private BlockPos redGoalPosition1;
    private BlockPos redGoalPosition2;

    private BlockPos blueGoalPosition1;
    private BlockPos blueGoalPosition2;

    public SoccerField(
            String id,
            String dimensionId
    ) {
        this.id = id;
        this.dimensionId = dimensionId;
    }

    public String getId() {
        return id;
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public void setDimensionId(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public BlockPos getFieldPosition1() {
        return fieldPosition1;
    }

    public void setFieldPosition1(BlockPos position) {
        this.fieldPosition1 = immutable(position);
    }

    public BlockPos getFieldPosition2() {
        return fieldPosition2;
    }

    public void setFieldPosition2(BlockPos position) {
        this.fieldPosition2 = immutable(position);
    }

    public BlockPos getCenter() {
        return center;
    }

    public void setCenter(BlockPos position) {
        this.center = immutable(position);
    }

    public BlockPos getBallSpawn() {
        return ballSpawn;
    }

    public void setBallSpawn(BlockPos position) {
        this.ballSpawn = immutable(position);
    }

    public BlockPos getRedSpawn() {
        return redSpawn;
    }

    public void setRedSpawn(BlockPos position) {
        this.redSpawn = immutable(position);
    }

    public BlockPos getBlueSpawn() {
        return blueSpawn;
    }

    public void setBlueSpawn(BlockPos position) {
        this.blueSpawn = immutable(position);
    }

    public BlockPos getRedGoalPosition1() {
        return redGoalPosition1;
    }

    public void setRedGoalPosition1(BlockPos position) {
        this.redGoalPosition1 = immutable(position);
    }

    public BlockPos getRedGoalPosition2() {
        return redGoalPosition2;
    }

    public void setRedGoalPosition2(BlockPos position) {
        this.redGoalPosition2 = immutable(position);
    }

    public BlockPos getBlueGoalPosition1() {
        return blueGoalPosition1;
    }

    public void setBlueGoalPosition1(BlockPos position) {
        this.blueGoalPosition1 = immutable(position);
    }

    public BlockPos getBlueGoalPosition2() {
        return blueGoalPosition2;
    }

    public void setBlueGoalPosition2(BlockPos position) {
        this.blueGoalPosition2 = immutable(position);
    }

    public boolean hasFieldBounds() {
        return fieldPosition1 != null
                && fieldPosition2 != null;
    }

    public boolean hasRedGoal() {
        return redGoalPosition1 != null
                && redGoalPosition2 != null;
    }

    public boolean hasBlueGoal() {
        return blueGoalPosition1 != null
                && blueGoalPosition2 != null;
    }

    /**
     * Configuración geométrica básica.
     */
    public boolean isComplete() {
        return dimensionId != null
                && !dimensionId.isBlank()
                && hasFieldBounds()
                && center != null
                && ballSpawn != null
                && hasRedGoal()
                && hasBlueGoal();
    }

    /**
     * Configuración necesaria para comenzar un partido.
     */
    public boolean isMatchReady() {
        return isComplete()
                && redSpawn != null
                && blueSpawn != null;
    }

    public AABB getFieldBounds() {
        if (!hasFieldBounds()) {
            return null;
        }

        return createBox(
                fieldPosition1,
                fieldPosition2
        );
    }

    public AABB getRedGoalBounds() {
        if (!hasRedGoal()) {
            return null;
        }

        return createBox(
                redGoalPosition1,
                redGoalPosition2
        );
    }

    public AABB getBlueGoalBounds() {
        if (!hasBlueGoal()) {
            return null;
        }

        return createBox(
                blueGoalPosition1,
                blueGoalPosition2
        );
    }

    public Vec3 getBallSpawnPosition() {
        return toCenteredPosition(
                ballSpawn,
                0.05D
        );
    }

    public Vec3 getRedSpawnPosition() {
        return toCenteredPosition(
                redSpawn,
                0.10D
        );
    }

    public Vec3 getBlueSpawnPosition() {
        return toCenteredPosition(
                blueSpawn,
                0.10D
        );
    }

    private static BlockPos immutable(
            BlockPos position
    ) {
        return position == null
                ? null
                : position.immutable();
    }

    private static Vec3 toCenteredPosition(
            BlockPos position,
            double verticalOffset
    ) {
        if (position == null) {
            return null;
        }

        return new Vec3(
                position.getX() + 0.5D,
                position.getY() + verticalOffset,
                position.getZ() + 0.5D
        );
    }

    private static AABB createBox(
            BlockPos first,
            BlockPos second
    ) {
        double minX = Math.min(
                first.getX(),
                second.getX()
        );

        double minY = Math.min(
                first.getY(),
                second.getY()
        );

        double minZ = Math.min(
                first.getZ(),
                second.getZ()
        );

        double maxX = Math.max(
                first.getX(),
                second.getX()
        ) + 1.0D;

        double maxY = Math.max(
                first.getY(),
                second.getY()
        ) + 1.0D;

        double maxZ = Math.max(
                first.getZ(),
                second.getZ()
        ) + 1.0D;

        return new AABB(
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        );
    }
}