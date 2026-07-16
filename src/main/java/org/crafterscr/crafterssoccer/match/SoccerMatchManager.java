package org.crafterscr.crafterssoccer.match;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.field.SoccerField;
import org.crafterscr.crafterssoccer.field.SoccerFieldManager;
import org.crafterscr.crafterssoccer.network.MatchStatePayload;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Administra el partido, los equipos y sus nombres.
 */
public final class SoccerMatchManager {

    private static final String DEFAULT_RED_NAME =
            "Equipo Rojo";

    private static final String DEFAULT_BLUE_NAME =
            "Equipo Azul";

    private static final Set<UUID> RED_PLAYERS =
            new LinkedHashSet<>();

    private static final Set<UUID> BLUE_PLAYERS =
            new LinkedHashSet<>();

    private static MinecraftServer currentServer;
    private static SoccerMatch activeMatch;

    private static String redTeamName =
            DEFAULT_RED_NAME;

    private static String blueTeamName =
            DEFAULT_BLUE_NAME;

    private static int synchronizationTicker;

    private SoccerMatchManager() {
    }

    private static void ensureServer(
            MinecraftServer server
    ) {
        if (currentServer == server) {
            return;
        }

        currentServer = server;
        activeMatch = null;

        RED_PLAYERS.clear();
        BLUE_PLAYERS.clear();

        redTeamName =
                DEFAULT_RED_NAME;

        blueTeamName =
                DEFAULT_BLUE_NAME;

        synchronizationTicker = 0;
    }

    public static boolean addPlayer(
            MinecraftServer server,
            ServerPlayer player,
            SoccerTeamSide side
    ) {
        ensureServer(server);

        RED_PLAYERS.remove(
                player.getUUID()
        );

        BLUE_PLAYERS.remove(
                player.getUUID()
        );

        boolean added =
                getTeam(side).add(
                        player.getUUID()
                );

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName
        );

        synchronize(server);

        return added;
    }

    public static boolean removePlayer(
            MinecraftServer server,
            UUID playerId
    ) {
        ensureServer(server);

        boolean removedRed =
                RED_PLAYERS.remove(playerId);

        boolean removedBlue =
                BLUE_PLAYERS.remove(playerId);

        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(playerId);

        if (player != null) {
            SoccerTeamDisplayManager.removePlayer(
                    server,
                    player
            );
        }

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName
        );

        synchronize(server);

        return removedRed || removedBlue;
    }

    public static boolean setTeamName(
            MinecraftServer server,
            SoccerTeamSide side,
            String requestedName
    ) {
        ensureServer(server);

        String safeName =
                sanitizeTeamName(
                        requestedName
                );

        if (safeName.isBlank()) {
            return false;
        }

        if (side == SoccerTeamSide.RED) {
            redTeamName = safeName;

        } else {
            blueTeamName = safeName;
        }

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName
        );

        synchronize(server);

        return true;
    }

    public static String getRedTeamName(
            MinecraftServer server
    ) {
        ensureServer(server);
        return redTeamName;
    }

    public static String getBlueTeamName(
            MinecraftServer server
    ) {
        ensureServer(server);
        return blueTeamName;
    }

    public static String getTeamName(
            MinecraftServer server,
            SoccerTeamSide side
    ) {
        return side == SoccerTeamSide.RED
                ? getRedTeamName(server)
                : getBlueTeamName(server);
    }

    public static Set<UUID> getRedPlayers(
            MinecraftServer server
    ) {
        ensureServer(server);
        return Set.copyOf(RED_PLAYERS);
    }

    public static Set<UUID> getBluePlayers(
            MinecraftServer server
    ) {
        ensureServer(server);
        return Set.copyOf(BLUE_PLAYERS);
    }

    public static SoccerTeamSide getPlayerTeam(
            MinecraftServer server,
            UUID playerId
    ) {
        ensureServer(server);

        if (RED_PLAYERS.contains(playerId)) {
            return SoccerTeamSide.RED;
        }

        if (BLUE_PLAYERS.contains(playerId)) {
            return SoccerTeamSide.BLUE;
        }

        return null;
    }

    public static SoccerMatch getActiveMatch(
            MinecraftServer server
    ) {
        ensureServer(server);
        return activeMatch;
    }

    public static boolean startMatch(
            MinecraftServer server,
            SoccerField field,
            int minutes
    ) {
        ensureServer(server);

        if (activeMatch != null) {
            return false;
        }

        int durationTicks =
                minutes * 60 * 20;

        activeMatch =
                new SoccerMatch(
                        field.getId(),
                        durationTicks
                );

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName
        );

        activeMatch.start(
                server,
                field,
                RED_PLAYERS,
                BLUE_PLAYERS
        );

        synchronize(server);

        return true;
    }

    public static boolean pauseMatch(
            MinecraftServer server
    ) {
        ensureServer(server);

        if (activeMatch == null) {
            return false;
        }

        activeMatch.pause();
        synchronize(server);

        return true;
    }

    public static boolean resumeMatch(
            MinecraftServer server
    ) {
        ensureServer(server);

        if (activeMatch == null) {
            return false;
        }

        activeMatch.resume();
        synchronize(server);

        return true;
    }

    public static boolean stopMatch(
            MinecraftServer server
    ) {
        ensureServer(server);

        if (activeMatch == null) {
            return false;
        }

        SoccerField field =
                SoccerFieldManager.getField(
                        server,
                        activeMatch.getFieldId()
                );

        if (field != null) {
            activeMatch.removeOfficialBall(
                    server,
                    field
            );
        }

        activeMatch = null;

        sendInactiveState(server);

        return true;
    }

    public static void tick(
            MinecraftServer server
    ) {
        ensureServer(server);

        if (activeMatch == null) {
            return;
        }

        SoccerField field =
                SoccerFieldManager.getField(
                        server,
                        activeMatch.getFieldId()
                );

        if (field == null) {
            stopMatch(server);
            return;
        }

        activeMatch.tick(
                server,
                field,
                RED_PLAYERS,
                BLUE_PLAYERS
        );

        synchronizationTicker++;

        if (synchronizationTicker >= 10) {
            synchronizationTicker = 0;
            synchronize(server);
        }

        if (activeMatch.shouldBeRemoved()) {
            stopMatch(server);
        }
    }

    public static void synchronizePlayer(
            MinecraftServer server,
            ServerPlayer player
    ) {
        ensureServer(server);

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName
        );

        if (activeMatch == null) {
            PacketDistributor.sendToPlayer(
                    player,
                    MatchStatePayload.inactive()
            );

            return;
        }

        PacketDistributor.sendToPlayer(
                player,
                createPayload(
                        server,
                        activeMatch,
                        player
                )
        );
    }

    public static void synchronize(
            MinecraftServer server
    ) {
        ensureServer(server);

        if (activeMatch == null) {
            sendInactiveState(server);
            return;
        }

        for (ServerPlayer player
                : server.getPlayerList()
                .getPlayers()) {

            PacketDistributor.sendToPlayer(
                    player,
                    createPayload(
                            server,
                            activeMatch,
                            player
                    )
            );
        }
    }

    private static MatchStatePayload createPayload(
            MinecraftServer server,
            SoccerMatch match,
            ServerPlayer player
    ) {
        SoccerTeamSide playerSide =
                getPlayerTeam(
                        server,
                        player.getUUID()
                );

        boolean participant =
                playerSide != null;

        return new MatchStatePayload(
                true,
                match.getFieldId(),
                redTeamName,
                blueTeamName,
                match.getRedScore(),
                match.getBlueScore(),
                match.getRemainingTicks(),
                match.getState().name(),
                match.getMessage(),
                participant,
                playerSide == null
                        ? ""
                        : playerSide.name()
        );
    }

    private static void sendInactiveState(
            MinecraftServer server
    ) {
        MatchStatePayload payload =
                MatchStatePayload.inactive();

        for (ServerPlayer player
                : server.getPlayerList()
                .getPlayers()) {

            PacketDistributor.sendToPlayer(
                    player,
                    payload
            );
        }
    }

    private static Set<UUID> getTeam(
            SoccerTeamSide side
    ) {
        return side == SoccerTeamSide.RED
                ? RED_PLAYERS
                : BLUE_PLAYERS;
    }

    private static String sanitizeTeamName(
            String requestedName
    ) {
        if (requestedName == null) {
            return "";
        }

        /*
         * Quitar códigos de formato de Minecraft
         * introducidos con el símbolo §.
         */
        String cleaned =
                requestedName
                        .replaceAll(
                                "§.",
                                ""
                        )
                        .trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (cleaned.length() > 24) {
            cleaned =
                    cleaned.substring(
                            0,
                            24
                    );
        }

        return cleaned;
    }
}