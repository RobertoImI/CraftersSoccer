package org.crafterscr.crafterssoccer.command;

import org.crafterscr.crafterssoccer.match.SoccerMatchManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** Shared access rules for soccer command groups. */
final class CommandAccess {
    private CommandAccess() { }

    static boolean isAdministrator(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    static boolean canControlMatch(CommandSourceStack source) {
        if (isAdministrator(source)) return true;
        return isAssignedReferee(source);
    }

    static boolean isAssignedReferee(CommandSourceStack source) {
        if (!(source.getEntity()
                instanceof ServerPlayer player)) {
            return false;
        }

        return SoccerMatchManager.isReferee(
                source.getServer(),
                player.getUUID()
        );
    }
}
