package org.crafterscr.crafterssoccer.registry;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sonidos personalizados de CraftersSoccer.
 */
public final class ModSounds {

    /**
     * Registro general de sonidos.
     */
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(
                    BuiltInRegistries.SOUND_EVENT,
                    CraftersSoccer.MOD_ID
            );

    /**
     * Silbato que indica el inicio real del partido,
     * justo después de terminar el conteo.
     */
    public static final DeferredHolder<
            SoundEvent,
            SoundEvent
            > WHISTLE_START =
            SOUND_EVENTS.register(
                    "whistle_start",
                    () ->
                            SoundEvent.createVariableRangeEvent(
                                    ResourceLocation.fromNamespaceAndPath(
                                            CraftersSoccer.MOD_ID,
                                            "whistle_start"
                                    )
                            )
            );

    /**
     * Silbato que indica el final completo del partido.
     */
    public static final DeferredHolder<
            SoundEvent,
            SoundEvent
            > WHISTLE_END =
            SOUND_EVENTS.register(
                    "whistle_end",
                    () ->
                            SoundEvent.createVariableRangeEvent(
                                    ResourceLocation.fromNamespaceAndPath(
                                            CraftersSoccer.MOD_ID,
                                            "whistle_end"
                                    )
                            )
            );

    private ModSounds() {
    }
}