package org.crafterscr.crafterssoccer.field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * Una cancha puede tener múltiples posiciones
     * de aparición para cada equipo.
     */
    private final List<BlockPos> redSpawns =
            new ArrayList<>();

    private final List<BlockPos> blueSpawns =
            new ArrayList<>();

    private BlockPos redGoalPosition1;
    private BlockPos redGoalPosition2;

    private BlockPos blueGoalPosition1;
    private BlockPos blueGoalPosition2;

    /*
     * Áreas donde cada portero puede usar las manos.
     */
    private BlockPos redGoalkeeperAreaPosition1;
    private BlockPos redGoalkeeperAreaPosition2;

    private BlockPos blueGoalkeeperAreaPosition1;
    private BlockPos blueGoalkeeperAreaPosition2;

    /*
     * Puntos donde esperan los jugadores expulsados.
     */
    private BlockPos redPenaltyPosition;
    private BlockPos bluePenaltyPosition;

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

    public void setDimensionId(
            String dimensionId
    ) {
        this.dimensionId = dimensionId;
    }

    public BlockPos getFieldPosition1() {
        return fieldPosition1;
    }

    public void setFieldPosition1(
            BlockPos position
    ) {
        this.fieldPosition1 =
                immutable(position);
    }

    public BlockPos getFieldPosition2() {
        return fieldPosition2;
    }

    public void setFieldPosition2(
            BlockPos position
    ) {
        this.fieldPosition2 =
                immutable(position);
    }

    public BlockPos getCenter() {
        return center;
    }

    public void setCenter(
            BlockPos position
    ) {
        this.center =
                immutable(position);
    }

    public BlockPos getBallSpawn() {
        return ballSpawn;
    }

    public void setBallSpawn(
            BlockPos position
    ) {
        this.ballSpawn =
                immutable(position);
    }

    public BlockPos getRedGoalPosition1() {
        return redGoalPosition1;
    }

    public void setRedGoalPosition1(
            BlockPos position
    ) {
        this.redGoalPosition1 =
                immutable(position);
    }

    public BlockPos getRedGoalPosition2() {
        return redGoalPosition2;
    }

    public void setRedGoalPosition2(
            BlockPos position
    ) {
        this.redGoalPosition2 =
                immutable(position);
    }

    public BlockPos getBlueGoalPosition1() {
        return blueGoalPosition1;
    }

    public void setBlueGoalPosition1(
            BlockPos position
    ) {
        this.blueGoalPosition1 =
                immutable(position);
    }

    public BlockPos getBlueGoalPosition2() {
        return blueGoalPosition2;
    }

    public void setBlueGoalPosition2(
            BlockPos position
    ) {
        this.blueGoalPosition2 =
                immutable(position);
    }


    public BlockPos getRedGoalkeeperAreaPosition1() {
        return redGoalkeeperAreaPosition1;
    }

    public void setRedGoalkeeperAreaPosition1(BlockPos position) {
        this.redGoalkeeperAreaPosition1 = immutable(position);
    }

    public BlockPos getRedGoalkeeperAreaPosition2() {
        return redGoalkeeperAreaPosition2;
    }

    public void setRedGoalkeeperAreaPosition2(BlockPos position) {
        this.redGoalkeeperAreaPosition2 = immutable(position);
    }

    public BlockPos getBlueGoalkeeperAreaPosition1() {
        return blueGoalkeeperAreaPosition1;
    }

    public void setBlueGoalkeeperAreaPosition1(BlockPos position) {
        this.blueGoalkeeperAreaPosition1 = immutable(position);
    }

    public BlockPos getBlueGoalkeeperAreaPosition2() {
        return blueGoalkeeperAreaPosition2;
    }

    public void setBlueGoalkeeperAreaPosition2(BlockPos position) {
        this.blueGoalkeeperAreaPosition2 = immutable(position);
    }

    public BlockPos getRedPenaltyPosition() {
        return redPenaltyPosition;
    }

    public void setRedPenaltyPosition(
            BlockPos position
    ) {
        this.redPenaltyPosition =
                immutable(position);
    }

    public BlockPos getBluePenaltyPosition() {
        return bluePenaltyPosition;
    }

    public void setBluePenaltyPosition(
            BlockPos position
    ) {
        this.bluePenaltyPosition =
                immutable(position);
    }

    public BlockPos getPenaltyPosition(
            org.crafterscr.crafterssoccer.match.SoccerTeamSide side
    ) {
        return side
                == org.crafterscr.crafterssoccer.match.SoccerTeamSide.RED
                ? redPenaltyPosition
                : bluePenaltyPosition;
    }

    public List<BlockPos> getRedSpawns() {
        return Collections.unmodifiableList(
                redSpawns
        );
    }

    public List<BlockPos> getBlueSpawns() {
        return Collections.unmodifiableList(
                blueSpawns
        );
    }

    public void addRedSpawn(
            BlockPos position
    ) {
        addSpawn(
                redSpawns,
                position
        );
    }

    public void addBlueSpawn(
            BlockPos position
    ) {
        addSpawn(
                blueSpawns,
                position
        );
    }

    public boolean removeRedSpawn(
            int index
    ) {
        return removeSpawn(
                redSpawns,
                index
        );
    }

    public boolean removeBlueSpawn(
            int index
    ) {
        return removeSpawn(
                blueSpawns,
                index
        );
    }

    public void clearRedSpawns() {
        redSpawns.clear();
    }

    public void clearBlueSpawns() {
        blueSpawns.clear();
    }

    /**
     * Métodos usados por el cargador JSON.
     */
    public void setRedSpawns(
            List<BlockPos> positions
    ) {
        redSpawns.clear();

        if (positions == null) {
            return;
        }

        for (BlockPos position : positions) {
            addRedSpawn(position);
        }
    }

    public void setBlueSpawns(
            List<BlockPos> positions
    ) {
        blueSpawns.clear();

        if (positions == null) {
            return;
        }

        for (BlockPos position : positions) {
            addBlueSpawn(position);
        }
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

    public boolean isComplete() {
        return dimensionId != null
                && !dimensionId.isBlank()
                && hasFieldBounds()
                && center != null
                && ballSpawn != null
                && hasRedGoal()
                && hasBlueGoal();
    }

    public boolean isMatchReady() {
        return isComplete()
                && !redSpawns.isEmpty()
                && !blueSpawns.isEmpty();
    }


    public boolean hasRedGoalkeeperArea() {
        return redGoalkeeperAreaPosition1 != null
                && redGoalkeeperAreaPosition2 != null;
    }

    public boolean hasBlueGoalkeeperArea() {
        return blueGoalkeeperAreaPosition1 != null
                && blueGoalkeeperAreaPosition2 != null;
    }

    public AABB getRedGoalkeeperAreaBounds() {
        if (!hasRedGoalkeeperArea()) {
            return null;
        }

        return createBox(
                redGoalkeeperAreaPosition1,
                redGoalkeeperAreaPosition2
        );
    }

    public AABB getBlueGoalkeeperAreaBounds() {
        if (!hasBlueGoalkeeperArea()) {
            return null;
        }

        return createBox(
                blueGoalkeeperAreaPosition1,
                blueGoalkeeperAreaPosition2
        );
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

    public Vec3 getRedSpawnPosition(
            int index
    ) {
        return getSpawnPosition(
                redSpawns,
                index
        );
    }

    public Vec3 getBlueSpawnPosition(
            int index
    ) {
        return getSpawnPosition(
                blueSpawns,
                index
        );
    }

    private static void addSpawn(
            List<BlockPos> spawns,
            BlockPos position
    ) {
        if (position == null) {
            return;
        }

        BlockPos immutablePosition =
                position.immutable();

        /*
         * Evitar puntos duplicados.
         */
        if (!spawns.contains(
                immutablePosition
        )) {
            spawns.add(
                    immutablePosition
            );
        }
    }

    /**
     * El índice recibido es de lista:
     * 0 representa el primer spawn.
     */
    private static boolean removeSpawn(
            List<BlockPos> spawns,
            int index
    ) {
        if (index < 0
                || index >= spawns.size()) {
            return false;
        }

        spawns.remove(index);
        return true;
    }

    private static Vec3 getSpawnPosition(
            List<BlockPos> spawns,
            int playerIndex
    ) {
        if (spawns.isEmpty()) {
            return null;
        }

        /*
         * Si hay más jugadores que spawns,
         * se reutilizan de forma circular.
         */
        int safeIndex =
                Math.floorMod(
                        playerIndex,
                        spawns.size()
                );

        return toCenteredPosition(
                spawns.get(safeIndex),
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
        double minX =
                Math.min(
                        first.getX(),
                        second.getX()
                );

        double minY =
                Math.min(
                        first.getY(),
                        second.getY()
                );

        double minZ =
                Math.min(
                        first.getZ(),
                        second.getZ()
                );

        double maxX =
                Math.max(
                        first.getX(),
                        second.getX()
                ) + 1.0D;

        double maxY =
                Math.max(
                        first.getY(),
                        second.getY()
                ) + 1.0D;

        double maxZ =
                Math.max(
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