package org.crafterscr.crafterssoccer.spectator;

import java.util.HashSet;
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
 * Un administrador asigna a un jugador como cámara.
 * Los usuarios que no pertenecen a un equipo pueden entrar o salir
 * pulsando la tecla B.
 */
public final class SpectatorBroadcastManager {

    private static UUID cameraOperator;

    private static final Set<UUID> VIEWERS =
            new HashSet<>();

    private static int synchronizationTicker;

    private SpectatorBroadcastManager() {
    }

    public static boolean setCameraOperator(
            MinecraftServer server,
            ServerPlayer operator
    ) {
        if (server == null || operator == null) {
            return false;
        }

        cameraOperator =
                operator.getUUID();

        /*
         * La cámara ahora se cambia únicamente en el cliente.
         * El cuerpo del espectador permanece donde estaba.
         */
        for (UUID viewerId : Set.copyOf(VIEWERS)) {
            ServerPlayer viewer =
                    server.getPlayerList().getPlayer(
                            viewerId
                    );

            if (viewer == null
                    || !isAllowedViewer(
                    server,
                    viewer
            )) {
                exitViewer(
                        server,
                        viewer,
                        false
                );
            }
        }

        synchronize(server);
        return true;
    }

    public static boolean clearCameraOperator(
            MinecraftServer server
    ) {
        if (cameraOperator == null) {
            return false;
        }

        for (UUID viewerId : Set.copyOf(VIEWERS)) {
            ServerPlayer viewer =
                    server.getPlayerList().getPlayer(
                            viewerId
                    );

            exitViewer(
                    server,
                    viewer,
                    false
            );
        }

        VIEWERS.clear();
        cameraOperator = null;
        synchronize(server);
        return true;
    }

    public static ServerPlayer getCameraOperator(
            MinecraftServer server
    ) {
        if (server == null
                || cameraOperator == null) {
            return null;
        }

        return server.getPlayerList().getPlayer(
                cameraOperator
        );
    }

    public static void toggleViewer(
            MinecraftServer server,
            ServerPlayer viewer
    ) {
        if (server == null || viewer == null) {
            return;
        }

        if (VIEWERS.contains(
                viewer.getUUID()
        )) {
            exitViewer(
                    server,
                    viewer,
                    true
            );
            return;
        }

        ServerPlayer operator =
                getCameraOperator(server);

        if (operator == null) {
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

        VIEWERS.add(
                viewer.getUUID()
        );

        /*
         * No usamos ServerPlayer#setCamera.
         * La vista TV se cambia solamente en el cliente.
         */
        viewer.displayClientMessage(
                Component.literal(
                        "§aCámara TV activada. "
                                + "§7Pulsa §fB §7para salir."
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
        ServerPlayer operator =
                getCameraOperator(server);

        if (cameraOperator != null
                && operator == null) {
            clearCameraOperator(server);
            return;
        }

        for (UUID viewerId : Set.copyOf(VIEWERS)) {
            ServerPlayer viewer =
                    server.getPlayerList().getPlayer(
                            viewerId
                    );

            if (viewer == null) {
                VIEWERS.remove(viewerId);
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

        }

        synchronizationTicker++;

        if (synchronizationTicker >= 20) {
            synchronizationTicker = 0;
            synchronize(server);
        }
    }

    public static void synchronizePlayer(
            MinecraftServer server,
            ServerPlayer player
    ) {
        ServerPlayer operator =
                getCameraOperator(server);

        boolean available =
                operator != null;

        boolean allowed =
                available
                        && isAllowedViewer(
                        server,
                        player
                );

        boolean watching =
                allowed
                        && VIEWERS.contains(
                        player.getUUID()
                );

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
                                : operator.getName()
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

        VIEWERS.remove(
                playerId
        );

        if (playerId.equals(
                cameraOperator
        )) {
            clearCameraOperator(server);
        }
    }

    private static boolean isAllowedViewer(
            MinecraftServer server,
            ServerPlayer viewer
    ) {
        if (viewer == null
                || cameraOperator == null
                || viewer.getUUID().equals(
                cameraOperator
        )) {
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

        VIEWERS.remove(
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
}
