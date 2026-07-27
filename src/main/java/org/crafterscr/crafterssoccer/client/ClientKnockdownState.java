package org.crafterscr.crafterssoccer.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Estado visual sincronizado de jugadores derribados.
 */
public final class ClientKnockdownState {

    private static final Set<Integer> KNOCKED_DOWN =
            new HashSet<>();

    private static final Map<Integer, AbstractClientPlayer>
            ANIMATED_PLAYERS =
            new HashMap<>();

    private ClientKnockdownState() {
    }

    public static void apply(
            int entityId,
            boolean knockedDown
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (knockedDown) {
            KNOCKED_DOWN.add(
                    entityId
            );

            tryStartAnimation(
                    minecraft,
                    entityId
            );

        } else {
            KNOCKED_DOWN.remove(
                    entityId
            );

            stopAnimation(
                    minecraft,
                    entityId
            );

            if (minecraft.player != null
                    && minecraft.player.getId()
                    == entityId) {

                ClientRecoveryState.reset();
            }
        }
    }

    public static boolean isKnockedDown(
            int entityId
    ) {
        return KNOCKED_DOWN.contains(
                entityId
        );
    }

    public static boolean isLocalPlayerDown() {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.player != null
                && isKnockedDown(
                minecraft.player.getId()
        );
    }

    public static void clientTick(
            Minecraft minecraft
    ) {
        if (minecraft.level == null) {
            reset();
            return;
        }

        for (int entityId : KNOCKED_DOWN) {
            tryStartAnimation(
                    minecraft,
                    entityId
            );
        }

        for (int entityId
                : Set.copyOf(
                ANIMATED_PLAYERS.keySet()
        )) {

            if (!KNOCKED_DOWN.contains(
                    entityId
            )) {
                stopAnimation(
                        minecraft,
                        entityId
                );
            }
        }
    }

    private static void tryStartAnimation(
            Minecraft minecraft,
            int entityId
    ) {
        if (minecraft.level == null) {
            return;
        }

        Entity entity =
                minecraft.level.getEntity(
                        entityId
                );

        if (!(entity
                instanceof AbstractClientPlayer player)) {
            return;
        }

        AbstractClientPlayer previous =
                ANIMATED_PLAYERS.get(
                        entityId
                );

        /*
         * La misma instancia ya tiene la animación.
         * No debe reiniciarse cada tick.
         */
        if (previous == player) {
            return;
        }

        if (previous != null) {
            KnockdownAnimationClient.stop(
                    previous
            );
        }

        KnockdownAnimationClient.play(
                player
        );

        ANIMATED_PLAYERS.put(
                entityId,
                player
        );
    }

    private static void stopAnimation(
            Minecraft minecraft,
            int entityId
    ) {
        AbstractClientPlayer animated =
                ANIMATED_PLAYERS.remove(
                        entityId
                );

        if (animated != null) {
            KnockdownAnimationClient.stop(
                    animated
            );
            return;
        }

        if (minecraft.level == null) {
            return;
        }

        Entity entity =
                minecraft.level.getEntity(
                        entityId
                );

        if (entity
                instanceof AbstractClientPlayer player) {

            KnockdownAnimationClient.stop(
                    player
            );
        }
    }

    public static void reset() {
        for (AbstractClientPlayer player
                : ANIMATED_PLAYERS.values()) {

            KnockdownAnimationClient.stop(
                    player
            );
        }

        ANIMATED_PLAYERS.clear();
        KNOCKED_DOWN.clear();

        ClientRecoveryState.reset();
    }
}
