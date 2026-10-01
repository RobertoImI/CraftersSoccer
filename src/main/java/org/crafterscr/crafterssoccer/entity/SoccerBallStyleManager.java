package org.crafterscr.crafterssoccer.entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.WeakHashMap;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Guarda la apariencia global seleccionada para los próximos balones.
 *
 * Archivo:
 * <mundo>/serverconfig/crafterssoccer/ball_style.txt
 */
public final class SoccerBallStyleManager {

    private static final Map<
            MinecraftServer,
            SoccerBallStyle
            > SELECTED_STYLES =
            new WeakHashMap<>();

    private SoccerBallStyleManager() {
    }

    public static synchronized SoccerBallStyle getSelectedStyle(
            MinecraftServer server
    ) {
        if (server == null) {
            return SoccerBallStyle.DEFAULT;
        }

        SoccerBallStyle cached =
                SELECTED_STYLES.get(
                        server
                );

        if (cached != null) {
            return cached;
        }

        SoccerBallStyle loaded =
                load(server);

        SELECTED_STYLES.put(
                server,
                loaded
        );

        return loaded;
    }

    public static synchronized void setSelectedStyle(
            MinecraftServer server,
            SoccerBallStyle style
    ) {
        if (server == null) {
            return;
        }

        SoccerBallStyle safeStyle =
                style == null
                        ? SoccerBallStyle.DEFAULT
                        : style;

        SELECTED_STYLES.put(
                server,
                safeStyle
        );

        save(
                server,
                safeStyle
        );
    }

    private static SoccerBallStyle load(
            MinecraftServer server
    ) {
        Path file =
                getFile(server);

        if (!Files.exists(file)) {
            return SoccerBallStyle.DEFAULT;
        }

        try {
            String value =
                    Files.readString(file)
                            .trim();

            return SoccerBallStyle
                    .fromCommandName(
                            value
                    );

        } catch (IOException exception) {
            System.err.println(
                    "["
                            + CraftersSoccer.MOD_ID
                            + "] No se pudo cargar ball_style.txt."
            );

            exception.printStackTrace();
            return SoccerBallStyle.DEFAULT;
        }
    }

    private static void save(
            MinecraftServer server,
            SoccerBallStyle style
    ) {
        Path file =
                getFile(server);

        Path directory =
                file.getParent();

        try {
            Files.createDirectories(
                    directory
            );

            Path temporary =
                    directory.resolve(
                            "ball_style.txt.tmp"
                    );

            Files.writeString(
                    temporary,
                    style.getCommandName()
            );

            try {
                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (IOException ignored) {
                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException exception) {
            System.err.println(
                    "["
                            + CraftersSoccer.MOD_ID
                            + "] No se pudo guardar ball_style.txt."
            );

            exception.printStackTrace();
        }
    }

    private static Path getFile(
            MinecraftServer server
    ) {
        return server.getWorldPath(
                        LevelResource.ROOT
                )
                .resolve(
                        "serverconfig"
                )
                .resolve(
                        CraftersSoccer.MOD_ID
                )
                .resolve(
                        "ball_style.txt"
                );
    }
}
