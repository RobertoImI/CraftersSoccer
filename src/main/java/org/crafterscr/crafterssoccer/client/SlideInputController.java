package org.crafterscr.crafterssoccer.client;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/**
 * Bloquea el movimiento vanilla mientras el servidor mantiene al jugador
 * dentro de un barrido.
 *
 * La cámara sigue siendo libre: el jugador puede mirar alrededor, pero
 * WASD, salto y agacharse no pueden alterar la trayectoria del tackle.
 */
@EventBusSubscriber(
        modid = CraftersSoccer.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class SlideInputController {

    private SlideInputController() {
    }

    @SubscribeEvent
    public static void onMovementInput(
            MovementInputUpdateEvent event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || event.getEntity() != minecraft.player
                || !ClientSlideState.isLocalPlayerSliding()) {
            return;
        }

        Input input =
                event.getInput();

        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;

        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }
}
