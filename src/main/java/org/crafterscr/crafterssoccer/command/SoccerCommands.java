package org.crafterscr.crafterssoccer.command;

import java.util.Collection;
import java.util.Locale;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.field.SoccerField;
import org.crafterscr.crafterssoccer.field.SoccerFieldManager;
import org.crafterscr.crafterssoccer.registry.ModEntities;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos administrativos de CraftersSoccer.
 */
public final class SoccerCommands {

    /**
     * Autocompletado con las canchas existentes.
     */
    private static final SuggestionProvider<CommandSourceStack>
            FIELD_SUGGESTIONS =
            (
                    context,
                    builder
            ) -> {

                for (SoccerField field
                        : SoccerFieldManager.getFields(
                        context.getSource()
                                .getServer()
                )) {

                    builder.suggest(
                            field.getId()
                    );
                }

                return builder.buildFuture();
            };

    private SoccerCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal("soccer")
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )

                        /*
                         * Comandos del balón.
                         */
                        .then(
                                createBallCommands()
                        )

                        /*
                         * Comandos de canchas.
                         */
                        .then(
                                createFieldCommands()
                        )
        );
    }

    /**
     * Construye /soccer ball
     */
    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createBallCommands() {

        return Commands.literal("ball")

                /*
                 * /soccer ball spawn
                 */
                .then(
                        Commands.literal("spawn")
                                .executes(
                                        context ->
                                                spawnBall(
                                                        context.getSource()
                                                )
                                )
                )

                /*
                 * /soccer ball removeall
                 */
                .then(
                        Commands.literal("removeall")
                                .executes(
                                        context ->
                                                removeAllBalls(
                                                        context.getSource()
                                                )
                                )
                );
    }

    /**
     * Construye /soccer field
     */
    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createFieldCommands() {

        return Commands.literal("field")

                /*
                 * /soccer field create <id>
                 */
                .then(
                        Commands.literal("create")
                                .then(
                                        Commands.argument(
                                                        "id",
                                                        StringArgumentType.word()
                                                )
                                                .executes(
                                                        context ->
                                                                createField(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "id"
                                                                        )
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer field delete <id>
                 */
                .then(
                        Commands.literal("delete")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                deleteField(
                                                                        context.getSource(),
                                                                        getFieldId(
                                                                                context
                                                                        )
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer field list
                 */
                .then(
                        Commands.literal("list")
                                .executes(
                                        context ->
                                                listFields(
                                                        context.getSource()
                                                )
                                )
                )

                /*
                 * /soccer field info <id>
                 */
                .then(
                        Commands.literal("info")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                showFieldInfo(
                                                                        context.getSource(),
                                                                        getFieldId(
                                                                                context
                                                                        )
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer field pos1 <id>
                 */
                .then(
                        Commands.literal("pos1")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                setFieldPosition(
                                                                        context.getSource(),
                                                                        getFieldId(
                                                                                context
                                                                        ),
                                                                        FieldPoint.FIELD_POSITION_1
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer field pos2 <id>
                 */
                .then(
                        Commands.literal("pos2")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                setFieldPosition(
                                                                        context.getSource(),
                                                                        getFieldId(
                                                                                context
                                                                        ),
                                                                        FieldPoint.FIELD_POSITION_2
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer field center <id>
                 */
                .then(
                        Commands.literal("center")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                setFieldPosition(
                                                                        context.getSource(),
                                                                        getFieldId(
                                                                                context
                                                                        ),
                                                                        FieldPoint.CENTER
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer field ballspawn <id>
                 */
                .then(
                        Commands.literal("ballspawn")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                setFieldPosition(
                                                                        context.getSource(),
                                                                        getFieldId(
                                                                                context
                                                                        ),
                                                                        FieldPoint.BALL_SPAWN
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer field goal red pos1 <id>
                 * /soccer field goal red pos2 <id>
                 * /soccer field goal blue pos1 <id>
                 * /soccer field goal blue pos2 <id>
                 */
                .then(
                        Commands.literal("goal")

                                .then(
                                        Commands.literal("red")

                                                .then(
                                                        Commands.literal("pos1")
                                                                .then(
                                                                        fieldArgument()
                                                                                .executes(
                                                                                        context ->
                                                                                                setFieldPosition(
                                                                                                        context.getSource(),
                                                                                                        getFieldId(
                                                                                                                context
                                                                                                        ),
                                                                                                        FieldPoint.RED_GOAL_POSITION_1
                                                                                                )
                                                                                )
                                                                )
                                                )

                                                .then(
                                                        Commands.literal("pos2")
                                                                .then(
                                                                        fieldArgument()
                                                                                .executes(
                                                                                        context ->
                                                                                                setFieldPosition(
                                                                                                        context.getSource(),
                                                                                                        getFieldId(
                                                                                                                context
                                                                                                        ),
                                                                                                        FieldPoint.RED_GOAL_POSITION_2
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )

                                .then(
                                        Commands.literal("blue")

                                                .then(
                                                        Commands.literal("pos1")
                                                                .then(
                                                                        fieldArgument()
                                                                                .executes(
                                                                                        context ->
                                                                                                setFieldPosition(
                                                                                                        context.getSource(),
                                                                                                        getFieldId(
                                                                                                                context
                                                                                                        ),
                                                                                                        FieldPoint.BLUE_GOAL_POSITION_1
                                                                                                )
                                                                                )
                                                                )
                                                )

                                                .then(
                                                        Commands.literal("pos2")
                                                                .then(
                                                                        fieldArgument()
                                                                                .executes(
                                                                                        context ->
                                                                                                setFieldPosition(
                                                                                                        context.getSource(),
                                                                                                        getFieldId(
                                                                                                                context
                                                                                                        ),
                                                                                                        FieldPoint.BLUE_GOAL_POSITION_2
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer field clear <id> <point>
                 */
                .then(
                        Commands.literal("clear")
                                .then(
                                        fieldArgument()
                                                .then(
                                                        Commands.argument(
                                                                        "point",
                                                                        StringArgumentType.word()
                                                                )
                                                                .suggests(
                                                                        (
                                                                                context,
                                                                                builder
                                                                        ) -> {

                                                                            for (FieldPoint point
                                                                                    : FieldPoint.values()) {

                                                                                builder.suggest(
                                                                                        point.commandName
                                                                                );
                                                                            }

                                                                            return builder.buildFuture();
                                                                        }
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                clearFieldPoint(
                                                                                        context.getSource(),
                                                                                        getFieldId(
                                                                                                context
                                                                                        ),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "point"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                )
                );
    }

    /**
     * Argumento reutilizable de ID de cancha.
     */
    private static com.mojang.brigadier.builder
            .RequiredArgumentBuilder<
            CommandSourceStack,
            String
            >
    fieldArgument() {

        return Commands.argument(
                        "id",
                        StringArgumentType.word()
                )
                .suggests(
                        FIELD_SUGGESTIONS
                );
    }

    private static String getFieldId(
            com.mojang.brigadier.context
                    .CommandContext<CommandSourceStack> context
    ) {
        return StringArgumentType.getString(
                context,
                "id"
        );
    }

    /**
     * Crear una cancha.
     */
    private static int createField(
            CommandSourceStack source,
            String requestedId
    ) {
        String normalizedId =
                SoccerFieldManager.normalizeId(
                        requestedId
                );

        if (normalizedId.isBlank()) {
            source.sendFailure(
                    Component.literal(
                            "§cEl ID de la cancha no es válido."
                    )
            );

            return 0;
        }

        String dimensionId =
                source.getLevel()
                        .dimension()
                        .location()
                        .toString();

        SoccerField field =
                SoccerFieldManager.createField(
                        source.getServer(),
                        normalizedId,
                        dimensionId
                );

        if (field == null) {
            source.sendFailure(
                    Component.literal(
                            "§cYa existe una cancha con el ID §f"
                                    + normalizedId
                                    + "§c."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aCancha creada: §f"
                                + field.getId()
                                + "\n§7Dimensión: §f"
                                + dimensionId
                ),
                true
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§eAhora marca sus puntos con "
                                + "/soccer field pos1 "
                                + field.getId()
                ),
                false
        );

        return 1;
    }

    /**
     * Eliminar una cancha.
     */
    private static int deleteField(
            CommandSourceStack source,
            String fieldId
    ) {
        boolean removed =
                SoccerFieldManager.removeField(
                        source.getServer(),
                        fieldId
                );

        if (!removed) {
            sendFieldNotFound(
                    source,
                    fieldId
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§eCancha eliminada: §f"
                                + SoccerFieldManager.normalizeId(
                                fieldId
                        )
                ),
                true
        );

        return 1;
    }

    /**
     * Listar canchas.
     */
    private static int listFields(
            CommandSourceStack source
    ) {
        Collection<SoccerField> fields =
                SoccerFieldManager.getFields(
                        source.getServer()
                );

        if (fields.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§7No hay canchas registradas."
                    ),
                    false
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§6§lCanchas registradas §7("
                                + fields.size()
                                + ")"
                ),
                false
        );

        for (SoccerField field : fields) {
            String status =
                    field.isComplete()
                            ? "§aCOMPLETA"
                            : "§eINCOMPLETA";

            source.sendSuccess(
                    () -> Component.literal(
                            "§8- §f"
                                    + field.getId()
                                    + " §7["
                                    + status
                                    + "§7]"
                    ),
                    false
            );
        }

        return fields.size();
    }

    /**
     * Mostrar información de una cancha.
     */
    private static int showFieldInfo(
            CommandSourceStack source,
            String fieldId
    ) {
        SoccerField field =
                getRequiredField(
                        source,
                        fieldId
                );

        if (field == null) {
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§6§lCancha: §f"
                                + field.getId()
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§7Dimensión: §f"
                                + field.getDimensionId()
                ),
                false
        );

        sendPointStatus(
                source,
                "Límite 1",
                field.getFieldPosition1()
        );

        sendPointStatus(
                source,
                "Límite 2",
                field.getFieldPosition2()
        );

        sendPointStatus(
                source,
                "Centro",
                field.getCenter()
        );

        sendPointStatus(
                source,
                "Balón",
                field.getBallSpawn()
        );

        sendPointStatus(
                source,
                "Portería roja 1",
                field.getRedGoalPosition1()
        );

        sendPointStatus(
                source,
                "Portería roja 2",
                field.getRedGoalPosition2()
        );

        sendPointStatus(
                source,
                "Portería azul 1",
                field.getBlueGoalPosition1()
        );

        sendPointStatus(
                source,
                "Portería azul 2",
                field.getBlueGoalPosition2()
        );

        source.sendSuccess(
                () -> Component.literal(
                        field.isComplete()
                                ? "§aLa cancha está completa y preparada."
                                : "§eLa cancha todavía está incompleta."
                ),
                false
        );

        return 1;
    }

    /**
     * Establecer uno de los puntos.
     */
    private static int setFieldPosition(
            CommandSourceStack source,
            String fieldId,
            FieldPoint fieldPoint
    ) {
        SoccerField field =
                getRequiredField(
                        source,
                        fieldId
                );

        if (field == null) {
            return 0;
        }

        String currentDimension =
                source.getLevel()
                        .dimension()
                        .location()
                        .toString();

        if (!field.getDimensionId()
                .equals(currentDimension)) {

            source.sendFailure(
                    Component.literal(
                            "§cEsta cancha pertenece a la dimensión §f"
                                    + field.getDimensionId()
                                    + "§c."
                    )
            );

            return 0;
        }

        BlockPos position =
                getSourceBlockPosition(
                        source
                );

        fieldPoint.set(
                field,
                position
        );

        SoccerFieldManager.save(
                source.getServer()
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§a"
                                + fieldPoint.displayName
                                + " establecido en §f"
                                + formatPosition(position)
                                + "§a para §f"
                                + field.getId()
                ),
                true
        );

        if (field.isComplete()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§a§lLa cancha ya está completa."
                    ),
                    true
            );
        }

        return 1;
    }

    /**
     * Borrar un punto individual.
     */
    private static int clearFieldPoint(
            CommandSourceStack source,
            String fieldId,
            String pointName
    ) {
        SoccerField field =
                getRequiredField(
                        source,
                        fieldId
                );

        if (field == null) {
            return 0;
        }

        FieldPoint selectedPoint =
                FieldPoint.fromCommandName(
                        pointName
                );

        if (selectedPoint == null) {
            source.sendFailure(
                    Component.literal(
                            "§cPunto desconocido: §f"
                                    + pointName
                    )
            );

            return 0;
        }

        selectedPoint.set(
                field,
                null
        );

        SoccerFieldManager.save(
                source.getServer()
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§eSe eliminó §f"
                                + selectedPoint.displayName
                                + "§e de la cancha §f"
                                + field.getId()
                ),
                true
        );

        return 1;
    }

    /**
     * Obtiene una cancha y muestra un error si no existe.
     */
    private static SoccerField getRequiredField(
            CommandSourceStack source,
            String fieldId
    ) {
        SoccerField field =
                SoccerFieldManager.getField(
                        source.getServer(),
                        fieldId
                );

        if (field == null) {
            sendFieldNotFound(
                    source,
                    fieldId
            );
        }

        return field;
    }

    private static void sendFieldNotFound(
            CommandSourceStack source,
            String fieldId
    ) {
        source.sendFailure(
                Component.literal(
                        "§cNo existe la cancha §f"
                                + SoccerFieldManager.normalizeId(
                                fieldId
                        )
                                + "§c."
                )
        );
    }

    /**
     * Obtiene la posición del jugador o del origen
     * que ejecutó el comando.
     */
    private static BlockPos getSourceBlockPosition(
            CommandSourceStack source
    ) {
        try {
            return source.getPlayerOrException()
                    .blockPosition();

        } catch (Exception ignored) {
            return BlockPos.containing(
                    source.getPosition()
            );
        }
    }

    private static void sendPointStatus(
            CommandSourceStack source,
            String name,
            BlockPos position
    ) {
        source.sendSuccess(
                () -> Component.literal(
                        "§8- §7"
                                + name
                                + ": "
                                + (
                                position == null
                                        ? "§cNO DEFINIDO"
                                        : "§f"
                                        + formatPosition(
                                        position
                                )
                        )
                ),
                false
        );
    }

    private static String formatPosition(
            BlockPos position
    ) {
        return position.getX()
                + " "
                + position.getY()
                + " "
                + position.getZ();
    }

    /**
     * Crear un balón frente al jugador.
     */
    private static int spawnBall(
            CommandSourceStack source
    ) {
        ServerLevel level =
                source.getLevel();

        SoccerBallEntity ball =
                new SoccerBallEntity(
                        ModEntities.SOCCER_BALL.get(),
                        level
                );

        Vec3 spawnPosition;

        try {
            ServerPlayer player =
                    source.getPlayerOrException();

            Vec3 look =
                    player.getLookAngle();

            spawnPosition =
                    player.getEyePosition()
                            .add(
                                    look.scale(1.8D)
                            )
                            .add(
                                    0.0D,
                                    -0.55D,
                                    0.0D
                            );

        } catch (Exception ignored) {
            spawnPosition =
                    source.getPosition();
        }

        ball.setPos(
                spawnPosition.x,
                spawnPosition.y,
                spawnPosition.z
        );

        level.addFreshEntity(ball);

        source.sendSuccess(
                () -> Component.literal(
                        "§aBalón creado correctamente."
                ),
                true
        );

        return 1;
    }

    /**
     * Eliminar todos los balones de la dimensión.
     */
    private static int removeAllBalls(
            CommandSourceStack source
    ) {
        ServerLevel level =
                source.getLevel();

        int removed = 0;

        for (Entity entity
                : level.getAllEntities()) {

            if (entity
                    instanceof SoccerBallEntity ball) {

                ball.discard();
                removed++;
            }
        }

        int finalRemoved =
                removed;

        source.sendSuccess(
                () -> Component.literal(
                        "§eBalones eliminados: "
                                + finalRemoved
                ),
                true
        );

        return removed;
    }

    /**
     * Puntos configurables de la cancha.
     */
    private enum FieldPoint {

        FIELD_POSITION_1(
                "pos1",
                "Límite 1"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setFieldPosition1(
                        position
                );
            }
        },

        FIELD_POSITION_2(
                "pos2",
                "Límite 2"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setFieldPosition2(
                        position
                );
            }
        },

        CENTER(
                "center",
                "Centro"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setCenter(
                        position
                );
            }
        },

        BALL_SPAWN(
                "ballspawn",
                "Punto del balón"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setBallSpawn(
                        position
                );
            }
        },

        RED_GOAL_POSITION_1(
                "red_goal_pos1",
                "Portería roja 1"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setRedGoalPosition1(
                        position
                );
            }
        },

        RED_GOAL_POSITION_2(
                "red_goal_pos2",
                "Portería roja 2"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setRedGoalPosition2(
                        position
                );
            }
        },

        BLUE_GOAL_POSITION_1(
                "blue_goal_pos1",
                "Portería azul 1"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setBlueGoalPosition1(
                        position
                );
            }
        },

        BLUE_GOAL_POSITION_2(
                "blue_goal_pos2",
                "Portería azul 2"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setBlueGoalPosition2(
                        position
                );
            }
        };

        private final String commandName;
        private final String displayName;

        FieldPoint(
                String commandName,
                String displayName
        ) {
            this.commandName =
                    commandName;

            this.displayName =
                    displayName;
        }

        abstract void set(
                SoccerField field,
                BlockPos position
        );

        private static FieldPoint fromCommandName(
                String requestedName
        ) {
            String normalizedName =
                    requestedName.toLowerCase(
                            Locale.ROOT
                    );

            for (FieldPoint point : values()) {
                if (point.commandName.equals(
                        normalizedName
                )) {
                    return point;
                }
            }

            return null;
        }
    }
}