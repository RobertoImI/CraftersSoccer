package org.crafterscr.crafterssoccer.match;

import org.crafterscr.crafterssoccer.CraftersSoccer;
import org.crafterscr.crafterssoccer.referee.RefereeCardManager;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Eventos del servidor para partidos y restauración de equipos.
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
        SoccerMatchManager.tick(event.getServer());

        RefereeCardManager.tick(
                event.getServer()
        );

        org.crafterscr.crafterssoccer.knockdown
                .PlayerImpactManager.tick(
                        event.getServer()
                );

        org.crafterscr.crafterssoccer.slide
                .SlideManager.tick(
                        event.getServer()
                );
    }

    /**
     * Al entrar, el jugador conserva su equipo porque la asignación
     * está guardada por UUID en teams.json.
     */
    @SubscribeEvent
    public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SoccerMatchManager.synchronizePlayer(
                player.getServer(),
                player
        );

        RefereeCardManager.ensureRefereeCards(
                player.getServer(),
                player
        );

        RefereeCardManager.handlePlayerLogin(
                player.getServer(),
                player
        );

        org.crafterscr.crafterssoccer.knockdown
                .PlayerImpactManager.handleLogin(
                        player
                );
    }
}
