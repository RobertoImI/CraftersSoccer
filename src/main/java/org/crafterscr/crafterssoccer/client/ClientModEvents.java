package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;
import org.crafterscr.crafterssoccer.registry.ModEntities;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Eventos exclusivos del cliente.
 */
public final class ClientModEvents {

    private ClientModEvents() {
    }

    @EventBusSubscriber(
            modid = CraftersSoccer.MOD_ID,
            bus = EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static final class ModBusEvents {

        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void onClientSetup(
                FMLClientSetupEvent event
        ) {
            KnockdownAnimationClient.registerFactory();
            SlideAnimationClient.registerFactory();
        }

        @SubscribeEvent
        public static void registerEntityRenderers(
                EntityRenderersEvent.RegisterRenderers event
        ) {
            event.registerEntityRenderer(
                    ModEntities.SOCCER_BALL.get(),
                    SoccerBallRenderer::new
            );
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(
                EntityRenderersEvent.RegisterLayerDefinitions event
        ) {
            event.registerLayerDefinition(
                    SoccerBallModel.MODEL_LAYER,
                    SoccerBallModel::createBodyLayer
            );
        }

        @SubscribeEvent
        public static void registerKeyMappings(
                RegisterKeyMappingsEvent event
        ) {
            event.register(
                    RefereeKeyMappings.WHISTLE
            );

            event.register(
                    KnockdownKeyMappings.STAND_UP
            );

            event.register(
                    SlideKeyMappings.SLIDE
            );

            event.register(
                    BroadcastCameraKeyMappings.TOGGLE_BROADCAST
            );

            event.register(
                    BroadcastCameraKeyMappings.PREVIOUS_CAMERA
            );

            event.register(
                    BroadcastCameraKeyMappings.NEXT_CAMERA
            );
        }

        @SubscribeEvent
        public static void registerGuiLayers(
                RegisterGuiLayersEvent event
        ) {
            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "soccer_match_hud"
                    ),
                    SoccerMatchHud::render
            );

            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "soccer_power_bar"
                    ),
                    SoccerPowerBarGui::render
            );

            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "soccer_crosshair"
                    ),
                    SoccerCrosshairGui::render
            );

            event.registerAboveAll(
                    ResourceLocation.fromNamespaceAndPath(
                            CraftersSoccer.MOD_ID,
                            "knockdown_recovery"
                    ),
                    KnockdownRecoveryHud::render
            );
        }
    }

    @EventBusSubscriber(
            modid = CraftersSoccer.MOD_ID,
            bus = EventBusSubscriber.Bus.GAME,
            value = Dist.CLIENT
    )
    public static final class GameBusEvents {

        private GameBusEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(
                ClientTickEvent.Post event
        ) {
            Minecraft minecraft =
                    Minecraft.getInstance();

            RefereeKeyMappings.clientTick();
            KnockdownKeyMappings.clientTick();
            SlideKeyMappings.clientTick();
            BroadcastCameraKeyMappings.clientTick();

            ClientKnockdownState.clientTick(
                    minecraft
            );

            ClientSlideState.clientTick(
                    minecraft
            );

            KnockdownCameraController.clientTick(
                    minecraft
            );

            ClientSoccerState.clientTick(
                    minecraft
            );

            ClientBroadcastCameraState.clientTick(
                    minecraft
            );

            if (minecraft.player == null
                    || minecraft.level == null) {

                KnockdownCameraController.restoreCamera();
                ClientMatchState.reset();
                ClientKnockdownState.reset();
                ClientRecoveryState.reset();
                ClientSlideState.reset();
                ClientBroadcastCameraState.reset();
            }
        }
    }
}