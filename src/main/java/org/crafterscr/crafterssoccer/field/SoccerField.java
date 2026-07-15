package org.crafterscr.crafterssoccer.field;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Representa una cancha de fútbol registrada.
 *
 * La cancha guarda:
 * - Dimensión.
 * - Límites generales.
 * - Centro.
 * - Punto del balón.
 * - Portería roja.
 * - Portería azul.
 *
 * Los nombres rojo y azul son identificadores internos.
 * Más adelante los equipos podrán tener nombres personalizados.
 */
public final class SoccerField {

    /**
     * Identificador único de la cancha.
     */
    private final String id;

    /**
     * Identificador de la dimensión.
     *
     * Ejemplos:
     * minecraft:overworld
     * minecraft:the_nether
     */
    private String dimensionId;

    /**
     * Esquinas que delimitan el área completa de juego.
     */
    private BlockPos fieldPosition1;
    private BlockPos fieldPosition2;

    /**
     * Centro del campo.
     */
    private BlockPos center;

    /**
     * Posición donde aparecerá o será reiniciado el balón.
     */
    private BlockPos ballSpawn;

    /**
     * Volumen de la portería roja.
     */
    private BlockPos redGoalPosition1;
    private BlockPos redGoalPosition2;

    /**
     * Volumen de la portería azul.
     */
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

    public void setDimensionId(
            String dimensionId
    ) {
        this.dimensionId = dimensionId;
    }

    public BlockPos getFieldPosition1() {
        return fieldPosition1;
    }

    public void setFieldPosition1(
            BlockPos fieldPosition1
    ) {
        this.fieldPosition1 =
                fieldPosition1 == null
                        ? null
                        : fieldPosition1.immutable();
    }

    public BlockPos getFieldPosition2() {
        return fieldPosition2;
    }

    public void setFieldPosition2(
            BlockPos fieldPosition2
    ) {
        this.fieldPosition2 =
                fieldPosition2 == null
                        ? null
                        : fieldPosition2.immutable();
    }

    public BlockPos getCenter() {
        return center;
    }

    public void setCenter(
            BlockPos center
    ) {
        this.center =
                center == null
                        ? null
                        : center.immutable();
    }

    public BlockPos getBallSpawn() {
        return ballSpawn;
    }

    public void setBallSpawn(
            BlockPos ballSpawn
    ) {
        this.ballSpawn =
                ballSpawn == null
                        ? null
                        : ballSpawn.immutable();
    }

    public BlockPos getRedGoalPosition1() {
        return redGoalPosition1;
    }

    public void setRedGoalPosition1(
            BlockPos redGoalPosition1
    ) {
        this.redGoalPosition1 =
                redGoalPosition1 == null
                        ? null
                        : redGoalPosition1.immutable();
    }

    public BlockPos getRedGoalPosition2() {
        return redGoalPosition2;
    }

    public void setRedGoalPosition2(
            BlockPos redGoalPosition2
    ) {
        this.redGoalPosition2 =
                redGoalPosition2 == null
                        ? null
                        : redGoalPosition2.immutable();
    }

    public BlockPos getBlueGoalPosition1() {
        return blueGoalPosition1;
    }

    public void setBlueGoalPosition1(
            BlockPos blueGoalPosition1
    ) {
        this.blueGoalPosition1 =
                blueGoalPosition1 == null
                        ? null
                        : blueGoalPosition1.immutable();
    }

    public BlockPos getBlueGoalPosition2() {
        return blueGoalPosition2;
    }

    public void setBlueGoalPosition2(
            BlockPos blueGoalPosition2
    ) {
        this.blueGoalPosition2 =
                blueGoalPosition2 == null
                        ? null
                        : blueGoalPosition2.immutable();
    }

    /**
     * Devuelve true si ya están definidos los límites
     * generales de la cancha.
     */
    public boolean hasFieldBounds() {
        return fieldPosition1 != null
                && fieldPosition2 != null;
    }

    /**
     * Devuelve true si la portería roja está completa.
     */
    public boolean hasRedGoal() {
        return redGoalPosition1 != null
                && redGoalPosition2 != null;
    }

    /**
     * Devuelve true si la portería azul está completa.
     */
    public boolean hasBlueGoal() {
        return blueGoalPosition1 != null
                && blueGoalPosition2 != null;
    }

    /**
     * Comprueba si la cancha contiene toda la información
     * mínima necesaria para comenzar un partido.
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
     * Obtiene el volumen completo de la cancha.
     *
     * Se suma 1 a los máximos porque BlockPos representa
     * la esquina inferior de un bloque.
     */
    public AABB getFieldBounds() {
        if (!hasFieldBounds()) {
            return null;
        }

        return createBox(
                fieldPosition1,
                fieldPosition2
        );
    }

    /**
     * Obtiene el volumen de la portería roja.
     */
    public AABB getRedGoalBounds() {
        if (!hasRedGoal()) {
            return null;
        }

        return createBox(
                redGoalPosition1,
                redGoalPosition2
        );
    }

    /**
     * Obtiene el volumen de la portería azul.
     */
    public AABB getBlueGoalBounds() {
        if (!hasBlueGoal()) {
            return null;
        }

        return createBox(
                blueGoalPosition1,
                blueGoalPosition2
        );
    }

    /**
     * Posición exacta donde debe colocarse el centro
     * del balón.
     *
     * Se utiliza el centro horizontal del bloque.
     */
    public Vec3 getBallSpawnPosition() {
        if (ballSpawn == null) {
            return null;
        }

        return new Vec3(
                ballSpawn.getX() + 0.5D,
                ballSpawn.getY() + 0.05D,
                ballSpawn.getZ() + 0.5D
        );
    }

    /**
     * Construye una caja tridimensional usando dos bloques.
     */
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