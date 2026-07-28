package org.crafterscr.crafterssoccer.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

/** Registers team administration commands. */
public final class TeamCommands {
    private TeamCommands() { }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(SoccerCommands.createTeamCommands()
                .requires(CommandAccess::isAdministrator));
    }
}

