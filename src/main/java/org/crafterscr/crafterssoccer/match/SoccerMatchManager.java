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
 * Administra el partido, las plantillas y los nombres.
 *
 * Las plantillas y nombres se guardan permanentemente en teams.json.
 * El partido activo, el tiempo y el marcador continúan siendo temporales.
 */
public final class SoccerMatchManager {

    public static final String DEFAULT_RED_NAME = "Equipo Rojo";
    public static final String DEFAULT_BLUE_NAME = "Equipo Azul";

    private static final Set<UUID> RED_PLAYERS =
            new LinkedHashSet<>();

    private static final Set<UUID> BLUE_PLAYERS =
            new LinkedHashSet<>();

    private static MinecraftServer currentServer;
    private static boolean loaded;

    private static SoccerMatch activeMatch;

    private static String redTeamName = DEFAULT_RED_NAME;
    private static String blueTeamName = DEFAULT_BLUE_NAME;

    /*
     * Se guardan desde este bloque para que el bloque 2 del portero
     * pueda utilizarlos sin cambiar el formato del JSON nuevamente.
     */
    private static UUID redGoalkeeper;
    private static UUID blueGoalkeeper;

    /**
     * Árbitro persistente del servidor.
     * Puede usar sus habilidades aunque no exista un partido activo.
     */
    private static UUID referee;

    private static int synchronizationTicker;

    private SoccerMatchManager() {
    }

    public static void ensureLoaded(
            MinecraftServer server
    ) {
        if (loaded && currentServer == server) {
            return;
        }

        currentServer = server;
        loaded = true;
        activeMatch = null;
        synchronizationTicker = 0;

        RED_PLAYERS.clear();
        BLUE_PLAYERS.clear();

        SoccerTeamStorage.TeamData data =
                SoccerTeamStorage.load(server);

        redTeamName = data.redTeamName();
        blueTeamName = data.blueTeamName();

        RED_PLAYERS.addAll(data.redPlayers());
        BLUE_PLAYERS.addAll(data.bluePlayers());

        redGoalkeeper = data.redGoalkeeper();
        blueGoalkeeper = data.blueGoalkeeper();
        referee = data.referee();

        normalizeLoadedData();

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName,
                redGoalkeeper,
                blueGoalkeeper,
                referee
        );
    }

    public static boolean addPlayer(
            MinecraftServer server,
            ServerPlayer player,
            SoccerTeamSide side
    ) {
        ensureLoaded(server);

        UUID playerId = player.getUUID();

        /*
         * El árbitro no puede pertenecer a un equipo.
         * Primero debe quitarse el rol de árbitro.
         */
        if (playerId.equals(referee)) {
            return false;
        }

        SoccerTeamSide previousSide = getPlayerTeam(server, playerId);

        if (previousSide == side) {
            return false;
        }

        RED_PLAYERS.remove(playerId);
        BLUE_PLAYERS.remove(playerId);

        /* Si cambia de equipo deja de ser portero del equipo anterior. */
        if (playerId.equals(redGoalkeeper)) {
            redGoalkeeper = null;
        }

        if (playerId.equals(blueGoalkeeper)) {
            blueGoalkeeper = null;
        }

        getMutableTeam(side).add(playerId);

        saveAndSynchronize(server);
        return true;
    }

    public static boolean removePlayer(
            MinecraftServer server,
            UUID playerId
    ) {
        ensureLoaded(server);

        boolean removed =
                RED_PLAYERS.remove(playerId)
                        | BLUE_PLAYERS.remove(playerId);

        if (playerId.equals(redGoalkeeper)) {
            redGoalkeeper = null;
        }

        if (playerId.equals(blueGoalkeeper)) {
            blueGoalkeeper = null;
        }

        if (!removed) {
            return false;
        }

        saveAndSynchronize(server);
        return true;
    }

    /**
     * Vacía un solo equipo.
     * Conserva su nombre personalizado para poder reutilizarlo.
     */
    public static int clearTeam(
            MinecraftServer server,
            SoccerTeamSide side
    ) {
        ensureLoaded(server);

        Set<UUID> team = getMutableTeam(side);
        int removed = team.size();
        team.clear();

        if (side == SoccerTeamSide.RED) {
            redGoalkeeper = null;
        } else {
            blueGoalkeeper = null;
        }

        saveAndSynchronize(server);
        return removed;
    }

    /**
     * Vacía ambos equipos, elimina porteros y restaura nombres.
     * No elimina la cancha ni detiene automáticamente el servidor.
     */
    public static int clearAllTeams(
            MinecraftServer server
    ) {
        ensureLoaded(server);

        int removed = RED_PLAYERS.size() + BLUE_PLAYERS.size();

        RED_PLAYERS.clear();
        BLUE_PLAYERS.clear();

        redGoalkeeper = null;
        blueGoalkeeper = null;

        redTeamName = DEFAULT_RED_NAME;
        blueTeamName = DEFAULT_BLUE_NAME;

        SoccerTeamDisplayManager.clearAll(server);
        save(server);
        synchronize(server);

        return removed;
    }

    public static boolean setTeamName(
            MinecraftServer server,
            SoccerTeamSide side,
            String requestedName
    ) {
        ensureLoaded(server);

        String safeName = sanitizeTeamName(requestedName);

        if (safeName.isBlank()) {
            return false;
        }

        if (side == SoccerTeamSide.RED) {
            redTeamName = safeName;
        } else {
            blueTeamName = safeName;
        }

        saveAndSynchronize(server);
        return true;
    }

    public static String getRedTeamName(
            MinecraftServer server
    ) {
        ensureLoaded(server);
        return redTeamName;
    }

    public static String getBlueTeamName(
            MinecraftServer server
    ) {
        ensureLoaded(server);
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
        ensureLoaded(server);
        return Set.copyOf(RED_PLAYERS);
    }

    public static Set<UUID> getBluePlayers(
            MinecraftServer server
    ) {
        ensureLoaded(server);
        return Set.copyOf(BLUE_PLAYERS);
    }

    public static SoccerTeamSide getPlayerTeam(
            MinecraftServer server,
            UUID playerId
    ) {
        ensureLoaded(server);

        if (RED_PLAYERS.contains(playerId)) {
            return SoccerTeamSide.RED;
        }

        if (BLUE_PLAYERS.contains(playerId)) {
            return SoccerTeamSide.BLUE;
        }

        return null;
    }

    public static UUID getGoalkeeper(
            MinecraftServer server,
            SoccerTeamSide side
    ) {
        ensureLoaded(server);
        return side == SoccerTeamSide.RED
                ? redGoalkeeper
                : blueGoalkeeper;
    }

    public static boolean setGoalkeeper(
            MinecraftServer server,
            SoccerTeamSide side,
            UUID playerId
    ) {
        ensureLoaded(server);

        if (playerId != null
                && (!getMutableTeam(side).contains(playerId)
                || playerId.equals(referee))) {
            return false;
        }

        if (side == SoccerTeamSide.RED) {
            redGoalkeeper = playerId;
        } else {
            blueGoalkeeper = playerId;
        }

        saveAndSynchronize(server);
        return true;
    }

    public static boolean isGoalkeeper(
            MinecraftServer server,
            UUID playerId
    ) {
        ensureLoaded(server);

        return playerId != null
                && (playerId.equals(redGoalkeeper)
                || playerId.equals(blueGoalkeeper));
    }

    public static UUID getReferee(
            MinecraftServer server
    ) {
        ensureLoaded(server);
        return referee;
    }

    public static boolean isReferee(
            MinecraftServer server,
            UUID playerId
    ) {
        ensureLoaded(server);

        return playerId != null
                && playerId.equals(referee);
    }

    /**
     * Asigna un árbitro único.
     *
     * Al recibir el rol:
     * - sale de cualquier equipo;
     * - deja de ser portero;
     * - conserva el rol después de reiniciar.
     */
    public static boolean setReferee(
            MinecraftServer server,
            UUID playerId
    ) {
        ensureLoaded(server);

        if (playerId == null) {
            return clearReferee(server);
        }

        if (playerId.equals(referee)) {
            return false;
        }

        RED_PLAYERS.remove(playerId);
        BLUE_PLAYERS.remove(playerId);

        if (playerId.equals(redGoalkeeper)) {
            redGoalkeeper = null;
        }

        if (playerId.equals(blueGoalkeeper)) {
            blueGoalkeeper = null;
        }

        referee = playerId;

        saveAndSynchronize(server);
        return true;
    }

    public static boolean clearReferee(
            MinecraftServer server
    ) {
        ensureLoaded(server);

        if (referee == null) {
            return false;
        }

        referee = null;

        saveAndSynchronize(server);
        return true;
    }

    public static SoccerMatch getActiveMatch(
            MinecraftServer server
    ) {
        ensureLoaded(server);
        return activeMatch;
    }

    public static boolean startMatch(
            MinecraftServer server,
            SoccerField field,
            int minutes
    ) {
        ensureLoaded(server);

        if (activeMatch != null) {
            return false;
        }

        activeMatch = new SoccerMatch(
                field.getId(),
                minutes * 60 * 20
        );

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName,
                redGoalkeeper,
                blueGoalkeeper,
                referee
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
        ensureLoaded(server);

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
        ensureLoaded(server);

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
        ensureLoaded(server);

        if (activeMatch == null) {
            return false;
        }

        SoccerField field = SoccerFieldManager.getField(
                server,
                activeMatch.getFieldId()
        );

        if (field != null) {
            activeMatch.removeOfficialBall(server, field);
        }

        activeMatch = null;
        sendInactiveState(server);
        return true;
    }

    public static void tick(
            MinecraftServer server
    ) {
        ensureLoaded(server);

        if (activeMatch == null) {
            return;
        }

        SoccerField field = SoccerFieldManager.getField(
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

        GoalkeeperManager.tick(
                server,
                activeMatch,
                field
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
        ensureLoaded(server);

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName,
                redGoalkeeper,
                blueGoalkeeper,
                referee
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
                createPayload(server, activeMatch, player)
        );
    }

    public static void synchronize(
            MinecraftServer server
    ) {
        ensureLoaded(server);

        if (activeMatch == null) {
            sendInactiveState(server);
            return;
        }

        for (ServerPlayer player
                : server.getPlayerList().getPlayers()) {

            PacketDistributor.sendToPlayer(
                    player,
                    createPayload(server, activeMatch, player)
            );
        }
    }

    private static MatchStatePayload createPayload(
            MinecraftServer server,
            SoccerMatch match,
            ServerPlayer player
    ) {
        SoccerTeamSide playerSide = getPlayerTeam(
                server,
                player.getUUID()
        );

        boolean goalkeeper =
                isGoalkeeper(server, player.getUUID());

        GoalkeeperManager.ClientStatus goalkeeperStatus =
                GoalkeeperManager.getClientStatus(
                        server,
                        match,
                        player
                );

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
                playerSide != null,
                playerSide == null ? "" : playerSide.name(),
                goalkeeper,
                goalkeeperStatus.inArea(),
                goalkeeperStatus.available(),
                goalkeeperStatus.holdingBall(),
                goalkeeperStatus.cooldownTicks()
        );
    }

    private static void sendInactiveState(
            MinecraftServer server
    ) {
        MatchStatePayload payload = MatchStatePayload.inactive();

        for (ServerPlayer player
                : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static Set<UUID> getMutableTeam(
            SoccerTeamSide side
    ) {
        return side == SoccerTeamSide.RED
                ? RED_PLAYERS
                : BLUE_PLAYERS;
    }

    private static void normalizeLoadedData() {
        /* Un jugador no puede pertenecer a ambos equipos. */
        BLUE_PLAYERS.removeAll(RED_PLAYERS);

        if (redGoalkeeper != null
                && !RED_PLAYERS.contains(redGoalkeeper)) {
            redGoalkeeper = null;
        }

        if (blueGoalkeeper != null
                && !BLUE_PLAYERS.contains(blueGoalkeeper)) {
            blueGoalkeeper = null;
        }

        /*
         * El árbitro nunca puede formar parte de un equipo
         * ni conservar un rol de portero.
         */
        if (referee != null) {
            RED_PLAYERS.remove(referee);
            BLUE_PLAYERS.remove(referee);

            if (referee.equals(redGoalkeeper)) {
                redGoalkeeper = null;
            }

            if (referee.equals(blueGoalkeeper)) {
                blueGoalkeeper = null;
            }
        }
    }

    private static void saveAndSynchronize(
            MinecraftServer server
    ) {
        save(server);

        SoccerTeamDisplayManager.synchronize(
                server,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redTeamName,
                blueTeamName,
                redGoalkeeper,
                blueGoalkeeper,
                referee
        );

        synchronize(server);
    }

    private static void save(
            MinecraftServer server
    ) {
        SoccerTeamStorage.save(
                server,
                redTeamName,
                blueTeamName,
                RED_PLAYERS,
                BLUE_PLAYERS,
                redGoalkeeper,
                blueGoalkeeper,
                referee
        );
    }

    private static String sanitizeTeamName(
            String requestedName
    ) {
        if (requestedName == null) {
            return "";
        }

        String cleaned = requestedName
                .replaceAll("§.", "")
                .trim()
                .replaceAll("\\s+", " ");

        return cleaned.length() > 24
                ? cleaned.substring(0, 24)
                : cleaned;
    }
}
