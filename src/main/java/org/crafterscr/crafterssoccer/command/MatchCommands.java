package org.crafterscr.crafterssoccer.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

/** Registers match lifecycle and referee-controlled match commands. */
public final class MatchCommands {
    private MatchCommands() { }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(SoccerCommands.createMatchCommands()
                .requires(CommandAccess::isAdministrator));
        root.then(SoccerCommands.createGoalControlCommands()
                .requires(CommandAccess::canControlMatch));
        root.then(SoccerCommands.createAddedTimeCommands()
                .requires(CommandAccess::canControlMatch));
    }
}
