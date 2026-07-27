package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Capa de PlayerAnimator para slide_soccer.
 */
public final class SlideAnimationClient {

    public static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CraftersSoccer.MOD_ID,
                    "slide_layer"
            );

    public static final ResourceLocation ANIMATION_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CraftersSoccer.MOD_ID,
                    "slide_soccer"
            );

    private static final int LAYER_PRIORITY = 85;

    private SlideAnimationClient() {
    }

    public static void registerFactory() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY
                .registerFactory(
                        LAYER_ID,
                        LAYER_PRIORITY,
                        player -> new ModifierLayer<>()
                );
    }

    public static void play(
            AbstractClientPlayer player
    ) {
        ModifierLayer<IAnimation> layer =
                getLayer(player);

        if (layer == null) {
            return;
        }

        var animation =
                PlayerAnimationRegistry.getAnimation(
                        ANIMATION_ID
                );

        if (animation == null) {
            System.err.println(
                    "[crafterssoccer] No se encontró "
                            + ANIMATION_ID
            );
            return;
        }

        layer.setAnimation(
                animation.playAnimation()
        );
    }

    public static void stop(
            AbstractClientPlayer player
    ) {
        ModifierLayer<IAnimation> layer =
                getLayer(player);

        if (layer != null) {
            layer.setAnimation(null);
        }
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> getLayer(
            AbstractClientPlayer player
    ) {
        IAnimation animation =
                PlayerAnimationAccess
                        .getPlayerAssociatedData(player)
                        .get(LAYER_ID);

        if (animation instanceof ModifierLayer<?> layer) {
            return (ModifierLayer<IAnimation>) layer;
        }

        return null;
    }
}
