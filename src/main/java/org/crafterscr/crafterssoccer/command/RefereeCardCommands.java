package org.crafterscr.crafterssoccer.command;

import org.crafterscr.crafterssoccer.referee.RefereeCardManager;
import org.crafterscr.crafterssoccer.referee.RefereePenaltyConfig;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos administrativos del sistema de tarjetas.
 */
public final class RefereeCardCommands {

    private RefereeCardCommands() {
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
                        .then(
                                Commands.literal("referee")
                                        .then(
                                                Commands.literal("cards")
                                                        .then(
                                                                Commands.literal("give")
                                                                        .executes(
                                                                                context ->
                                                                                        giveCards(
                                                                                                context.getSource()
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("status")
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "player",
                                                                                                EntityArgument.player()
                                                                                        )
                                                                                        .executes(
                                                                                                context ->
                                                                                                        showStatus(
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
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "player",
                                                                                                EntityArgument.player()
                                                                                        )
                                                                                        .executes(
                                                                                                context ->
                                                                                                        clearPlayer(
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
                                                                Commands.literal("clearall")
                                                                        .executes(
                                                                                context ->
                                                                                        clearAll(
                                                                                                context.getSource()
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("redtime")
                                                        .then(
                                                                Commands.argument(
                                                                                "minutes",
                                                                                IntegerArgumentType.integer(
                                                                                        RefereePenaltyConfig.MIN_MINUTES,
                                                                                        RefereePenaltyConfig.MAX_MINUTES
                                                                                )
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        setRedTime(
                                                                                                context.getSource(),
                                                                                                IntegerArgumentType.getInteger(
                                                                                                        context,
                                                                                                        "minutes"
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("pardon")
                                                        .then(
                                                                Commands.argument(
                                                                                "player",
                                                                                EntityArgument.player()
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        pardon(
                                                                                                context.getSource(),
                                                                                                EntityArgument.getPlayer(
                                                                                                        context,
                                                                                                        "player"
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int giveCards(
            CommandSourceStack source
    ) {
        ServerPlayer referee;

        try {
            referee = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "§cEste comando debe ejecutarlo el árbitro."
                    )
            );
            return 0;
        }

        RefereeCardManager.ensureRefereeCards(
                source.getServer(),
                referee
        );

        source.sendSuccess(
                () -> Component.literal(
                        "§aSe comprobaron las tarjetas del árbitro."
                ),
                false
        );

        return 1;
    }

    private static int showStatus(
            CommandSourceStack source,
            ServerPlayer target
    ) {
        RefereeCardManager.CardStatus status =
                RefereeCardManager.getStatus(
                        source.getServer(),
                        target.getUUID()
                );

        String remaining =
                status.expelled()
                        ? "§7, restante=§f"
                        + formatTicks(
                        status.remainingTicks()
                )
                        : "";

        source.sendSuccess(
                () -> Component.literal(
                        "§eSanciones de §f"
                                + target.getGameProfile().getName()
                                + "§7: amarillas=§e"
                                + status.yellowCards()
                                + "§7, expulsado="
                                + (status.expelled()
                                ? "§cSÍ"
                                : "§aNO")
                                + remaining
                ),
                false
        );

        return 1;
    }

    private static int clearPlayer(
            CommandSourceStack source,
            ServerPlayer target
    ) {
        boolean changed =
                RefereeCardManager.clearPlayer(
                        source.getServer(),
                        target.getUUID()
                );

        source.sendSuccess(
                () -> Component.literal(
                        changed
                                ? "§aSanciones eliminadas de §f"
                                + target.getGameProfile().getName()
                                : "§7Ese jugador no tenía sanciones."
                ),
                true
        );

        return changed ? 1 : 0;
    }

    private static int clearAll(
            CommandSourceStack source
    ) {
        int affected =
                RefereeCardManager.clearAll(
                        source.getServer()
                );

        source.sendSuccess(
                () -> Component.literal(
                        "§aSanciones eliminadas. Jugadores afectados: §f"
                                + affected
                ),
                true
        );

        return Math.max(
                1,
                affected
        );
    }

    private static int setRedTime(
            CommandSourceStack source,
            int minutes
    ) {
        int saved =
                RefereePenaltyConfig.setRedCardMinutes(
                        source.getServer(),
                        minutes
                );

        source.sendSuccess(
                () -> Component.literal(
                        "§aLa expulsión roja durará §f"
                                + saved
                                + " minuto"
                                + (saved == 1 ? "" : "s")
                                + "§a."
                ),
                true
        );

        return 1;
    }

    private static int pardon(
            CommandSourceStack source,
            ServerPlayer target
    ) {
        if (!RefereeCardManager.pardon(
                source.getServer(),
                target.getUUID()
        )) {
            source.sendFailure(
                    Component.literal(
                            "§cEse jugador no está expulsado."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aSe perdonó la expulsión de §f"
                                + target.getGameProfile().getName()
                ),
                true
        );

        return 1;
    }

    private static String formatTicks(
            int ticks
    ) {
        int totalSeconds =
                Math.max(0, ticks / 20);

        return String.format(
                java.util.Locale.ROOT,
                "%02d:%02d",
                totalSeconds / 60,
                totalSeconds % 60
        );
    }
}
