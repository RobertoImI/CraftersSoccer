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

    private static final String RED_GOALKEEPER_TEAM =
            "csoccer_red_gk";

    private static final String BLUE_GOALKEEPER_TEAM =
            "csoccer_blue_gk";

    private static final String REFEREE_SCOREBOARD_TEAM =
            "csoccer_referee";

    private SoccerTeamDisplayManager() {
    }

    public static void synchronize(
            MinecraftServer server,
            Set<UUID> redPlayers,
            Set<UUID> bluePlayers,
            String redName,
            String blueName,
            UUID redGoalkeeper,
            UUID blueGoalkeeper,
            UUID referee
    ) {
        Scoreboard scoreboard = server.getScoreboard();

        PlayerTeam redTeam =
                getOrCreateTeam(scoreboard, RED_SCOREBOARD_TEAM);

        PlayerTeam blueTeam =
                getOrCreateTeam(scoreboard, BLUE_SCOREBOARD_TEAM);

        PlayerTeam redGoalkeeperTeam =
                getOrCreateTeam(scoreboard, RED_GOALKEEPER_TEAM);

        PlayerTeam blueGoalkeeperTeam =
                getOrCreateTeam(scoreboard, BLUE_GOALKEEPER_TEAM);

        PlayerTeam refereeTeam =
                getOrCreateTeam(scoreboard, REFEREE_SCOREBOARD_TEAM);

        configureTeam(
                redTeam,
                redName,
                ChatFormatting.RED,
                Component.empty()
        );

        configureTeam(
                blueTeam,
                blueName,
                ChatFormatting.BLUE,
                Component.empty()
        );

        configureTeam(
                redGoalkeeperTeam,
                redName + " - Portero",
                ChatFormatting.RED,
                Component.literal("[PT] ")
                        .withStyle(ChatFormatting.GOLD)
        );

        configureTeam(
                blueGoalkeeperTeam,
                blueName + " - Portero",
                ChatFormatting.BLUE,
                Component.literal("[PT] ")
                        .withStyle(ChatFormatting.GOLD)
        );

        configureTeam(
                refereeTeam,
                "Árbitro",
                ChatFormatting.YELLOW,
                Component.literal("[ÁRBITRO] ")
                        .withStyle(ChatFormatting.YELLOW)
        );

        clearTeamMembers(scoreboard, redTeam);
        clearTeamMembers(scoreboard, blueTeam);
        clearTeamMembers(scoreboard, redGoalkeeperTeam);
        clearTeamMembers(scoreboard, blueGoalkeeperTeam);
        clearTeamMembers(scoreboard, refereeTeam);

        addPlayers(
                server,
                scoreboard,
                redTeam,
                redGoalkeeperTeam,
                redPlayers,
                redGoalkeeper
        );

        addPlayers(
                server,
                scoreboard,
                blueTeam,
                blueGoalkeeperTeam,
                bluePlayers,
                blueGoalkeeper
        );

        if (referee != null) {
            ServerPlayer refereePlayer =
                    server.getPlayerList().getPlayer(referee);

            if (refereePlayer != null) {
                scoreboard.addPlayerToTeam(
                        refereePlayer.getScoreboardName(),
                        refereeTeam
                );
            }
        }
    }

    private static void addPlayers(
            MinecraftServer server,
            Scoreboard scoreboard,
            PlayerTeam normalTeam,
            PlayerTeam goalkeeperTeam,
            Set<UUID> players,
            UUID goalkeeper
    ) {
        for (UUID playerId : players) {
            ServerPlayer player =
                    server.getPlayerList().getPlayer(playerId);

            if (player == null) {
                continue;
            }

            PlayerTeam targetTeam =
                    playerId.equals(goalkeeper)
                            ? goalkeeperTeam
                            : normalTeam;

            scoreboard.addPlayerToTeam(
                    player.getScoreboardName(),
                    targetTeam
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

        PlayerTeam redGoalkeeperTeam =
                scoreboard.getPlayerTeam(
                        RED_GOALKEEPER_TEAM
                );

        PlayerTeam blueGoalkeeperTeam =
                scoreboard.getPlayerTeam(
                        BLUE_GOALKEEPER_TEAM
                );

        PlayerTeam refereeTeam =
                scoreboard.getPlayerTeam(
                        REFEREE_SCOREBOARD_TEAM
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

        if (redGoalkeeperTeam != null) {
            scoreboard.removePlayerTeam(
                    redGoalkeeperTeam
            );
        }

        if (blueGoalkeeperTeam != null) {
            scoreboard.removePlayerTeam(
                    blueGoalkeeperTeam
            );
        }

        if (refereeTeam != null) {
            scoreboard.removePlayerTeam(
                    refereeTeam
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
            ChatFormatting color,
            Component prefix
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
        team.setPlayerPrefix(prefix);

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
                || BLUE_SCOREBOARD_TEAM.equals(name)
                || RED_GOALKEEPER_TEAM.equals(name)
                || BLUE_GOALKEEPER_TEAM.equals(name)
                || REFEREE_SCOREBOARD_TEAM.equals(name);
    }
}