package org.crafterscr.crafterssoccer.match;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Eventos del servidor para actualizar los partidos.
 */
@EventBusSubscriber(
        modid = CraftersSoccer.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME
)
public final class SoccerServerEvents {

    private SoccerServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        SoccerMatchManager.tick(
                event.getServer()
        );
    }

    /**
     * Sincroniza el HUD cuando un jugador entra.
     */
    @SubscribeEvent
    public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity()
                instanceof net.minecraft.server.level.ServerPlayer player)) {
            return;
        }

        SoccerMatchManager.synchronizePlayer(
                player.getServer(),
                player
        );
    }
}