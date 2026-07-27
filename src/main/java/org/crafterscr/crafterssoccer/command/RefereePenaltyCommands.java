package org.crafterscr.crafterssoccer.command;

import org.crafterscr.crafterssoccer.CraftersSoccer;
import org.crafterscr.crafterssoccer.field.SoccerField;
import org.crafterscr.crafterssoccer.field.SoccerFieldManager;
import org.crafterscr.crafterssoccer.match.SoccerTeamSide;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Puntos de expulsión de cada cancha.
 *
 * Se registra automáticamente y no necesita modificar
 * CraftersSoccer.java.
 */
@EventBusSubscriber(
        modid = CraftersSoccer.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME
)
public final class RefereePenaltyCommands {

    private static final com.mojang.brigadier.suggestion
            .SuggestionProvider<CommandSourceStack>
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

    private RefereePenaltyCommands() {
    }

    @SubscribeEvent
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
                        .then(
                                Commands.literal("field")
                                        .then(
                                                Commands.literal("penalty")
                                                        .then(
                                                                teamBranch(
                                                                        "red",
                                                                        SoccerTeamSide.RED
                                                                )
                                                        )
                                                        .then(
                                                                teamBranch(
                                                                        "blue",
                                                                        SoccerTeamSide.BLUE
                                                                )
                                                        )
                                                        .then(
                                                                Commands.literal("clear")
                                                                        .then(
                                                                                clearBranch(
                                                                                        "red",
                                                                                        SoccerTeamSide.RED
                                                                                )
                                                                        )
                                                                        .then(
                                                                                clearBranch(
                                                                                        "blue",
                                                                                        SoccerTeamSide.BLUE
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    teamBranch(
            String name,
            SoccerTeamSide side
    ) {
        return Commands.literal(name)
                .then(
                        Commands.argument(
                                        "field",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        FIELD_SUGGESTIONS
                                )
                                .executes(
                                        context ->
                                                setPenalty(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context,
                                                                "field"
                                                        ),
                                                        side
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    clearBranch(
            String name,
            SoccerTeamSide side
    ) {
        return Commands.literal(name)
                .then(
                        Commands.argument(
                                        "field",
                                        StringArgumentType.word()
                                )
                                .suggests(
                                        FIELD_SUGGESTIONS
                                )
                                .executes(
                                        context ->
                                                clearPenalty(
                                                        context.getSource(),
                                                        StringArgumentType.getString(
                                                                context,
                                                                "field"
                                                        ),
                                                        side
                                                )
                                )
                );
    }

    private static int setPenalty(
            CommandSourceStack source,
            String fieldId,
            SoccerTeamSide side
    ) {
        SoccerField field =
                SoccerFieldManager.getField(
                        source.getServer(),
                        fieldId
                );

        if (field == null) {
            source.sendFailure(
                    Component.literal(
                            "§cNo existe la cancha §f"
                                    + fieldId
                    )
            );
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
                BlockPos.containing(
                        source.getPosition()
                );

        if (side == SoccerTeamSide.RED) {
            field.setRedPenaltyPosition(position);
        } else {
            field.setBluePenaltyPosition(position);
        }

        SoccerFieldManager.save(
                source.getServer()
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§aZona de expulsión "
                                + side.getDisplayName()
                                + " definida en §f"
                                + position.getX()
                                + " "
                                + position.getY()
                                + " "
                                + position.getZ()
                ),
                true
        );

        return 1;
    }

    private static int clearPenalty(
            CommandSourceStack source,
            String fieldId,
            SoccerTeamSide side
    ) {
        SoccerField field =
                SoccerFieldManager.getField(
                        source.getServer(),
                        fieldId
                );

        if (field == null) {
            source.sendFailure(
                    Component.literal(
                            "§cNo existe la cancha §f"
                                    + fieldId
                    )
            );
            return 0;
        }

        if (side == SoccerTeamSide.RED) {
            field.setRedPenaltyPosition(null);
        } else {
            field.setBluePenaltyPosition(null);
        }

        SoccerFieldManager.save(
                source.getServer()
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§eZona de expulsión "
                                + side.getDisplayName()
                                + " eliminada."
                ),
                true
        );

        return 1;
    }
}
