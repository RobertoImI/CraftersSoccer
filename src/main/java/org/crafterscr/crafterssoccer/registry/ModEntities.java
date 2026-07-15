package org.crafterscr.crafterssoccer.registry;

import org.crafterscr.crafterssoccer.CraftersSoccer;
import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registro de entidades.
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    CraftersSoccer.MOD_ID
            );

    /**
     * Entidad del balón.
     */
    public static final DeferredHolder<
            EntityType<?>,
            EntityType<SoccerBallEntity>
            > SOCCER_BALL =
            ENTITY_TYPES.register(
                    "soccer_ball",
                    () ->
                            EntityType.Builder
                                    .<SoccerBallEntity>of(
                                            SoccerBallEntity::new,
                                            MobCategory.MISC
                                    )

                                    /*
                                     * Hitbox de 0.48 bloques.
                                     */
                                    .sized(
                                            0.48F,
                                            0.48F
                                    )

                                    /*
                                     * Distancia de seguimiento.
                                     */
                                    .clientTrackingRange(12)

                                    /*
                                     * Actualizar en red cada tick.
                                     */
                                    .updateInterval(1)

                                    .build(
                                            "soccer_ball"
                                    )
            );

    private ModEntities() {
    }
}