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
 * Incluye:
 * - equipo rojo;
 * - equipo azul;
 * - portero rojo;
 * - portero azul;
 * - árbitro.
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

        PlayerTeam redGoalkeeperTeam =
                getOrCreateTeam(
                        scoreboard,
                        RED_GOALKEEPER_TEAM
                );

        PlayerTeam blueGoalkeeperTeam =
                getOrCreateTeam(
                        scoreboard,
                        BLUE_GOALKEEPER_TEAM
                );

        PlayerTeam refereeTeam =
                getOrCreateTeam(
                        scoreboard,
                        REFEREE_SCOREBOARD_TEAM
                );

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
                Component.literal("[P] ")
                        .withStyle(
                                ChatFormatting.GOLD
                        )
        );

        configureTeam(
                blueGoalkeeperTeam,
                blueName + " - Portero",
                ChatFormatting.BLUE,
                Component.literal("[P] ")
                        .withStyle(
                                ChatFormatting.GOLD
                        )
        );

        configureTeam(
                refereeTeam,
                "Árbitro",
                ChatFormatting.YELLOW,
                Component.literal("[ÁRBITRO] ")
                        .withStyle(
                                ChatFormatting.YELLOW
                        )
        );

        clearTeamMembers(
                scoreboard,
                redTeam
        );

        clearTeamMembers(
                scoreboard,
                blueTeam
        );

        clearTeamMembers(
                scoreboard,
                redGoalkeeperTeam
        );

        clearTeamMembers(
                scoreboard,
                blueGoalkeeperTeam
        );

        clearTeamMembers(
                scoreboard,
                refereeTeam
        );

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
                    server.getPlayerList()
                            .getPlayer(
                                    referee
                            );

            if (refereePlayer != null) {
                scoreboard.addPlayerToTeam(
                        refereePlayer
                                .getScoreboardName(),
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
                    server.getPlayerList()
                            .getPlayer(
                                    playerId
                            );

            if (player == null) {
                continue;
            }

            PlayerTeam targetTeam =
                    playerId.equals(
                            goalkeeper
                    )
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

        removeTeamIfPresent(
                scoreboard,
                RED_SCOREBOARD_TEAM
        );

        removeTeamIfPresent(
                scoreboard,
                BLUE_SCOREBOARD_TEAM
        );

        removeTeamIfPresent(
                scoreboard,
                RED_GOALKEEPER_TEAM
        );

        removeTeamIfPresent(
                scoreboard,
                BLUE_GOALKEEPER_TEAM
        );

        removeTeamIfPresent(
                scoreboard,
                REFEREE_SCOREBOARD_TEAM
        );
    }

    private static void removeTeamIfPresent(
            Scoreboard scoreboard,
            String internalName
    ) {
        PlayerTeam team =
                scoreboard.getPlayerTeam(
                        internalName
                );

        if (team != null) {
            scoreboard.removePlayerTeam(
                    team
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
        team.setColor(
                color
        );

        team.setDisplayName(
                Component.literal(
                        visibleName
                )
        );

        team.setPlayerPrefix(
                prefix
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

        team.setAllowFriendlyFire(
                true
        );
    }

    private static void clearTeamMembers(
            Scoreboard scoreboard,
            PlayerTeam team
    ) {
        for (String member
                : Set.copyOf(
                        team.getPlayers()
                )) {

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
