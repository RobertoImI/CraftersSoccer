package org.crafterscr.crafterssoccer.command;

import org.crafterscr.crafterssoccer.entity.SoccerBallEntity;
import org.crafterscr.crafterssoccer.registry.ModEntities;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Comandos administrativos.
 */
public final class SoccerCommands {

    private SoccerCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        CommandDispatcher<CommandSourceStack> dispatcher =
                event.getDispatcher();

        dispatcher.register(
                Commands.literal("soccer")
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )

                        .then(
                                Commands.literal("ball")

                                        /*
                                         * /soccer ball spawn
                                         */
                                        .then(
                                                Commands.literal("spawn")
                                                        .executes(
                                                                context ->
                                                                        spawnBall(
                                                                                context.getSource()
                                                                        )
                                                        )
                                        )

                                        /*
                                         * /soccer ball removeall
                                         */
                                        .then(
                                                Commands.literal("removeall")
                                                        .executes(
                                                                context ->
                                                                        removeAllBalls(
                                                                                context.getSource()
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    /**
     * Crear un balón frente al jugador.
     */
    private static int spawnBall(
            CommandSourceStack source
    ) {
        ServerLevel level =
                source.getLevel();

        SoccerBallEntity ball =
                new SoccerBallEntity(
                        ModEntities.SOCCER_BALL.get(),
                        level
                );

        Vec3 spawnPosition;

        try {
            ServerPlayer player =
                    source.getPlayerOrException();

            Vec3 look =
                    player.getLookAngle();

            spawnPosition =
                    player.getEyePosition()
                            .add(
                                    look.scale(1.8D)
                            )
                            .add(
                                    0.0D,
                                    -0.55D,
                                    0.0D
                            );

        } catch (Exception ignored) {
            spawnPosition =
                    source.getPosition();
        }

        ball.setPos(
                spawnPosition.x,
                spawnPosition.y,
                spawnPosition.z
        );

        level.addFreshEntity(ball);

        source.sendSuccess(
                () -> Component.literal(
                        "§aBalón creado correctamente."
                ),
                true
        );

        return 1;
    }

    /**
     * Eliminar todos los balones de la dimensión.
     */
    private static int removeAllBalls(
            CommandSourceStack source
    ) {
        ServerLevel level =
                source.getLevel();

        int removed = 0;

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SoccerBallEntity ball) {
                ball.discard();
                removed++;
            }
        }

        int finalRemoved =
                removed;

        source.sendSuccess(
                () -> Component.literal(
                        "§eBalones eliminados: "
                                + finalRemoved
                ),
                true
        );

        return removed;
    }
}