package org.crafterscr.crafterssoccer.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

/** Registers broadcast-camera administration commands. */
public final class BroadcastCommands {
    private BroadcastCommands() { }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(SoccerCommands.createBroadcastCameraCommands()
                .requires(CommandAccess::isAdministrator));
    }
}
