package org.crafterscr.crafterssoccer.referee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.field.SoccerField;
import org.crafterscr.crafterssoccer.field.SoccerFieldManager;
import org.crafterscr.crafterssoccer.match.SoccerMatch;
import org.crafterscr.crafterssoccer.match.SoccerMatchManager;
import org.crafterscr.crafterssoccer.match.SoccerTeamSide;
import org.crafterscr.crafterssoccer.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Tarjetas y expulsiones temporales.
 */
public final class RefereeCardManager {

    private static final double PENALTY_MAX_DISTANCE = 3.75D;

    private static final Map<UUID, Integer> YELLOW_CARDS =
            new HashMap<>();

    private static final Map<UUID, ExpulsionData> EXPULSIONS =
            new HashMap<>();

    private RefereeCardManager() {
    }

    public static boolean issueYellow(
            MinecraftServer server,
            ServerPlayer referee,
            ServerPlayer target
    ) {
        if (!canIssueCard(server, referee, target)) {
            return false;
        }

        if (isExpelled(target.getUUID())) {
            referee.displayClientMessage(
                    Component.literal(
                            "§cEse jugador ya está expulsado."
                    ),
                    true
            );
            return false;
        }

        int newCount =
                getYellowCards(target.getUUID()) + 1;

        YELLOW_CARDS.put(
                target.getUUID(),
                newCount
        );

        if (newCount >= 2) {
            broadcast(
                    server,
                    Component.literal(
                            "§6⚠ §f"
                                    + target.getGameProfile().getName()
                                    + " §erecibió una segunda amarilla. "
                                    + "§c§l¡TARJETA ROJA!"
                    )
            );

            beginTemporaryExpulsion(
                    server,
                    target
            );

            return true;
        }

        broadcast(
                server,
                Component.literal(
                        "§e▮ §f"
                                + target.getGameProfile().getName()
                                + " §erecibió una tarjeta amarilla. "
                                + "§7(" + newCount + "/2)"
                )
        );

        target.displayClientMessage(
                Component.literal(
                        "§eHas recibido una tarjeta amarilla."
                ),
                true
        );

        return true;
    }

    public static boolean issueRed(
            MinecraftServer server,
            ServerPlayer referee,
            ServerPlayer target
    ) {
        if (!canIssueCard(server, referee, target)) {
            return false;
        }

        if (isExpelled(target.getUUID())) {
            referee.displayClientMessage(
                    Component.literal(
                            "§cEse jugador ya está expulsado."
                    ),
                    true
            );
            return false;
        }

        broadcast(
                server,
                Component.literal(
                        "§c▮ §f"
                                + target.getGameProfile().getName()
                                + " §crecibió una tarjeta roja directa."
                )
        );

        beginTemporaryExpulsion(
                server,
                target
        );

        return true;
    }

    private static void beginTemporaryExpulsion(
            MinecraftServer server,
            ServerPlayer target
    ) {
        SoccerTeamSide side =
                SoccerMatchManager.getPlayerTeam(
                        server,
                        target.getUUID()
                );

        SoccerMatch match =
                SoccerMatchManager.getActiveMatch(
                        server
                );

        String fieldId =
                match == null
                        ? ""
                        : match.getFieldId();

        long endGameTime =
                server.overworld().getGameTime()
                        + RefereePenaltyConfig.getRedCardTicks(
                        server
                );

        ExpulsionData data =
                new ExpulsionData(
                        side,
                        fieldId,
                        endGameTime
                );

        EXPULSIONS.put(
                target.getUUID(),
                data
        );

        teleportToPenaltyIfPossible(
                server,
                target,
                data
        );

        target.displayClientMessage(
                Component.literal(
                        "§cHas sido expulsado temporalmente."
                ),
                true
        );
    }

    public static void tick(
            MinecraftServer server
    ) {
        if (EXPULSIONS.isEmpty()) {
            return;
        }

        long gameTime =
                server.overworld().getGameTime();

        List<UUID> finished =
                new ArrayList<>();

        for (Map.Entry<UUID, ExpulsionData> entry
                : EXPULSIONS.entrySet()) {

            UUID playerId =
                    entry.getKey();

            ExpulsionData data =
                    entry.getValue();

            if (gameTime >= data.endGameTime()) {
                finished.add(playerId);
                continue;
            }

            ServerPlayer player =
                    server.getPlayerList().getPlayer(
                            playerId
                    );

            if (player == null) {
                continue;
            }

            keepInsidePenalty(
                    server,
                    player,
                    data
            );

            int remainingTicks =
                    (int) Math.max(
                            0L,
                            data.endGameTime() - gameTime
                    );

            player.connection.send(
                    new ClientboundSetActionBarTextPacket(
                            Component.literal(
                                    "§c§l🟥 EXPULSADO §7— Regresas en §f"
                                            + formatTicks(
                                            remainingTicks
                                    )
                            )
                    )
            );
        }

        for (UUID playerId : finished) {
            finishExpulsion(
                    server,
                    playerId,
                    true
            );
        }
    }

    public static void handlePlayerLogin(
            MinecraftServer server,
            ServerPlayer player
    ) {
        ExpulsionData data =
                EXPULSIONS.get(
                        player.getUUID()
                );

        if (data == null) {
            return;
        }

        long gameTime =
                server.overworld().getGameTime();

        if (gameTime >= data.endGameTime()) {
            finishExpulsion(
                    server,
                    player.getUUID(),
                    true
            );
            return;
        }

        teleportToPenaltyIfPossible(
                server,
                player,
                data
        );
    }

    public static boolean pardon(
            MinecraftServer server,
            UUID playerId
    ) {
        if (!EXPULSIONS.containsKey(playerId)) {
            return false;
        }

        finishExpulsion(
                server,
                playerId,
                true
        );

        return true;
    }

    public static void cancelAllExpulsions(
            MinecraftServer server,
            boolean returnPlayers
    ) {
        Set<UUID> ids =
                Set.copyOf(
                        EXPULSIONS.keySet()
                );

        for (UUID playerId : ids) {
            finishExpulsion(
                    server,
                    playerId,
                    returnPlayers
            );
        }
    }

    private static void finishExpulsion(
            MinecraftServer server,
            UUID playerId,
            boolean returnPlayer
    ) {
        ExpulsionData data =
                EXPULSIONS.remove(
                        playerId
                );

        if (data == null) {
            return;
        }

        YELLOW_CARDS.remove(
                playerId
        );

        ServerPlayer player =
                server.getPlayerList().getPlayer(
                        playerId
                );

        if (player == null) {
            return;
        }

        if (returnPlayer) {
            teleportToTeamSpawn(
                    server,
                    player,
                    data
            );
        }

        player.displayClientMessage(
                Component.literal(
                        "§aTu expulsión terminó. Regresas al juego."
                ),
                true
        );
    }

    private static void keepInsidePenalty(
            MinecraftServer server,
            ServerPlayer player,
            ExpulsionData data
    ) {
        SoccerField field =
                getExpulsionField(
                        server,
                        data
                );

        if (field == null
                || data.side() == null) {
            return;
        }

        BlockPos penalty =
                field.getPenaltyPosition(
                        data.side()
                );

        if (penalty == null) {
            return;
        }

        ServerLevel level =
                getFieldLevel(
                        server,
                        field
                );

        if (level == null) {
            return;
        }

        Vec3 center =
                Vec3.atBottomCenterOf(
                        penalty
                );

        boolean wrongDimension =
                player.serverLevel() != level;

        boolean tooFar =
                player.position().distanceToSqr(
                        center
                ) > PENALTY_MAX_DISTANCE
                        * PENALTY_MAX_DISTANCE;

        if (wrongDimension || tooFar) {
            teleport(
                    player,
                    level,
                    penalty
            );
        }
    }

    private static void teleportToPenaltyIfPossible(
            MinecraftServer server,
            ServerPlayer player,
            ExpulsionData data
    ) {
        SoccerField field =
                getExpulsionField(
                        server,
                        data
                );

        if (field == null
                || data.side() == null) {
            return;
        }

        BlockPos penalty =
                field.getPenaltyPosition(
                        data.side()
                );

        ServerLevel level =
                getFieldLevel(
                        server,
                        field
                );

        if (penalty == null || level == null) {
            return;
        }

        teleport(
                player,
                level,
                penalty
        );
    }

    private static void teleportToTeamSpawn(
            MinecraftServer server,
            ServerPlayer player,
            ExpulsionData data
    ) {
        SoccerField field =
                getExpulsionField(
                        server,
                        data
                );

        if (field == null
                || data.side() == null) {
            return;
        }

        List<BlockPos> spawns =
                data.side() == SoccerTeamSide.RED
                        ? field.getRedSpawns()
                        : field.getBlueSpawns();

        if (spawns.isEmpty()) {
            return;
        }

        ServerLevel level =
                getFieldLevel(
                        server,
                        field
                );

        if (level == null) {
            return;
        }

        int index =
                Math.floorMod(
                        player.getUUID().hashCode(),
                        spawns.size()
                );

        teleport(
                player,
                level,
                spawns.get(index)
        );
    }

    private static SoccerField getExpulsionField(
            MinecraftServer server,
            ExpulsionData data
    ) {
        if (data.fieldId() == null
                || data.fieldId().isBlank()) {
            return null;
        }

        return SoccerFieldManager.getField(
                server,
                data.fieldId()
        );
    }

    private static ServerLevel getFieldLevel(
            MinecraftServer server,
            SoccerField field
    ) {
        try {
            ResourceLocation location =
                    ResourceLocation.parse(
                            field.getDimensionId()
                    );

            ResourceKey<Level> key =
                    ResourceKey.create(
                            Registries.DIMENSION,
                            location
                    );

            return server.getLevel(key);

        } catch (Exception ignored) {
            return null;
        }
    }

    private static void teleport(
            ServerPlayer player,
            ServerLevel level,
            BlockPos position
    ) {
        player.teleportTo(
                level,
                position.getX() + 0.5D,
                position.getY() + 0.10D,
                position.getZ() + 0.5D,
                Set.<RelativeMovement>of(),
                player.getYRot(),
                player.getXRot()
        );

        player.setDeltaMovement(
                Vec3.ZERO
        );
    }

    private static boolean canIssueCard(
            MinecraftServer server,
            ServerPlayer referee,
            ServerPlayer target
    ) {
        if (server == null
                || referee == null
                || target == null) {
            return false;
        }

        if (!SoccerMatchManager.isReferee(
                server,
                referee.getUUID()
        )) {
            referee.displayClientMessage(
                    Component.literal(
                            "§cSolo el árbitro puede usar esta tarjeta."
                    ),
                    true
            );
            return false;
        }

        if (referee.getUUID().equals(target.getUUID())) {
            referee.displayClientMessage(
                    Component.literal(
                            "§cNo puedes mostrarte una tarjeta a ti mismo."
                    ),
                    true
            );
            return false;
        }

        return true;
    }

    public static int getYellowCards(
            UUID playerId
    ) {
        return YELLOW_CARDS.getOrDefault(
                playerId,
                0
        );
    }

    public static boolean isExpelled(
            UUID playerId
    ) {
        return EXPULSIONS.containsKey(
                playerId
        );
    }

    public static int getRemainingTicks(
            MinecraftServer server,
            UUID playerId
    ) {
        ExpulsionData data =
                EXPULSIONS.get(playerId);

        if (data == null) {
            return 0;
        }

        return (int) Math.max(
                0L,
                data.endGameTime()
                        - server.overworld().getGameTime()
        );
    }

    public static boolean clearPlayer(
            MinecraftServer server,
            UUID playerId
    ) {
        boolean changed =
                YELLOW_CARDS.remove(playerId) != null;

        if (EXPULSIONS.containsKey(playerId)) {
            finishExpulsion(
                    server,
                    playerId,
                    true
            );
            changed = true;
        }

        return changed;
    }

    public static int clearAll(
            MinecraftServer server
    ) {
        int affected =
                (int) java.util.stream.Stream
                        .concat(
                                YELLOW_CARDS.keySet().stream(),
                                EXPULSIONS.keySet().stream()
                        )
                        .distinct()
                        .count();

        cancelAllExpulsions(
                server,
                true
        );

        YELLOW_CARDS.clear();

        return affected;
    }

    public static void ensureRefereeCards(
            MinecraftServer server,
            ServerPlayer player
    ) {
        if (server == null
                || player == null
                || !SoccerMatchManager.isReferee(
                server,
                player.getUUID()
        )) {
            return;
        }

        giveIfMissing(
                player,
                ModItems.YELLOW_CARD.get()
        );

        giveIfMissing(
                player,
                ModItems.RED_CARD.get()
        );
    }

    public static void removeRefereeCards(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        removeItem(
                player,
                ModItems.YELLOW_CARD.get()
        );

        removeItem(
                player,
                ModItems.RED_CARD.get()
        );
    }

    private static void giveIfMissing(
            ServerPlayer player,
            Item item
    ) {
        if (player.getInventory().contains(
                new ItemStack(item)
        )) {
            return;
        }

        ItemStack stack =
                new ItemStack(item);

        if (!player.getInventory().add(stack)) {
            player.drop(
                    stack,
                    false
            );
        }
    }

    private static void removeItem(
            ServerPlayer player,
            Item item
    ) {
        for (int slot = 0;
             slot < player.getInventory().getContainerSize();
             slot++) {

            ItemStack stack =
                    player.getInventory().getItem(slot);

            if (stack.is(item)) {
                player.getInventory().setItem(
                        slot,
                        ItemStack.EMPTY
                );
            }
        }
    }

    private static void broadcast(
            MinecraftServer server,
            Component message
    ) {
        server.getPlayerList().broadcastSystemMessage(
                message,
                false
        );
    }

    private static String formatTicks(
            int ticks
    ) {
        int totalSeconds =
                Math.max(0, ticks / 20);

        int minutes =
                totalSeconds / 60;

        int seconds =
                totalSeconds % 60;

        return String.format(
                java.util.Locale.ROOT,
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    public record CardStatus(
            int yellowCards,
            boolean expelled,
            int remainingTicks
    ) {
    }

    public static CardStatus getStatus(
            MinecraftServer server,
            UUID playerId
    ) {
        return new CardStatus(
                getYellowCards(playerId),
                isExpelled(playerId),
                getRemainingTicks(
                        server,
                        playerId
                )
        );
    }

    private record ExpulsionData(
            SoccerTeamSide side,
            String fieldId,
            long endGameTime
    ) {
    }
}
