package org.crafterscr.crafterssoccer.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

/** Registers field administration without exposing its implementation at the root. */
public final class FieldCommands {
    private FieldCommands() { }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        var field = SoccerCommands.createFieldCommands()
                .requires(CommandAccess::isAdministrator);
        GoalkeeperCommands.register(field);
        root.then(field);
    }
}
