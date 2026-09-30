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
import org.crafterscr.crafterssoccer.referee.RefereeManager;
import org.crafterscr.crafterssoccer.spectator.SpectatorBroadcastManager;

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
        var root = Commands.literal("soccer");

        root.then(
                createBallCommands()
                        .requires(CommandAccess::isAdministrator)
        );
        FieldCommands.register(root);
        TeamCommands.register(root);
        root.then(
                createRefereeCommands()
        );
        MatchCommands.register(root);
        BroadcastCommands.register(root);

        event.getDispatcher().register(root);
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

    static com.mojang.brigadier.builder
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
                                        createSpawnTeamCommands(
                                                "red",
                                                SoccerTeamSide.RED
                                        )
                                )
                                .then(
                                        createSpawnTeamCommands(
                                                "blue",
                                                SoccerTeamSide.BLUE
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
    createSpawnTeamCommands(
            String commandName,
            SoccerTeamSide side
    ) {
        return Commands.literal(commandName)

                .then(
                        Commands.literal("add")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                addTeamSpawn(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        side
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("list")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                listTeamSpawns(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        side
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("remove")
                                .then(
                                        fieldArgument()
                                                .then(
                                                        Commands.argument(
                                                                        "number",
                                                                        IntegerArgumentType.integer(1)
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                removeTeamSpawn(
                                                                                        context.getSource(),
                                                                                        getFieldId(context),
                                                                                        side,
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "number"
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
                                                .executes(
                                                        context ->
                                                                clearTeamSpawns(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        side
                                                                )
                                                )
                                )
                );
    }

    static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createGoalkeeperAreaCommands(
            String commandName,
            SoccerTeamSide side
    ) {
        return Commands.literal(commandName)
                .then(
                        Commands.literal("pos1")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                setGoalkeeperAreaPosition(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        side,
                                                                        true
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
                                                                setGoalkeeperAreaPosition(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        side,
                                                                        false
                                                                )
                                                )
                                )
                )
                .then(
                        Commands.literal("clear")
                                .then(
                                        fieldArgument()
                                                .executes(
                                                        context ->
                                                                clearGoalkeeperArea(
                                                                        context.getSource(),
                                                                        getFieldId(context),
                                                                        side
                                                                )
                                                )
                                )
                );
    }

    static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createTeamCommands() {

        return Commands.literal("team")

                /*
                 * Equipo rojo.
                 */
                .then(
                        Commands.literal("red")

                                /*
                                 * /soccer team red add <jugadores>
                                 *
                                 * Admite:
                                 * nombre
                                 * @a
                                 * @p
                                 * @r
                                 * @s
                                 * @a[filtros]
                                 */
                                .then(
                                        Commands.literal("add")
                                                .then(
                                                        Commands.argument(
                                                                        "players",
                                                                        EntityArgument.players()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                addPlayersToTeam(
                                                                                        context.getSource(),
                                                                                        EntityArgument.getPlayers(
                                                                                                context,
                                                                                                "players"
                                                                                        ),
                                                                                        SoccerTeamSide.RED
                                                                                )
                                                                )
                                                )
                                )

                                /*
                                 * /soccer team red name <nombre>
                                 */
                                .then(
                                        Commands.literal("name")
                                                .then(
                                                        Commands.argument(
                                                                        "name",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                setTeamName(
                                                                                        context.getSource(),
                                                                                        SoccerTeamSide.RED,
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "name"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                )

                                .then(
                                        Commands.literal("goalkeeper")
                                                .then(
                                                        Commands.argument(
                                                                        "player",
                                                                        EntityArgument.player()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                setGoalkeeper(
                                                                                        context.getSource(),
                                                                                        SoccerTeamSide.RED,
                                                                                        EntityArgument.getPlayer(
                                                                                                context,
                                                                                                "player"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                                .then(
                                                        Commands.literal("clear")
                                                                .executes(
                                                                        context ->
                                                                                clearGoalkeeper(
                                                                                        context.getSource(),
                                                                                        SoccerTeamSide.RED
                                                                                )
                                                                )
                                                )
                                )

                                .then(
                                        Commands.literal("clear")
                                                .executes(
                                                        context ->
                                                                clearTeam(
                                                                        context.getSource(),
                                                                        SoccerTeamSide.RED
                                                                )
                                                )
                                )
                )

                /*
                 * Equipo azul.
                 */
                .then(
                        Commands.literal("blue")

                                /*
                                 * /soccer team blue add <jugadores>
                                 */
                                .then(
                                        Commands.literal("add")
                                                .then(
                                                        Commands.argument(
                                                                        "players",
                                                                        EntityArgument.players()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                addPlayersToTeam(
                                                                                        context.getSource(),
                                                                                        EntityArgument.getPlayers(
                                                                                                context,
                                                                                                "players"
                                                                                        ),
                                                                                        SoccerTeamSide.BLUE
                                                                                )
                                                                )
                                                )
                                )

                                /*
                                 * /soccer team blue name <nombre>
                                 */
                                .then(
                                        Commands.literal("name")
                                                .then(
                                                        Commands.argument(
                                                                        "name",
                                                                        StringArgumentType.greedyString()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                setTeamName(
                                                                                        context.getSource(),
                                                                                        SoccerTeamSide.BLUE,
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "name"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                )

                                .then(
                                        Commands.literal("goalkeeper")
                                                .then(
                                                        Commands.argument(
                                                                        "player",
                                                                        EntityArgument.player()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                setGoalkeeper(
                                                                                        context.getSource(),
                                                                                        SoccerTeamSide.BLUE,
                                                                                        EntityArgument.getPlayer(
                                                                                                context,
                                                                                                "player"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                                .then(
                                                        Commands.literal("clear")
                                                                .executes(
                                                                        context ->
                                                                                clearGoalkeeper(
                                                                                        context.getSource(),
                                                                                        SoccerTeamSide.BLUE
                                                                                )
                                                                )
                                                )
                                )

                                .then(
                                        Commands.literal("clear")
                                                .executes(
                                                        context ->
                                                                clearTeam(
                                                                        context.getSource(),
                                                                        SoccerTeamSide.BLUE
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("clearall")
                                .executes(
                                        context ->
                                                clearAllTeams(
                                                        context.getSource()
                                                )
                                )
                )

                /*
                 * /soccer team remove <jugadores>
                 *
                 * También admite @a, @p, @r y filtros.
                 */
                .then(
                        Commands.literal("remove")
                                .then(
                                        Commands.argument(
                                                        "players",
                                                        EntityArgument.players()
                                                )
                                                .executes(
                                                        context ->
                                                                removePlayersFromTeams(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayers(
                                                                                context,
                                                                                "players"
                                                                        )
                                                                )
                                                )
                                )
                )

                /*
                 * /soccer team list
                 */
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
    createRefereeCommands() {

        return Commands.literal("referee")

                .then(
                        Commands.literal("set")
                                .requires(
                                        CommandAccess::isAdministrator
                                )
                                .then(
                                        Commands.argument(
                                                        "player",
                                                        EntityArgument.player()
                                                )
                                                .executes(
                                                        context ->
                                                                setReferee(
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
                        Commands.literal("clear")
                                .requires(
                                        CommandAccess::isAdministrator
                                )
                                .executes(
                                        context ->
                                                clearReferee(
                                                        context.getSource()
                                                )
                                )
                )

                .then(
                        Commands.literal("status")
                                .requires(
                                        CommandAccess::isAdministrator
                                )
                                .executes(
                                        context ->
                                                showRefereeStatus(
                                                        context.getSource()
                                                )
                                )
                )

                .then(
                        Commands.literal("foul")
                                .requires(
                                        CommandAccess::isAssignedReferee
                                )

                                .then(
                                        Commands.literal("freeze")
                                                .executes(
                                                        context ->
                                                                freezeFoulBall(
                                                                        context.getSource()
                                                                )
                                                )
                                )

                                .then(
                                        Commands.literal("release")
                                                .executes(
                                                        context ->
                                                                releaseFoulBall(
                                                                        context.getSource()
                                                                )
                                                )
                                )

                                .then(
                                        Commands.literal("status")
                                                .executes(
                                                        context ->
                                                                showFoulBallStatus(
                                                                        context.getSource()
                                                                )
                                                )
                                )
                );
    }

    static com.mojang.brigadier.builder
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


    static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createGoalControlCommands() {
        return Commands.literal("goal")
                .then(
                        createGoalActionCommands(
                                "add",
                                1
                        )
                )
                .then(
                        createGoalActionCommands(
                                "remove",
                                -1
                        )
                )
                .then(
                        Commands.literal("status")
                                .executes(
                                        context ->
                                                showManualScoreStatus(
                                                        context.getSource()
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createGoalActionCommands(
            String actionName,
            int direction
    ) {
        return Commands.literal(actionName)
                .then(
                        createGoalTeamCommand(
                                "red",
                                SoccerTeamSide.RED,
                                direction
                        )
                )
                .then(
                        createGoalTeamCommand(
                                "blue",
                                SoccerTeamSide.BLUE,
                                direction
                        )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createGoalTeamCommand(
            String commandName,
            SoccerTeamSide side,
            int direction
    ) {
        return Commands.literal(commandName)
                .executes(
                        context ->
                                adjustGoal(
                                        context.getSource(),
                                        side,
                                        direction
                                )
                )
                .then(
                        Commands.argument(
                                        "amount",
                                        IntegerArgumentType.integer(
                                                1,
                                                99
                                        )
                                )
                                .executes(
                                        context ->
                                                adjustGoal(
                                                        context.getSource(),
                                                        side,
                                                        direction
                                                                * IntegerArgumentType
                                                                .getInteger(
                                                                        context,
                                                                        "amount"
                                                                )
                                                )
                                )
                );
    }

    static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createAddedTimeCommands() {
        return Commands.literal("addedtime")
                /*
                 * /soccer addedtime 5
                 * Establece el total de reposición en +5.
                 */
                .then(
                        Commands.argument(
                                        "minutes",
                                        IntegerArgumentType.integer(
                                                0,
                                                120
                                        )
                                )
                                .executes(
                                        context ->
                                                setAddedTime(
                                                        context.getSource(),
                                                        IntegerArgumentType
                                                                .getInteger(
                                                                        context,
                                                                        "minutes"
                                                                )
                                                )
                                )
                )
                .then(
                        Commands.literal("add")
                                .then(
                                        Commands.argument(
                                                        "minutes",
                                                        IntegerArgumentType
                                                                .integer(
                                                                        1,
                                                                        120
                                                                )
                                                )
                                                .executes(
                                                        context ->
                                                                addAddedTime(
                                                                        context.getSource(),
                                                                        IntegerArgumentType
                                                                                .getInteger(
                                                                                        context,
                                                                                        "minutes"
                                                                                )
                                                                )
                                                )
                                )
                )
                .then(
                        Commands.literal("clear")
                                .executes(
                                        context ->
                                                setAddedTime(
                                                        context.getSource(),
                                                        0
                                                )
                                )
                )
                .then(
                        Commands.literal("status")
                                .executes(
                                        context ->
                                                showAddedTimeStatus(
                                                        context.getSource()
                                                )
                                )
                );
    }

    static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createBroadcastCameraCommands() {
        return Commands.literal("camera")
                .then(
                        Commands.literal("set")

                                /*
                                 * Compatibilidad:
                                 * /soccer camera set <player>
                                 * sigue asignando Cámara 1.
                                 */
                                .then(
                                        Commands.argument(
                                                        "player",
                                                        EntityArgument.player()
                                                )
                                                .executes(
                                                        context ->
                                                                setBroadcastCamera(
                                                                        context.getSource(),
                                                                        1,
                                                                        EntityArgument.getPlayer(
                                                                                context,
                                                                                "player"
                                                                        )
                                                                )
                                                )
                                )

                                .then(
                                        Commands.argument(
                                                        "slot",
                                                        IntegerArgumentType.integer(
                                                                1,
                                                                SpectatorBroadcastManager.MAX_CAMERAS
                                                        )
                                                )
                                                .then(
                                                        Commands.argument(
                                                                        "player",
                                                                        EntityArgument.player()
                                                                )
                                                                .executes(
                                                                        context ->
                                                                                setBroadcastCamera(
                                                                                        context.getSource(),
                                                                                        IntegerArgumentType.getInteger(
                                                                                                context,
                                                                                                "slot"
                                                                                        ),
                                                                                        EntityArgument.getPlayer(
                                                                                                context,
                                                                                                "player"
                                                                                        )
                                                                                )
                                                                )
                                                )
                                )
                )
                .then(
                        Commands.literal("clear")
                                .executes(
                                        context ->
                                                clearBroadcastCamera(
                                                        context.getSource()
                                                )
                                )
                                .then(
                                        Commands.argument(
                                                        "slot",
                                                        IntegerArgumentType.integer(
                                                                1,
                                                                SpectatorBroadcastManager.MAX_CAMERAS
                                                        )
                                                )
                                                .executes(
                                                        context ->
                                                                clearBroadcastCamera(
                                                                        context.getSource(),
                                                                        IntegerArgumentType.getInteger(
                                                                                context,
                                                                                "slot"
                                                                        )
                                                                )
                                                )
                                )
                )
                .then(
                        Commands.literal("status")
                                .executes(
                                        context ->
                                                showBroadcastCameraStatus(
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
                "Punto de castigo rojo",
                field.getRedPenaltyPosition()
        );

        sendPointStatus(
                source,
                "Punto de castigo azul",
                field.getBluePenaltyPosition()
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§8- §7Spawns rojos: §f"
                                + field.getRedSpawns().size()
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§8- §7Spawns azules: §f"
                                + field.getBlueSpawns().size()
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§8- §7Área portero rojo: "
                                + (field.hasRedGoalkeeperArea()
                                ? "§aDEFINIDA"
                                : "§cNO DEFINIDA")
                ),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§8- §7Área portero azul: "
                                + (field.hasBlueGoalkeeperArea()
                                ? "§aDEFINIDA"
                                : "§cNO DEFINIDA")
                ),
                false
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

    private static int setGoalkeeperAreaPosition(
            CommandSourceStack source,
            String fieldId,
            SoccerTeamSide side,
            boolean firstPosition
    ) {
        SoccerField field =
                getRequiredField(source, fieldId);

        if (field == null) {
            return 0;
        }

        String currentDimension =
                source.getLevel()
                        .dimension()
                        .location()
                        .toString();

        if (!field.getDimensionId().equals(currentDimension)) {
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

        if (side == SoccerTeamSide.RED) {
            if (firstPosition) {
                field.setRedGoalkeeperAreaPosition1(position);
            } else {
                field.setRedGoalkeeperAreaPosition2(position);
            }
        } else {
            if (firstPosition) {
                field.setBlueGoalkeeperAreaPosition1(position);
            } else {
                field.setBlueGoalkeeperAreaPosition2(position);
            }
        }

        SoccerFieldManager.save(source.getServer());

        source.sendSuccess(
                () -> Component.literal(
                        "§aÁrea del portero "
                                + side.getDisplayName()
                                + " pos"
                                + (firstPosition ? "1" : "2")
                                + " definida en §f"
                                + formatPosition(position)
                ),
                true
        );

        return 1;
    }

    private static int clearGoalkeeperArea(
            CommandSourceStack source,
            String fieldId,
            SoccerTeamSide side
    ) {
        SoccerField field =
                getRequiredField(source, fieldId);

        if (field == null) {
            return 0;
        }

        if (side == SoccerTeamSide.RED) {
            field.setRedGoalkeeperAreaPosition1(null);
            field.setRedGoalkeeperAreaPosition2(null);
        } else {
            field.setBlueGoalkeeperAreaPosition1(null);
            field.setBlueGoalkeeperAreaPosition2(null);
        }

        SoccerFieldManager.save(source.getServer());

        source.sendSuccess(
                () -> Component.literal(
                        "§eÁrea del portero "
                                + side.getDisplayName()
                                + " eliminada."
                ),
                true
        );

        return 1;
    }

    private static int setGoalkeeper(
            CommandSourceStack source,
            SoccerTeamSide side,
            ServerPlayer player
    ) {
        if (SoccerMatchManager.isReferee(
                source.getServer(),
                player.getUUID()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cEl árbitro no puede ser portero."
                    )
            );
            return 0;
        }

        SoccerTeamSide playerTeam =
                SoccerMatchManager.getPlayerTeam(
                        source.getServer(),
                        player.getUUID()
                );

        if (playerTeam != side) {
            source.sendFailure(
                    Component.literal(
                            "§cEl jugador debe pertenecer primero al equipo "
                                    + side.getDisplayName()
                                    + "."
                    )
            );
            return 0;
        }

        if (!SoccerMatchManager.setGoalkeeper(
                source.getServer(),
                side,
                player.getUUID()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo se pudo asignar el portero."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§a"
                                + player.getGameProfile().getName()
                                + " ahora es portero de §f"
                                + SoccerMatchManager.getTeamName(
                                source.getServer(),
                                side
                        )
                ),
                true
        );

        return 1;
    }

    private static int clearGoalkeeper(
            CommandSourceStack source,
            SoccerTeamSide side
    ) {
        SoccerMatchManager.setGoalkeeper(
                source.getServer(),
                side,
                null
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§ePortero eliminado del equipo "
                                + side.getDisplayName()
                                + "."
                ),
                true
        );

        return 1;
    }

    private static int addTeamSpawn(
            CommandSourceStack source,
            String fieldId,
            SoccerTeamSide side
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

        if (!field.getDimensionId().equals(currentDimension)) {
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

        int previousSize =
                side == SoccerTeamSide.RED
                        ? field.getRedSpawns().size()
                        : field.getBlueSpawns().size();

        if (side == SoccerTeamSide.RED) {
            field.addRedSpawn(position);
        } else {
            field.addBlueSpawn(position);
        }

        int newSize =
                side == SoccerTeamSide.RED
                        ? field.getRedSpawns().size()
                        : field.getBlueSpawns().size();

        if (newSize == previousSize) {
            source.sendFailure(
                    Component.literal(
                            "§cYa existe un spawn en esa posición."
                    )
            );
            return 0;
        }

        SoccerFieldManager.save(source.getServer());

        source.sendSuccess(
                () -> Component.literal(
                        "§aSpawn "
                                + side.getDisplayName()
                                + " agregado como punto §f"
                                + newSize
                                + "§a en §f"
                                + formatPosition(position)
                ),
                true
        );

        return 1;
    }

    private static int listTeamSpawns(
            CommandSourceStack source,
            String fieldId,
            SoccerTeamSide side
    ) {
        SoccerField field =
                getRequiredField(source, fieldId);

        if (field == null) {
            return 0;
        }

        java.util.List<BlockPos> spawns =
                side == SoccerTeamSide.RED
                        ? field.getRedSpawns()
                        : field.getBlueSpawns();

        source.sendSuccess(
                () -> Component.literal(
                        "§6Spawns del equipo "
                                + side.getDisplayName()
                                + " §7("
                                + spawns.size()
                                + ")"
                ),
                false
        );

        if (spawns.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§8- §7No hay puntos registrados."
                    ),
                    false
            );
            return 0;
        }

        for (int index = 0; index < spawns.size(); index++) {
            int visibleNumber = index + 1;
            BlockPos position = spawns.get(index);

            source.sendSuccess(
                    () -> Component.literal(
                            "§8- §f"
                                    + visibleNumber
                                    + "§7: §f"
                                    + formatPosition(position)
                    ),
                    false
            );
        }

        return spawns.size();
    }

    private static int removeTeamSpawn(
            CommandSourceStack source,
            String fieldId,
            SoccerTeamSide side,
            int visibleNumber
    ) {
        SoccerField field =
                getRequiredField(source, fieldId);

        if (field == null) {
            return 0;
        }

        int internalIndex = visibleNumber - 1;

        boolean removed =
                side == SoccerTeamSide.RED
                        ? field.removeRedSpawn(internalIndex)
                        : field.removeBlueSpawn(internalIndex);

        if (!removed) {
            source.sendFailure(
                    Component.literal(
                            "§cNo existe el spawn número §f"
                                    + visibleNumber
                                    + "§c."
                    )
            );
            return 0;
        }

        SoccerFieldManager.save(source.getServer());

        source.sendSuccess(
                () -> Component.literal(
                        "§eSpawn "
                                + side.getDisplayName()
                                + " eliminado: §f"
                                + visibleNumber
                ),
                true
        );

        return 1;
    }

    private static int clearTeamSpawns(
            CommandSourceStack source,
            String fieldId,
            SoccerTeamSide side
    ) {
        SoccerField field =
                getRequiredField(source, fieldId);

        if (field == null) {
            return 0;
        }

        if (side == SoccerTeamSide.RED) {
            field.clearRedSpawns();
        } else {
            field.clearBlueSpawns();
        }

        SoccerFieldManager.save(source.getServer());

        source.sendSuccess(
                () -> Component.literal(
                        "§eSe eliminaron todos los spawns "
                                + side.getDisplayName()
                                + "."
                ),
                true
        );

        return 1;
    }

    private static int setTeamName(
            CommandSourceStack source,
            SoccerTeamSide side,
            String requestedName
    ) {
        if (!SoccerMatchManager.setTeamName(
                source.getServer(),
                side,
                requestedName
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cEl nombre no es válido."
                    )
            );
            return 0;
        }

        String finalName =
                SoccerMatchManager.getTeamName(
                        source.getServer(),
                        side
                );

        source.sendSuccess(
                () -> Component.literal(
                        "§aEl equipo "
                                + side.getDisplayName()
                                + " ahora se llama §f"
                                + finalName
                ),
                true
        );

        return 1;
    }

    /**
     * Añade uno o varios jugadores al equipo indicado.
     *
     * Funciona con:
     * - Un nombre individual.
     * - @a
     * - @p
     * - @r
     * - @s
     * - Selectores con filtros.
     */
    private static int addPlayersToTeam(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            SoccerTeamSide side
    ) {
        if (players.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "§cNo se encontraron jugadores."
                    )
            );

            return 0;
        }

        int added = 0;

        for (ServerPlayer player : players) {
            if (SoccerMatchManager.isReferee(
                    source.getServer(),
                    player.getUUID()
            )) {
                continue;
            }

            SoccerTeamSide previousSide =
                    SoccerMatchManager.getPlayerTeam(
                            source.getServer(),
                            player.getUUID()
                    );

            /*
             * Si ya estaba en este mismo equipo,
             * no contamos una modificación nueva.
             */
            if (previousSide == side) {
                continue;
            }

            SoccerMatchManager.addPlayer(
                    source.getServer(),
                    player,
                    side
            );

            added++;
        }

        int finalAdded = added;

        if (added == 0) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§eTodos los jugadores seleccionados ya pertenecían al equipo §f"
                                    + SoccerMatchManager.getTeamName(
                                    source.getServer(),
                                    side
                            )
                                    + "§e."
                    ),
                    false
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aJugadores añadidos al equipo §f"
                                + SoccerMatchManager.getTeamName(
                                source.getServer(),
                                side
                        )
                                + "§a: §f"
                                + finalAdded
                ),
                true
        );

        return added;
    }

    /**
     * Elimina uno o varios jugadores de sus equipos.
     */
    private static int removePlayersFromTeams(
            CommandSourceStack source,
            Collection<ServerPlayer> players
    ) {
        if (players.isEmpty()) {
            source.sendFailure(
                    Component.literal(
                            "§cNo se encontraron jugadores."
                    )
            );

            return 0;
        }

        int removed = 0;

        for (ServerPlayer player : players) {
            boolean playerRemoved =
                    SoccerMatchManager.removePlayer(
                            source.getServer(),
                            player.getUUID()
                    );

            if (playerRemoved) {
                removed++;
            }
        }

        int finalRemoved = removed;

        if (removed == 0) {
            source.sendFailure(
                    Component.literal(
                            "§cNinguno de los jugadores seleccionados pertenecía a un equipo."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§eJugadores eliminados de sus equipos: §f"
                                + finalRemoved
                ),
                true
        );

        return removed;
    }

    private static int clearTeam(
            CommandSourceStack source,
            SoccerTeamSide side
    ) {
        int removed = SoccerMatchManager.clearTeam(
                source.getServer(),
                side
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§eEquipo "
                                + side.getDisplayName()
                                + " vaciado. Jugadores eliminados: §f"
                                + removed
                ),
                true
        );

        return Math.max(1, removed);
    }

    private static int clearAllTeams(
            CommandSourceStack source
    ) {
        int removed = SoccerMatchManager.clearAllTeams(
                source.getServer()
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§eSe limpiaron ambos equipos, los porteros y los nombres personalizados. "
                                + "Jugadores eliminados: §f"
                                + removed
                ),
                true
        );

        return Math.max(1, removed);
    }

    private static int setReferee(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        MinecraftServer server = source.getServer();

        if (SoccerMatchManager.isReferee(
                server,
                player.getUUID()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§e"
                                    + player.getGameProfile().getName()
                                    + " ya es el árbitro."
                    )
            );
            return 0;
        }

        if (!SoccerMatchManager.setReferee(
                server,
                player.getUUID()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo se pudo asignar el árbitro."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§e§l"
                                + player.getGameProfile().getName()
                                + " §aahora tiene el rol de árbitro."
                ),
                true
        );

        player.sendSystemMessage(
                Component.literal(
                        "§eHas sido asignado como árbitro de CraftersSoccer."
                )
        );

        return 1;
    }

    private static int clearReferee(
            CommandSourceStack source
    ) {
        MinecraftServer server = source.getServer();
        UUID previousReferee =
                SoccerMatchManager.getReferee(server);

        if (!SoccerMatchManager.clearReferee(server)) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un árbitro asignado."
                    )
            );
            return 0;
        }

        if (previousReferee != null) {
            ServerPlayer player =
                    server.getPlayerList().getPlayer(previousReferee);

            if (player != null) {
                player.sendSystemMessage(
                        Component.literal(
                                "§7Ya no tienes el rol de árbitro."
                        )
                );
            }
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§eSe eliminó el árbitro asignado."
                ),
                true
        );

        return 1;
    }

    private static int showRefereeStatus(
            CommandSourceStack source
    ) {
        MinecraftServer server = source.getServer();
        UUID refereeId =
                SoccerMatchManager.getReferee(server);

        if (refereeId == null) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§7No hay un árbitro asignado."
                    ),
                    false
            );
            return 0;
        }

        ServerPlayer player =
                server.getPlayerList().getPlayer(refereeId);

        String name =
                player == null
                        ? refereeId.toString()
                        : player.getGameProfile().getName();

        String connection =
                player == null
                        ? "§7(desconectado)"
                        : "§a(conectado)";

        source.sendSuccess(
                () -> Component.literal(
                        "§eÁrbitro actual: §f"
                                + name
                                + " "
                                + connection
                ),
                false
        );

        return 1;
    }

    private static int freezeFoulBall(
            CommandSourceStack source
    ) {
        if (!(source.getEntity()
                instanceof ServerPlayer referee)) {
            return 0;
        }

        RefereeManager.FoulBallResult result =
                RefereeManager.freezeBallForFoul(
                        source.getServer(),
                        referee
                );

        return sendFoulBallResult(
                source,
                result,
                true
        );
    }

    private static int releaseFoulBall(
            CommandSourceStack source
    ) {
        if (!(source.getEntity()
                instanceof ServerPlayer referee)) {
            return 0;
        }

        RefereeManager.FoulBallResult result =
                RefereeManager.releaseBallFromFoul(
                        source.getServer(),
                        referee
                );

        return sendFoulBallResult(
                source,
                result,
                false
        );
    }

    private static int showFoulBallStatus(
            CommandSourceStack source
    ) {
        boolean frozen =
                RefereeManager.isBallFrozenForFoul(
                        source.getServer()
                );

        source.sendSuccess(
                () -> Component.literal(
                        frozen
                                ? "§eBalón por falta: §cCONGELADO"
                                : "§eBalón por falta: §aLIBRE"
                ),
                false
        );

        return frozen ? 1 : 0;
    }

    private static int sendFoulBallResult(
            CommandSourceStack source,
            RefereeManager.FoulBallResult result,
            boolean freezing
    ) {
        if (result
                == RefereeManager.FoulBallResult.SUCCESS) {

            source.sendSuccess(
                    () -> Component.literal(
                            freezing
                                    ? "§c§lFALTA §7- §eBalón detenido. Solo puedes moverlo como árbitro."
                                    : "§aBalón liberado. El juego puede continuar."
                    ),
                    true
            );

            return 1;
        }

        String message =
                switch (result) {
                    case NOT_REFEREE ->
                            "§cSolo el árbitro asignado puede usar esta acción.";
                    case NO_ACTIVE_MATCH ->
                            "§cNo hay un partido activo.";
                    case NO_BALL ->
                            "§cNo se encontró el balón oficial del partido.";
                    case ALREADY_FROZEN ->
                            "§eEl balón ya está congelado por una falta.";
                    case NOT_FROZEN ->
                            "§eEl balón no está congelado por una falta.";
                    case SUCCESS ->
                            "";
                };

        source.sendFailure(
                Component.literal(
                        message
                )
        );

        return 0;
    }

    private static int listTeams(
            CommandSourceStack source
    ) {
        MinecraftServer server =
                source.getServer();

        source.sendSuccess(
                () -> Component.literal(
                        "§c§lEQUIPO ROJO §7- §f"
                                + SoccerMatchManager.getRedTeamName(server)
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
                        "§9§lEQUIPO AZUL §7- §f"
                                + SoccerMatchManager.getBlueTeamName(server)
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


    private static int adjustGoal(
            CommandSourceStack source,
            SoccerTeamSide side,
            int amount
    ) {
        SoccerMatch match =
                SoccerMatchManager.getActiveMatch(
                        source.getServer()
                );

        if (match == null) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un partido activo."
                    )
            );
            return 0;
        }

        SoccerMatchManager.adjustScore(
                source.getServer(),
                side,
                amount
        );

        int currentScore =
                side == SoccerTeamSide.RED
                        ? match.getRedScore()
                        : match.getBlueScore();

        String operation =
                amount >= 0
                        ? "agregado"
                        : "anulado";

        int absoluteAmount =
                Math.abs(amount);

        source.sendSuccess(
                () -> Component.literal(
                        "§aGol "
                                + operation
                                + " para §f"
                                + SoccerMatchManager.getTeamName(
                                source.getServer(),
                                side
                        )
                                + "§a. Cambio: §f"
                                + absoluteAmount
                                + "§a. Marcador actual: §c"
                                + match.getRedScore()
                                + " §f- §9"
                                + match.getBlueScore()
                                + " §7(Puntaje del equipo: "
                                + currentScore
                                + ")"
                ),
                true
        );

        return 1;
    }

    private static int showManualScoreStatus(
            CommandSourceStack source
    ) {
        SoccerMatch match =
                SoccerMatchManager.getActiveMatch(
                        source.getServer()
                );

        if (match == null) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un partido activo."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§7Marcador actual: §c"
                                + match.getRedScore()
                                + " §f- §9"
                                + match.getBlueScore()
                ),
                false
        );

        return 1;
    }

    private static int setAddedTime(
            CommandSourceStack source,
            int minutes
    ) {
        if (!SoccerMatchManager.setAddedTimeMinutes(
                source.getServer(),
                minutes
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un partido activo que acepte reposición."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        minutes <= 0
                                ? "§eTiempo de reposición eliminado."
                                : "§aTiempo de reposición establecido: §e+"
                                + minutes
                                + " minutos."
                ),
                true
        );

        return 1;
    }

    private static int addAddedTime(
            CommandSourceStack source,
            int minutes
    ) {
        if (!SoccerMatchManager.addAddedTimeMinutes(
                source.getServer(),
                minutes
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un partido activo que acepte reposición."
                    )
            );
            return 0;
        }

        SoccerMatch match =
                SoccerMatchManager.getActiveMatch(
                        source.getServer()
                );

        int total =
                match == null
                        ? 0
                        : match.getAddedTimeMinutes();

        source.sendSuccess(
                () -> Component.literal(
                        "§aSe agregaron §e+"
                                + minutes
                                + " minutos§a. Reposición total: §e+"
                                + total
                ),
                true
        );

        return 1;
    }

    private static int showAddedTimeStatus(
            CommandSourceStack source
    ) {
        SoccerMatch match =
                SoccerMatchManager.getActiveMatch(
                        source.getServer()
                );

        if (match == null) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay un partido activo."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§7Reposición actual: §e+"
                                + match.getAddedTimeMinutes()
                                + " minutos"
                ),
                false
        );

        return 1;
    }

    private static int setBroadcastCamera(
            CommandSourceStack source,
            int cameraNumber,
            ServerPlayer operator
    ) {
        if (SoccerMatchManager.getPlayerTeam(
                source.getServer(),
                operator.getUUID()
        ) != null) {
            source.sendFailure(
                    Component.literal(
                            "§cLa cámara asignada no puede ser un jugador "
                                    + "de uno de los equipos."
                    )
            );
            return 0;
        }

        if (!SpectatorBroadcastManager.setCameraOperator(
                source.getServer(),
                cameraNumber,
                operator
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo se pudo asignar la Cámara "
                                    + cameraNumber
                                    + ". El jugador puede estar asignado "
                                    + "ya a otra cámara."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aCámara TV "
                                + cameraNumber
                                + " asignada a §f"
                                + operator.getName().getString()
                                + "§a. Los espectadores cambian con §fB§a."
                ),
                true
        );

        return 1;
    }

    private static int clearBroadcastCamera(
            CommandSourceStack source
    ) {
        if (!SpectatorBroadcastManager.clearCameraOperator(
                source.getServer()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cNo hay cámaras TV asignadas."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§eTodas las cámaras TV fueron eliminadas."
                ),
                true
        );

        return 1;
    }

    private static int clearBroadcastCamera(
            CommandSourceStack source,
            int cameraNumber
    ) {
        if (!SpectatorBroadcastManager.clearCameraOperator(
                source.getServer(),
                cameraNumber
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cLa Cámara "
                                    + cameraNumber
                                    + " no está asignada."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§eCámara TV "
                                + cameraNumber
                                + " eliminada."
                ),
                true
        );

        return 1;
    }

    private static int showBroadcastCameraStatus(
            CommandSourceStack source
    ) {
        int active =
                SpectatorBroadcastManager
                        .getConfiguredCameraCount(
                                source.getServer()
                        );

        if (active <= 0) {
            source.sendSuccess(
                    () -> Component.literal(
                            "§7No hay cámaras TV asignadas."
                    ),
                    false
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§6§lCámaras TV activas: §f"
                                + active
                                + "§7/"
                                + SpectatorBroadcastManager.MAX_CAMERAS
                ),
                false
        );

        for (int cameraNumber = 1;
             cameraNumber
                     <= SpectatorBroadcastManager.MAX_CAMERAS;
             cameraNumber++) {

            ServerPlayer operator =
                    SpectatorBroadcastManager.getCameraOperator(
                            source.getServer(),
                            cameraNumber
                    );

            int finalCameraNumber =
                    cameraNumber;

            source.sendSuccess(
                    () -> Component.literal(
                            operator == null
                                    ? "§8- Cámara "
                                    + finalCameraNumber
                                    + ": §7vacía"
                                    : "§8- §bCámara "
                                    + finalCameraNumber
                                    + ": §f"
                                    + operator.getName()
                                    .getString()
                    ),
                    false
            );
        }

        return active;
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
                                + (match.getAddedTimeMinutes() > 0
                                ? "\n§7Reposición: §e+"
                                + match.getAddedTimeMinutes()
                                : "")
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


        RED_GOALKEEPER_AREA_POSITION_1(
                "red_goalkeeper_area_pos1",
                "Área portero rojo 1"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setRedGoalkeeperAreaPosition1(position);
            }
        },

        RED_GOALKEEPER_AREA_POSITION_2(
                "red_goalkeeper_area_pos2",
                "Área portero rojo 2"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setRedGoalkeeperAreaPosition2(position);
            }
        },

        BLUE_GOALKEEPER_AREA_POSITION_1(
                "blue_goalkeeper_area_pos1",
                "Área portero azul 1"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setBlueGoalkeeperAreaPosition1(position);
            }
        },

        BLUE_GOALKEEPER_AREA_POSITION_2(
                "blue_goalkeeper_area_pos2",
                "Área portero azul 2"
        ) {
            @Override
            void set(
                    SoccerField field,
                    BlockPos position
            ) {
                field.setBlueGoalkeeperAreaPosition2(position);
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