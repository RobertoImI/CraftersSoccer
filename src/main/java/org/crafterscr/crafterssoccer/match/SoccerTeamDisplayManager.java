package org.crafterscr.crafterssoccer.match;

import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * Controla los equipos visuales del scoreboard.
 *
 * Objetivo:
 * - El nombre del jugador se ve del color del equipo.
 * - Sirve sobre la cabeza y en el tab.
 * - Ya no usamos "TU EQUIPO" en el HUD.
 */
public final class SoccerTeamDisplayManager {

    private static final String RED_SCOREBOARD_TEAM =
            "csoccer_red";

    private static final String BLUE_SCOREBOARD_TEAM =
            "csoccer_blue";

    private SoccerTeamDisplayManager() {
    }

    public static void synchronize(
            MinecraftServer server,
            Set<UUID> redPlayers,
            Set<UUID> bluePlayers,
            String redName,
            String blueName
    ) {
        Scoreboard scoreboard =
                server.getScoreboard();

        PlayerTeam redTeam =
                getOrCreateTeam(
                        scoreboard,
                        RED_SCOREBOARD_TEAM
                );

        PlayerTeam blueTeam =
                getOrCreateTeam(
                        scoreboard,
                        BLUE_SCOREBOARD_TEAM
                );

        configureTeam(
                redTeam,
                redName,
                ChatFormatting.RED
        );

        configureTeam(
                blueTeam,
                blueName,
                ChatFormatting.BLUE
        );

        clearTeamMembers(
                scoreboard,
                redTeam
        );

        clearTeamMembers(
                scoreboard,
                blueTeam
        );

        for (UUID playerId : redPlayers) {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(playerId);

            if (player == null) {
                continue;
            }

            scoreboard.addPlayerToTeam(
                    player.getScoreboardName(),
                    redTeam
            );
        }

        for (UUID playerId : bluePlayers) {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(playerId);

            if (player == null) {
                continue;
            }

            scoreboard.addPlayerToTeam(
                    player.getScoreboardName(),
                    blueTeam
            );
        }
    }

    public static void removePlayer(
            MinecraftServer server,
            ServerPlayer player
    ) {
        Scoreboard scoreboard =
                server.getScoreboard();

        PlayerTeam currentTeam =
                scoreboard.getPlayersTeam(
                        player.getScoreboardName()
                );

        if (isSoccerTeam(currentTeam)) {
            scoreboard.removePlayerFromTeam(
                    player.getScoreboardName()
            );
        }
    }

    public static void clearAll(
            MinecraftServer server
    ) {
        Scoreboard scoreboard =
                server.getScoreboard();

        PlayerTeam redTeam =
                scoreboard.getPlayerTeam(
                        RED_SCOREBOARD_TEAM
                );

        PlayerTeam blueTeam =
                scoreboard.getPlayerTeam(
                        BLUE_SCOREBOARD_TEAM
                );

        if (redTeam != null) {
            scoreboard.removePlayerTeam(
                    redTeam
            );
        }

        if (blueTeam != null) {
            scoreboard.removePlayerTeam(
                    blueTeam
            );
        }
    }

    private static PlayerTeam getOrCreateTeam(
            Scoreboard scoreboard,
            String internalName
    ) {
        PlayerTeam team =
                scoreboard.getPlayerTeam(
                        internalName
                );

        if (team == null) {
            team =
                    scoreboard.addPlayerTeam(
                            internalName
                    );
        }

        return team;
    }

    private static void configureTeam(
            PlayerTeam team,
            String visibleName,
            ChatFormatting color
    ) {
        team.setColor(color);

        team.setDisplayName(
                Component.literal(
                        visibleName
                )
        );

        /*
         * No mostramos prefijo extra.
         * Lo importante es que el nombre del jugador
         * tome el color del equipo.
         */
        team.setPlayerPrefix(
                Component.empty()
        );

        team.setPlayerSuffix(
                Component.empty()
        );

        team.setNameTagVisibility(
                Team.Visibility.ALWAYS
        );

        team.setSeeFriendlyInvisibles(
                false
        );

        team.setCollisionRule(
                Team.CollisionRule.ALWAYS
        );

        team.setAllowFriendlyFire(true);
    }

    private static void clearTeamMembers(
            Scoreboard scoreboard,
            PlayerTeam team
    ) {
        for (String member
                : Set.copyOf(team.getPlayers())) {

            scoreboard.removePlayerFromTeam(
                    member,
                    team
            );
        }
    }

    private static boolean isSoccerTeam(
            PlayerTeam team
    ) {
        if (team == null) {
            return false;
        }

        String name =
                team.getName();

        return RED_SCOREBOARD_TEAM.equals(name)
                || BLUE_SCOREBOARD_TEAM.equals(name);
    }
}