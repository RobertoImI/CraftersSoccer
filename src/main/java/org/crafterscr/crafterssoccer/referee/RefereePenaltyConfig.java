package org.crafterscr.crafterssoccer.referee;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Guarda la duración de las expulsiones.
 */
public final class RefereePenaltyConfig {

    public static final int DEFAULT_MINUTES = 3;
    public static final int MIN_MINUTES = 1;
    public static final int MAX_MINUTES = 5;

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static MinecraftServer loadedServer;
    private static boolean loaded;
    private static int redCardMinutes =
            DEFAULT_MINUTES;

    private RefereePenaltyConfig() {
    }

    public static void ensureLoaded(
            MinecraftServer server
    ) {
        if (loaded && loadedServer == server) {
            return;
        }

        loadedServer = server;
        loaded = true;
        redCardMinutes = DEFAULT_MINUTES;

        Path file = getFile(server);

        if (!Files.exists(file)) {
            return;
        }

        try (Reader reader =
                     Files.newBufferedReader(file)) {

            StoredData data =
                    GSON.fromJson(
                            reader,
                            StoredData.class
                    );

            if (data != null) {
                redCardMinutes =
                        clampMinutes(
                                data.redCardMinutes
                        );
            }

        } catch (Exception exception) {
            System.err.println(
                    "[" + CraftersSoccer.MOD_ID
                            + "] No se pudo cargar referee.json."
            );
            exception.printStackTrace();
        }
    }

    public static int getRedCardMinutes(
            MinecraftServer server
    ) {
        ensureLoaded(server);
        return redCardMinutes;
    }

    public static int getRedCardTicks(
            MinecraftServer server
    ) {
        return getRedCardMinutes(server)
                * 60
                * 20;
    }

    public static int setRedCardMinutes(
            MinecraftServer server,
            int requestedMinutes
    ) {
        ensureLoaded(server);

        redCardMinutes =
                clampMinutes(
                        requestedMinutes
                );

        save(server);
        return redCardMinutes;
    }

    private static int clampMinutes(
            int value
    ) {
        return Math.max(
                MIN_MINUTES,
                Math.min(
                        MAX_MINUTES,
                        value
                )
        );
    }

    private static void save(
            MinecraftServer server
    ) {
        Path file = getFile(server);
        Path directory = file.getParent();

        StoredData data =
                new StoredData();

        data.redCardMinutes =
                redCardMinutes;

        try {
            Files.createDirectories(directory);

            Path temporary =
                    directory.resolve(
                            "referee.json.tmp"
                    );

            try (Writer writer =
                         Files.newBufferedWriter(temporary)) {
                GSON.toJson(data, writer);
            }

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
                    "[" + CraftersSoccer.MOD_ID
                            + "] No se pudo guardar referee.json."
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
                .resolve("serverconfig")
                .resolve(CraftersSoccer.MOD_ID)
                .resolve("referee.json");
    }

    private static final class StoredData {
        private int version = 1;
        private int redCardMinutes =
                DEFAULT_MINUTES;
    }
}
