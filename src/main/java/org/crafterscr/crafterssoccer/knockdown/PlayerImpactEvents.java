package org.crafterscr.crafterssoccer.knockdown;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * Detecta exclusivamente ataques directos de jugador contra jugador.
 *
 * Para NeoForge 1.21.1 se utiliza AttackEntityEvent.
 */
@EventBusSubscriber(
        modid = CraftersSoccer.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME
)
public final class PlayerImpactEvents {

    private PlayerImpactEvents() {
    }

    @SubscribeEvent
    public static void onPlayerAttack(
            AttackEntityEvent event
    ) {
        if (!(event.getEntity()
                instanceof ServerPlayer attacker)) {
            return;
        }

        /*
         * Un jugador derribado no puede atacar.
         */
        if (PlayerImpactManager.isKnockedDown(
                attacker.getUUID()
        )) {
            event.setCanceled(true);
            return;
        }

        /*
         * Solamente cuentan ataques directos contra otro jugador.
         */
        if (!(event.getTarget()
                instanceof ServerPlayer victim)) {
            return;
        }

        if (victim == attacker) {
            return;
        }

        float attackDamage =
                (float) attacker.getAttributeValue(
                        Attributes.ATTACK_DAMAGE
                );

        PlayerImpactManager.addNormalPlayerHit(
                victim,
                attacker,
                attackDamage
        );
    }
}