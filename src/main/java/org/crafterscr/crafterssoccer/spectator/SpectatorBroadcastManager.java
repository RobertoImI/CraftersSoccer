package org.crafterscr.crafterssoccer.spectator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.match.SoccerMatchManager;
import org.crafterscr.crafterssoccer.network.BroadcastCameraStatePayload;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Transmisión tipo televisión para espectadores.
 *
 * Soporta hasta tres operadores de cámara simultáneos.
 * Los espectadores usan B para entrar, cambiar a la siguiente cámara
 * disponible y salir después de la última.
 */
public final class SpectatorBroadcastManager {

    public static final int MAX_CAMERAS = 3;

    /**
     * Índices internos 0..2 = cámaras visibles 1..3.
     */
    private static final UUID[] CAMERA_OPERATORS =
            new UUID[MAX_CAMERAS];

    /**
     * Cámara que está viendo cada espectador (índice interno 0..2).
     */
    private static final Map<UUID, Integer> VIEWER_CAMERAS =
            new HashMap<>();

    private static int synchronizationTicker;

    private SpectatorBroadcastManager() {
    }

    /**
     * Compatibilidad con el sistema anterior: asigna Cámara 1.
     */
    public static boolean setCameraOperator(
            MinecraftServer server,
            ServerPlayer operator
    ) {
        return setCameraOperator(
                server,
                1,
                operator
        );
    }

    public static boolean setCameraOperator(
            MinecraftServer server,
            int cameraNumber,
            ServerPlayer operator
    ) {
        if (server == null
                || operator == null
                || !isValidCameraNumber(
                cameraNumber
        )) {
            return false;
        }

        int slot =
                cameraNumber - 1;

        /*
         * Un mismo jugador no puede ocupar dos cámaras.
         */
        int existingSlot =
                findCameraSlot(
                        operator.getUUID()
                );

        if (existingSlot >= 0
                && existingSlot != slot) {
            return false;
        }

        CAMERA_OPERATORS[slot] =
                operator.getUUID();

        /*
         * Un operador de cámara nunca debe continuar como espectador.
         */
        VIEWER_CAMERAS.remove(
                operator.getUUID()
        );

        normalizeViewers(
                server,
                false
        );

        synchronize(server);
        return true;
    }

    /**
     * Compatibilidad con el comando anterior: limpia todas las cámaras.
     */
    public static boolean clearCameraOperator(
            MinecraftServer server
    ) {
        boolean hadCamera = false;

        for (int slot = 0;
             slot < MAX_CAMERAS;
             slot++) {

            if (CAMERA_OPERATORS[slot] != null) {
                hadCamera = true;
                CAMERA_OPERATORS[slot] = null;
            }
        }

        if (!hadCamera) {
            return false;
        }

        for (UUID viewerId
                : Set.copyOf(
                VIEWER_CAMERAS.keySet()
        )) {

            ServerPlayer viewer =
                    server.getPlayerList()
                            .getPlayer(
                                    viewerId
                            );

            exitViewer(
                    server,
                    viewer,
                    false
            );
        }

        VIEWER_CAMERAS.clear();
        synchronize(server);
        return true;
    }

    public static boolean clearCameraOperator(
            MinecraftServer server,
            int cameraNumber
    ) {
        if (server == null
                || !isValidCameraNumber(
                cameraNumber
        )) {
            return false;
        }

        int slot =
                cameraNumber - 1;

        if (CAMERA_OPERATORS[slot] == null) {
            return false;
        }

        CAMERA_OPERATORS[slot] = null;

        normalizeViewers(
                server,
                true
        );

        synchronize(server);
        return true;
    }

    /**
     * Compatibilidad: devuelve la primera cámara disponible.
     */
    public static ServerPlayer getCameraOperator(
            MinecraftServer server
    ) {
        int slot =
                firstAvailableSlot(
                        server
                );

        return slot < 0
                ? null
                : getCameraOperatorBySlot(
                server,
                slot
        );
    }

    public static ServerPlayer getCameraOperator(
            MinecraftServer server,
            int cameraNumber
    ) {
        if (!isValidCameraNumber(
                cameraNumber
        )) {
            return null;
        }

        return getCameraOperatorBySlot(
                server,
                cameraNumber - 1
        );
    }

    public static int getConfiguredCameraCount(
            MinecraftServer server
    ) {
        int count = 0;

        for (int slot = 0;
             slot < MAX_CAMERAS;
             slot++) {

            if (getCameraOperatorBySlot(
                    server,
                    slot
            ) != null) {
                count++;
            }
        }

        return count;
    }

    /**
     * B funciona como una secuencia:
     *
     * - Si no mira TV -> entra a la primera cámara.
     * - Si ya mira TV -> pasa a la siguiente cámara.
     * - Si estaba en la última -> sale de TV.
     *
     * Con una sola cámara conserva el comportamiento anterior:
     * entrar -> salir.
     */
    public static void toggleViewer(
            MinecraftServer server,
            ServerPlayer viewer
    ) {
        if (server == null
                || viewer == null) {
            return;
        }

        Integer currentSlot =
                VIEWER_CAMERAS.get(
                        viewer.getUUID()
                );

        if (currentSlot != null) {
            if (!isAllowedViewer(
                    server,
                    viewer
            )) {
                exitViewer(
                        server,
                        viewer,
                        true
                );
                return;
            }

            int nextSlot =
                    nextAvailableSlotAfter(
                            server,
                            currentSlot
                    );

            if (nextSlot < 0) {
                exitViewer(
                        server,
                        viewer,
                        true
                );
                return;
            }

            VIEWER_CAMERAS.put(
                    viewer.getUUID(),
                    nextSlot
            );

            ServerPlayer operator =
                    getCameraOperatorBySlot(
                            server,
                            nextSlot
                    );

            viewer.displayClientMessage(
                    Component.literal(
                            "§bCámara TV "
                                    + (nextSlot + 1)
                                    + "§7: §f"
                                    + (operator == null
                                    ? ""
                                    : operator.getName()
                                    .getString())
                                    + " §8(§fB§8: siguiente)"
                    ),
                    false
            );

            synchronizePlayer(
                    server,
                    viewer
            );
            return;
        }

        int firstSlot =
                firstAvailableSlot(
                        server
                );

        if (firstSlot < 0) {
            viewer.displayClientMessage(
                    Component.literal(
                            "§cNo hay una cámara TV asignada."
                    ),
                    false
            );

            synchronizePlayer(
                    server,
                    viewer
            );
            return;
        }

        if (!isAllowedViewer(
                server,
                viewer
        )) {
            viewer.displayClientMessage(
                    Component.literal(
                            "§cSolo los espectadores que no pertenecen "
                                    + "a un equipo pueden usar la cámara TV."
                    ),
                    false
            );

            synchronizePlayer(
                    server,
                    viewer
            );
            return;
        }

        VIEWER_CAMERAS.put(
                viewer.getUUID(),
                firstSlot
        );

        ServerPlayer operator =
                getCameraOperatorBySlot(
                        server,
                        firstSlot
                );

        int cameraCount =
                getConfiguredCameraCount(
                        server
                );

        viewer.displayClientMessage(
                Component.literal(
                        "§aCámara TV activada: §bCámara "
                                + (firstSlot + 1)
                                + " §7- §f"
                                + (operator == null
                                ? ""
                                : operator.getName()
                                .getString())
                                + (cameraCount > 1
                                ? " §8(§fB§8: siguiente)"
                                : " §8(§fB§8: salir)")
                ),
                false
        );

        synchronizePlayer(
                server,
                viewer
        );
    }

    public static void tick(
            MinecraftServer server
    ) {
        boolean cameraChanged = false;

        /*
         * Si un operador se desconectó sin pasar por el evento normal,
         * su slot se libera sin afectar las demás cámaras.
         */
        for (int slot = 0;
             slot < MAX_CAMERAS;
             slot++) {

            UUID operatorId =
                    CAMERA_OPERATORS[slot];

            if (operatorId != null
                    && server.getPlayerList()
                    .getPlayer(
                            operatorId
                    ) == null) {

                CAMERA_OPERATORS[slot] = null;
                cameraChanged = true;
            }
        }

        if (cameraChanged) {
            normalizeViewers(
                    server,
                    true
            );
        } else {
            normalizeViewers(
                    server,
                    false
            );
        }

        synchronizationTicker++;

        if (cameraChanged
                || synchronizationTicker >= 20) {

            synchronizationTicker = 0;
            synchronize(server);
        }
    }

    public static void synchronizePlayer(
            MinecraftServer server,
            ServerPlayer player
    ) {
        int firstSlot =
                firstAvailableSlot(
                        server
                );

        boolean available =
                firstSlot >= 0;

        boolean allowed =
                available
                        && isAllowedViewer(
                        server,
                        player
                );

        Integer selectedSlot =
                VIEWER_CAMERAS.get(
                        player.getUUID()
                );

        ServerPlayer operator = null;

        if (selectedSlot != null) {
            operator =
                    getCameraOperatorBySlot(
                            server,
                            selectedSlot
                    );
        }

        boolean watching =
                allowed
                        && selectedSlot != null
                        && operator != null;

        if (!watching) {
            selectedSlot = firstSlot;
            operator =
                    firstSlot < 0
                            ? null
                            : getCameraOperatorBySlot(
                            server,
                            firstSlot
                    );
        }

        PacketDistributor.sendToPlayer(
                player,
                new BroadcastCameraStatePayload(
                        available,
                        allowed,
                        watching,
                        operator == null
                                ? -1
                                : operator.getId(),
                        operator == null
                                ? ""
                                : "Cámara "
                                + (selectedSlot + 1)
                                + " - "
                                + operator.getName()
                                .getString()
                )
        );
    }

    public static void synchronize(
            MinecraftServer server
    ) {
        for (ServerPlayer player
                : server.getPlayerList().getPlayers()) {

            synchronizePlayer(
                    server,
                    player
            );
        }
    }

    public static void handleLogout(
            MinecraftServer server,
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        UUID playerId =
                player.getUUID();

        VIEWER_CAMERAS.remove(
                playerId
        );

        boolean cameraRemoved = false;

        for (int slot = 0;
             slot < MAX_CAMERAS;
             slot++) {

            if (playerId.equals(
                    CAMERA_OPERATORS[slot]
            )) {
                CAMERA_OPERATORS[slot] = null;
                cameraRemoved = true;
            }
        }

        if (cameraRemoved) {
            normalizeViewers(
                    server,
                    true
            );

            synchronize(server);
        }
    }

    private static void normalizeViewers(
            MinecraftServer server,
            boolean notifyCameraChange
    ) {
        for (UUID viewerId
                : Set.copyOf(
                VIEWER_CAMERAS.keySet()
        )) {

            ServerPlayer viewer =
                    server.getPlayerList()
                            .getPlayer(
                                    viewerId
                            );

            if (viewer == null) {
                VIEWER_CAMERAS.remove(
                        viewerId
                );
                continue;
            }

            if (!isAllowedViewer(
                    server,
                    viewer
            )) {
                exitViewer(
                        server,
                        viewer,
                        true
                );
                continue;
            }

            Integer slot =
                    VIEWER_CAMERAS.get(
                            viewerId
                    );

            if (slot == null
                    || getCameraOperatorBySlot(
                    server,
                    slot
            ) != null) {
                continue;
            }

            int replacement =
                    firstAvailableSlot(
                            server
                    );

            if (replacement < 0) {
                exitViewer(
                        server,
                        viewer,
                        true
                );
                continue;
            }

            VIEWER_CAMERAS.put(
                    viewerId,
                    replacement
            );

            if (notifyCameraChange) {
                viewer.displayClientMessage(
                        Component.literal(
                                "§eLa cámara anterior dejó de estar disponible. "
                                        + "§7Cambiando a §bCámara "
                                        + (replacement + 1)
                                        + "§7."
                        ),
                        false
                );
            }

            synchronizePlayer(
                    server,
                    viewer
            );
        }
    }

    private static boolean isAllowedViewer(
            MinecraftServer server,
            ServerPlayer viewer
    ) {
        if (viewer == null
                || findCameraSlot(
                viewer.getUUID()
        ) >= 0) {
            return false;
        }

        /*
         * Jugador = cualquier integrante del equipo rojo o azul.
         * El árbitro tampoco entra a la transmisión mientras arbitra.
         */
        return SoccerMatchManager.getPlayerTeam(
                server,
                viewer.getUUID()
        ) == null
                && !SoccerMatchManager.isReferee(
                server,
                viewer.getUUID()
        );
    }

    private static void exitViewer(
            MinecraftServer server,
            ServerPlayer viewer,
            boolean showMessage
    ) {
        if (viewer == null) {
            return;
        }

        VIEWER_CAMERAS.remove(
                viewer.getUUID()
        );

        if (showMessage) {
            viewer.displayClientMessage(
                    Component.literal(
                            "§eCámara TV desactivada."
                    ),
                    false
            );
        }

        synchronizePlayer(
                server,
                viewer
        );
    }

    private static int firstAvailableSlot(
            MinecraftServer server
    ) {
        for (int slot = 0;
             slot < MAX_CAMERAS;
             slot++) {

            if (getCameraOperatorBySlot(
                    server,
                    slot
            ) != null) {
                return slot;
            }
        }

        return -1;
    }

    private static int nextAvailableSlotAfter(
            MinecraftServer server,
            int currentSlot
    ) {
        for (int slot = currentSlot + 1;
             slot < MAX_CAMERAS;
             slot++) {

            if (getCameraOperatorBySlot(
                    server,
                    slot
            ) != null) {
                return slot;
            }
        }

        return -1;
    }

    private static ServerPlayer getCameraOperatorBySlot(
            MinecraftServer server,
            int slot
    ) {
        if (server == null
                || slot < 0
                || slot >= MAX_CAMERAS) {
            return null;
        }

        UUID operatorId =
                CAMERA_OPERATORS[slot];

        if (operatorId == null) {
            return null;
        }

        return server.getPlayerList()
                .getPlayer(
                        operatorId
                );
    }

    private static int findCameraSlot(
            UUID operatorId
    ) {
        if (operatorId == null) {
            return -1;
        }

        for (int slot = 0;
             slot < MAX_CAMERAS;
             slot++) {

            if (operatorId.equals(
                    CAMERA_OPERATORS[slot]
            )) {
                return slot;
            }
        }

        return -1;
    }

    private static boolean isValidCameraNumber(
            int cameraNumber
    ) {
        return cameraNumber >= 1
                && cameraNumber <= MAX_CAMERAS;
    }
}
