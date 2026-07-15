package org.crafterscr.crafterssoccer.command;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.field.SoccerField;
import org.crafterscr.crafterssoccer.field.SoccerFieldManager;
import org.crafterscr.crafterssoccer.match.SoccerMatch;
import org.crafterscr.crafterssoccer.match.SoccerMatchManager;
import org.crafterscr.crafterssoccer.match.SoccerTeamSide;
import org.crafterscr.crafterssoccer.registry.ModEntities;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos administrativos.
 */
public final class SoccerCommands {

    private static final SuggestionProvider<CommandSourceStack>
            FIELD_SUGGESTIONS =
            (context, builder) -> {

                for (SoccerField field
                        : SoccerFieldManager.getFields(
                        context.getSource().getServer()
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
                        .then(createBallCommands())
                        .then(createFieldCommands())
                        .then(createTeamCommands())
                        .then(createMatchCommands())
        );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createBallCommands() {

        return Commands.literal("ball")

                .then(
                        Commands.literal("spawn")
                                .executes(
                                        context ->
                                                spawnBall(
                                                        context.getSource()
                                                )
                                )
                )

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

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createFieldCommands() {

        return Commands.literal("field")

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

                .then(
                        Commands.literal("delete")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                deleteField(
                                                                        context.getSource(),
                                                                        getFieldId(context)
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("list")
                                .executes(
                                        context ->
                                                listFields(
                                                        context.getSource()
                                                )
                                )
                )

                .then(
                        Commands.literal("info")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                showFieldInfo(
                                                                        context.getSource(),
                                                                        getFieldId(context)
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("pos1")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                setFieldPosition(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        FieldPoint.FIELD_POSITION_1
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
                                                                        getFieldId(context),
                                                                        FieldPoint.FIELD_POSITION_2
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("center")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                setFieldPosition(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        FieldPoint.CENTER
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("ballspawn")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                setFieldPosition(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        FieldPoint.BALL_SPAWN
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("spawn")

                                .then(
                                        Commands.literal("red")
                                                .then(
                                                        fieldArgument()
                                                                .executes(
                                                                        context ->
                                                                                setFieldPosition(
                                                                                        context.getSource(),
                                                                                        getFieldId(context),
                                                                                        FieldPoint.RED_SPAWN
                                                                                )
                                                                )
                                                )
                                )

                                .then(
                                        Commands.literal("blue")
                                                .then(
                                                        fieldArgument()
                                                                .executes(
                                                                        context ->
                                                                                setFieldPosition(
                                                                                        context.getSource(),
                                                                                        getFieldId(context),
                                                                                        FieldPoint.BLUE_SPAWN
                                                                                )
                                                                )
                                                )
                                )
                )

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
                                                                                                        getFieldId(context),
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
                                                                                                        getFieldId(context),
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
                                                                                                        getFieldId(context),
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
                                                                                                        getFieldId(context),
                                                                                                        FieldPoint.BLUE_GOAL_POSITION_2
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                )

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
                                                                        (context, builder) -> {

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
                                                                                        getFieldId(context),
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

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createTeamCommands() {

        return Commands.literal("team")

                .then(
                        Commands.literal("red")
                                .then(
                                        Commands.literal("add")
                                                .then(
                                                        Commands.argument(
                                                                        "player",
                                                                        EntityArgument.player()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                addPlayerToTeam(
                                                                                        context.getSource(),
                                                                                        EntityArgument.getPlayer(
                                                                                                context,
                                                                                                "player"
                                                                                        ),
                                                                                        SoccerTeamSide.RED
                                                                                )
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("blue")
                                .then(
                                        Commands.literal("add")
                                                .then(
                                                        Commands.argument(
                                                                        "player",
                                                                        EntityArgument.player()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                addPlayerToTeam(
                                                                                        context.getSource(),
                                                                                        EntityArgument.getPlayer(
                                                                                                context,
                                                                                                "player"
                                                                                        ),
                                                                                        SoccerTeamSide.BLUE
                                                                                )
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("remove")
                                .then(
                                        Commands.argument(
                                                        "player",
                                                        EntityArgument.player()
                                                )
                                                .executes(
                                                        context ->
                                                                removePlayerFromTeam(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayer(
                                                                                context,
                                                                                "player"
                                                                        )
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("list")
                                .executes(
                                        context ->
                                                listTeams(
                                                        context.getSource()
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createMatchCommands() {

        return Commands.literal("match")

                .then(
                        Commands.literal("start")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                startMatch(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        10
                                                                )
                                                )
                                                .then(
                                                        Commands.argument(
                                                                        "minutes",
                                                                        IntegerArgumentType.integer(
                                                                                1,
                                                                                120
                                                                        )
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                startMatch(
                                                                                        context.getSource(),
                                                                                        getFieldId(context),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "minutes"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("pause")
                                .executes(
                                        context ->
                                                pauseMatch(
                                                        context.getSource()
                                                )
                                )
                )

                .then(
                        Commands.literal("resume")
                                .executes(
                                        context ->
                                                resumeMatch(
                                                        context.getSource()
                                                )
                                )
                )

                .then(
                        Commands.literal("stop")
                                .executes(
                                        context ->
                                                stopMatch(
                                                        context.getSource()
                                                )
                                )
                )

                .then(
                        Commands.literal("status")
                                .executes(
                                        context ->
                                                showMatchStatus(
                                                        context.getSource()
                                                )
                                )
                );
    }

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
                .suggests(FIELD_SUGGESTIONS);
    }

    private static String getFieldId(
            CommandContext<CommandSourceStack> context
    ) {
        return StringArgumentType.getString(
                context,
                "id"
        );
    }

    private static int createField(
            CommandSourceStack source,
            String requestedId
    ) {
        String normalizedId =
                SoccerFieldManager.normalizeId(
                        requestedId
                );

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
                            "§cNo se pudo crear la cancha. "
                                    + "Puede que el ID ya exista."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aCancha creada: §f"
                                + field.getId()
                ),
                true
        );

        return 1;
    }

    private static int deleteField(
            CommandSourceStack source,
            String fieldId
    ) {
        if (!SoccerFieldManager.removeField(
                source.getServer(),
                fieldId
        )) {
            sendFieldNotFound(source, fieldId);
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§eCancha eliminada: §f"
                                + fieldId
                ),
                true
        );

        return 1;
    }

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

        for (SoccerField field : fields) {
            String status =
                    field.isMatchReady()
                            ? "§aLISTA"
                            : field.isComplete()
                            ? "§eFALTAN SPAWNS"
                            : "§cINCOMPLETA";

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
                "Spawn rojo",
                field.getRedSpawn()
        );

        sendPointStatus(
                source,
                "Spawn azul",
                field.getBlueSpawn()
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
                        field.isMatchReady()
                                ? "§aLa cancha está lista para partidos."
                                : "§eLa cancha todavía no está lista."
                ),
                false
        );

        return 1;
    }

    private static int setFieldPosition(
            CommandSourceStack source,
            String fieldId,
            FieldPoint point
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
                            "§cLa cancha pertenece a §f"
                                    + field.getDimensionId()
                    )
            );

            return 0;
        }

        BlockPos position =
                getSourceBlockPosition(source);

        point.set(
                field,
                position
        );

        SoccerFieldManager.save(
                source.getServer()
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§a"
                                + point.displayName
                                + " establecido en §f"
                                + formatPosition(position)
                ),
                true
        );

        return 1;
    }

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

        FieldPoint selected =
                FieldPoint.fromCommandName(
                        pointName
                );

        if (selected == null) {
            source.sendFailure(
                    Component.literal(
                            "§cPunto desconocido: §f"
                                    + pointName
                    )
            );

            return 0;
        }

        selected.set(
                field,
                null
        );

        SoccerFieldManager.save(
                source.getServer()
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§ePunto eliminado: §f"
                                + selected.displayName
                ),
                true
        );

        return 1;
    }

    private static int addPlayerToTeam(
            CommandSourceStack source,
            ServerPlayer player,
            SoccerTeamSide side
    ) {
        SoccerMatchManager.addPlayer(
                source.getServer(),
                player,
                side
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§a"
                                + player.getGameProfile().getName()
                                + " ahora pertenece al equipo §f"
                                + side.getDisplayName()
                ),
                true
        );

        return 1;
    }

    private static int removePlayerFromTeam(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        if (!SoccerMatchManager.removePlayer(
                source.getServer(),
                player.getUUID()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cEl jugador no pertenece a ningún equipo."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§eJugador eliminado de su equipo: §f"
                                + player.getGameProfile().getName()
                ),
                true
        );

        return 1;
    }

    private static int listTeams(
            CommandSourceStack source
    ) {
        MinecraftServer server =
                source.getServer();

        source.sendSuccess(
                () -> Component.literal(
                        "§c§lEQUIPO ROJO"
                ),
                false
        );

        sendPlayerSet(
                source,
                server,
                SoccerMatchManager.getRedPlayers(server)
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§9§lEQUIPO AZUL"
                ),
                false
        );

        sendPlayerSet(
                source,
                server,
                SoccerMatchManager.getBluePlayers(server)
        );

        return 1;
    }

    private static void sendPlayerSet(
            CommandSourceStack source,
            MinecraftServer server,
            Set<UUID> players
    ) {
        if (players.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§8- §7Vacío"
                    ),
                    false
            );

            return;
        }

        for (UUID playerId : players) {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(playerId);

            String name =
                    player == null
                            ? playerId.toString()
                            : player.getGameProfile().getName();

            source.sendSuccess(
                    () -> Component.literal(
                            "§8- §f" + name
                    ),
                    false
            );
        }
    }

    private static int startMatch(
            CommandSourceStack source,
            String fieldId,
            int minutes
    ) {
        SoccerField field =
                getRequiredField(
                        source,
                        fieldId
                );

        if (field == null) {
            return 0;
        }

        if (!field.isMatchReady()) {
            source.sendFailure(
                    Component.literal(
                            "§cLa cancha no está lista. "
                                    + "Debes configurar los spawns rojo y azul."
                    )
            );

            return 0;
        }

        if (!SoccerMatchManager.startMatch(
                source.getServer(),
                field,
                minutes
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cYa existe un partido activo."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aPartido iniciado en §f"
                                + field.getId()
                                + "§a por §f"
                                + minutes
                                + " minutos."
                ),
                true
        );

        return 1;
    }

    private static int pauseMatch(
            CommandSourceStack source
    ) {
        if (!SoccerMatchManager.pauseMatch(
                source.getServer()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un partido que pueda pausarse."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§ePartido pausado."
                ),
                true
        );

        return 1;
    }

    private static int resumeMatch(
            CommandSourceStack source
    ) {
        if (!SoccerMatchManager.resumeMatch(
                source.getServer()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un partido pausado."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aPartido reanudado."
                ),
                true
        );

        return 1;
    }

    private static int stopMatch(
            CommandSourceStack source
    ) {
        if (!SoccerMatchManager.stopMatch(
                source.getServer()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un partido activo."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§ePartido detenido."
                ),
                true
        );

        return 1;
    }

    private static int showMatchStatus(
            CommandSourceStack source
    ) {
        SoccerMatch match =
                SoccerMatchManager.getActiveMatch(
                        source.getServer()
                );

        if (match == null) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§7No hay un partido activo."
                    ),
                    false
            );

            return 0;
        }

        int totalSeconds =
                match.getRemainingTicks() / 20;

        int minutes =
                totalSeconds / 60;

        int seconds =
                totalSeconds % 60;

        source.sendSuccess(
                () -> Component.literal(
                        "§6§lPartido activo\n"
                                + "§7Cancha: §f"
                                + match.getFieldId()
                                + "\n§7Estado: §f"
                                + match.getState().name()
                                + "\n§7Marcador: §c"
                                + match.getRedScore()
                                + " §f- §9"
                                + match.getBlueScore()
                                + "\n§7Tiempo: §f"
                                + String.format(
                                "%02d:%02d",
                                minutes,
                                seconds
                        )
                ),
                false
        );

        return 1;
    }

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
                )
        );
    }

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
                                        + formatPosition(position)
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

            spawnPosition =
                    player.getEyePosition()
                            .add(
                                    player.getLookAngle()
                                            .scale(1.8D)
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

    private static int removeAllBalls(
            CommandSourceStack source
    ) {
        ServerLevel level =
                source.getLevel();

        int removed = 0;

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SoccerBallEntity ball) {
                ball.discard();
                removed++;
            }
        }

        int finalRemoved = removed;

        source.sendSuccess(
                () -> Component.literal(
                        "§eBalones eliminados: "
                                + finalRemoved
                ),
                true
        );

        return removed;
    }

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
                field.setFieldPosition1(position);
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
                field.setFieldPosition2(position);
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
                field.setCenter(position);
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
                field.setBallSpawn(position);
            }
        },

        RED_SPAWN(
                "red_spawn",
                "Spawn rojo"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setRedSpawn(position);
            }
        },

        BLUE_SPAWN(
                "blue_spawn",
                "Spawn azul"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setBlueSpawn(position);
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
                field.setRedGoalPosition1(position);
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
                field.setRedGoalPosition2(position);
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
                field.setBlueGoalPosition1(position);
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
                field.setBlueGoalPosition2(position);
            }
        };

        private final String commandName;
        private final String displayName;

        FieldPoint(
                String commandName,
                String displayName
        ) {
            this.commandName = commandName;
            this.displayName = displayName;
        }

        abstract void set(
                SoccerField field,
                BlockPos position
        );

        private static FieldPoint fromCommandName(
                String requestedName
        ) {
            String normalized =
                    requestedName.toLowerCase(
                            Locale.ROOT
                    );

            for (FieldPoint point : values()) {
                if (point.commandName.equals(normalized)) {
                    return point;
                }
            }

            return null;
        }
    }
}