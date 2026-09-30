package org.crafterscr.crafterssoccer.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Estado visual de los barridos sincronizados.
 */
public final class ClientSlideState {

    public static final int COOLDOWN_TICKS = 100;

    /**
     * Duración visual alineada con el barrido autoritativo del servidor.
     */
    public static final int SLIDE_VISUAL_TICKS = 20;

    /**
     * La inclinación de cámara termina antes que la animación completa.
     */
    public static final int FIRST_PERSON_CAMERA_TICKS = 12;

    private static final Set<Integer> SLIDING =
            new HashSet<>();

    private static final Map<Integer, AbstractClientPlayer>
            ANIMATED =
            new HashMap<>();

    private static int localCooldownTicks;

    /*
     * Tiempo visual del barrido local. Se utiliza para animar
     * la inclinación de cámara en primera persona.
     */
    private static int localSlideVisualTicks;

    private ClientSlideState() {
    }

    public static void apply(
            int entityId,
            boolean sliding
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (sliding) {
            SLIDING.add(entityId);

            if (minecraft.player != null
                    && minecraft.player.getId() == entityId) {
                localSlideVisualTicks = 0;
            }
            tryStartAnimation(
                    minecraft,
                    entityId
            );
        } else {
            SLIDING.remove(entityId);

            if (minecraft.player != null
                    && minecraft.player.getId() == entityId) {
                localSlideVisualTicks = 0;
            }
            stopAnimation(
                    minecraft,
                    entityId
            );
        }
    }

    public static boolean isSliding(
            int entityId
    ) {
        return SLIDING.contains(entityId);
    }

    public static boolean isLocalPlayerSliding() {
        Minecraft minecraft =
                Minecraft.getInstance();

        return minecraft.player != null
                && isSliding(
                minecraft.player.getId()
        );
    }

    public static void startLocalCooldown() {
        localCooldownTicks =
                COOLDOWN_TICKS;
    }

    public static boolean hasLocalCooldown() {
        return localCooldownTicks > 0;
    }

    public static void clientTick(
            Minecraft minecraft
    ) {
        if (localCooldownTicks > 0) {
            localCooldownTicks--;
        }

        if (isLocalPlayerSliding()) {
            localSlideVisualTicks =
                    Math.min(
                            localSlideVisualTicks + 1,
                            SLIDE_VISUAL_TICKS
                    );
        }

        if (minecraft.level == null) {
            reset();
            return;
        }

        for (int entityId : SLIDING) {
            tryStartAnimation(
                    minecraft,
                    entityId
            );
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

        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        if (ANIMATED.get(entityId) == player) {
            return;
        }

        AbstractClientPlayer previous =
                ANIMATED.put(
                        entityId,
                        player
                );

        if (previous != null) {
            SlideAnimationClient.stop(previous);
        }

        SlideAnimationClient.play(player);
    }

    private static void stopAnimation(
            Minecraft minecraft,
            int entityId
    ) {
        AbstractClientPlayer player =
                ANIMATED.remove(entityId);

        if (player != null) {
            SlideAnimationClient.stop(player);
            return;
        }

        if (minecraft.level != null
                && minecraft.level.getEntity(entityId)
                instanceof AbstractClientPlayer found) {
            SlideAnimationClient.stop(found);
        }
    }

    public static float getLocalSlideProgress() {
        return Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        localSlideVisualTicks
                                / (float) SLIDE_VISUAL_TICKS
                )
        );
    }

    public static float getLocalCameraProgress() {
        return Math.max(
                0.0F,
                Math.min(
                        1.0F,
                        localSlideVisualTicks
                                / (float) FIRST_PERSON_CAMERA_TICKS
                )
        );
    }

    public static boolean shouldKeepFirstPersonCameraLow() {
        return isLocalPlayerSliding()
                && localSlideVisualTicks
                < FIRST_PERSON_CAMERA_TICKS;
    }

    public static void reset() {
        for (AbstractClientPlayer player
                : ANIMATED.values()) {
            SlideAnimationClient.stop(player);
        }

        SLIDING.clear();
        ANIMATED.clear();
        localCooldownTicks = 0;
        localSlideVisualTicks = 0;
    }
}
