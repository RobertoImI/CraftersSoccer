package org.crafterscr.crafterssoccer;

import org.crafterscr.crafterssoccer.command.SoccerCommands;
import org.crafterscr.crafterssoccer.network.ModNetworking;
import org.crafterscr.crafterssoccer.registry.ModEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Clase principal de CraftersSoccer.
 */
@Mod(CraftersSoccer.MOD_ID)
public final class CraftersSoccer {

    /**
     * Identificador interno del mod.
     */
    public static final String MOD_ID = "crafterssoccer";

    public CraftersSoccer(IEventBus modEventBus) {

        /*
         * Registrar entidades.
         */
        ModEntities.ENTITY_TYPES.register(modEventBus);

        /*
         * Registrar paquetes de red.
         */
        modEventBus.addListener(
                ModNetworking::registerPayloads
        );

        /*
         * Registrar comandos.
         */
        NeoForge.EVENT_BUS.addListener(
                SoccerCommands::register
        );
    }
}