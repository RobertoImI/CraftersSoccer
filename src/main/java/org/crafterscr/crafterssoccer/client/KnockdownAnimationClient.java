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
 * Integración visual del derribo con PlayerAnimator.
 */
public final class KnockdownAnimationClient {

    public static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CraftersSoccer.MOD_ID,
                    "knockdown_layer"
            );

    public static final ResourceLocation ANIMATION_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CraftersSoccer.MOD_ID,
                    "soccer_knockdown"
            );

    private static final int LAYER_PRIORITY = 90;

    private static boolean missingAnimationReported;

    private KnockdownAnimationClient() {
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
            if (!missingAnimationReported) {
                missingAnimationReported = true;

                System.err.println(
                        "[crafterssoccer] No se encontró la animación "
                                + ANIMATION_ID
                                + ". Revisa que soccer_knockdown.json "
                                + "esté dentro de assets/crafterssoccer/"
                                + "player_animation/."
                );
            }

            return;
        }

        /*
         * En PlayerAnimator 2.0.1, el registro devuelve IPlayable.
         * playAnimation() crea la instancia reproducible correcta.
         */
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

        if (animation
                instanceof ModifierLayer<?> modifierLayer) {

            return (ModifierLayer<IAnimation>) modifierLayer;
        }

        return null;
    }
}
