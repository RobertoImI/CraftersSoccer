package org.crafterscr.crafterssoccer.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.crafterscr.crafterssoccer.match.SoccerTeamSide;

/** Boundary for goalkeeper command registration during the incremental extraction. */
public final class GoalkeeperCommands {
    private GoalkeeperCommands() { }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> field) {
        field.then(Commands.literal("goalkeeperarea")
                .then(SoccerCommands.createGoalkeeperAreaCommands(
                        "red", SoccerTeamSide.RED))
                .then(SoccerCommands.createGoalkeeperAreaCommands(
                        "blue", SoccerTeamSide.BLUE)));
    }
}
