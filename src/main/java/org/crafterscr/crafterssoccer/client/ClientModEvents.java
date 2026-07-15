package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;
import org.crafterscr.crafterssoccer.registry.ModEntities;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Eventos exclusivos del cliente.
 */
public final class ClientModEvents {

    private ClientModEvents() {
    }

    /**
     * Eventos del bus de registro del mod.
     */
    @EventBusSubscriber(
            modid = CraftersSoccer.MOD_ID,
            bus = EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static final class ModBusEvents {

        private ModBusEvents() {
        }

        /**
         * Registrar el renderer del balón.
         */
        @SubscribeEvent
        public static void registerEntityRenderers(
                EntityRenderersEvent.RegisterRenderers event
        ) {
            event.registerEntityRenderer(
                    ModEntities.SOCCER_BALL.get(),
                    SoccerBallRenderer::new
            );
        }

        /**
         * Registrar la geometría del modelo.
         */
        @SubscribeEvent
        public static void registerLayerDefinitions(
                EntityRenderersEvent.RegisterLayerDefinitions event
        ) {
            event.registerLayerDefinition(
                    SoccerBallModel.MODEL_LAYER,
                    SoccerBallModel::createBodyLayer
            );
        }

        /**
         * Registrar elementos personalizados del HUD.
         */
        @SubscribeEvent
        public static void registerGuiLayers(
                RegisterGuiLayersEvent event
        ) {
            /*
             * Barra de potencia.
             */
            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "soccer_power_bar"
                    ),
                    SoccerPowerBarGui::render
            );

            /*
             * Indicador de cruceta.
             */
            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "soccer_crosshair"
                    ),
                    SoccerCrosshairGui::render
            );
        }
    }

    /**
     * Eventos normales del juego.
     */
    @EventBusSubscriber(
            modid = CraftersSoccer.MOD_ID,
            bus = EventBusSubscriber.Bus.GAME,
            value = Dist.CLIENT
    )
    public static final class GameBusEvents {

        private GameBusEvents() {
        }

        /**
         * Actualizar el control del tiro cada tick.
         */
        @SubscribeEvent
        public static void onClientTick(
                ClientTickEvent.Post event
        ) {
            ClientSoccerState.clientTick(
                    Minecraft.getInstance()
            );
        }
    }
}