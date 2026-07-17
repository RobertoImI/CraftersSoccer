package org.crafterscr.crafterssoccer.match;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Guarda permanentemente las plantillas y nombres de los equipos.
 *
 * Archivo generado:
 * <mundo>/serverconfig/crafterssoccer/teams.json
 */
public final class SoccerTeamStorage {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

    private SoccerTeamStorage() {
    }

    public static TeamData load(
            MinecraftServer server
    ) {
        Path file = getFile(server);

        if (!Files.exists(file)) {
            return TeamData.defaults();
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            StoredData stored = GSON.fromJson(reader, StoredData.class);

            if (stored == null) {
                return TeamData.defaults();
            }

            return new TeamData(
                    sanitizeName(stored.redTeamName, "Equipo Rojo"),
                    sanitizeName(stored.blueTeamName, "Equipo Azul"),
                    parseUuidSet(stored.redPlayers),
                    parseUuidSet(stored.bluePlayers),
                    parseUuid(stored.redGoalkeeper),
                    parseUuid(stored.blueGoalkeeper)
            );

        } catch (Exception exception) {
            System.err.println(
                    "[" + CraftersSoccer.MOD_ID
                            + "] No se pudo cargar teams.json."
            );
            exception.printStackTrace();
            return TeamData.defaults();
        }
    }

    public static void save(
            MinecraftServer server,
            String redTeamName,
            String blueTeamName,
            Set<UUID> redPlayers,
            Set<UUID> bluePlayers,
            UUID redGoalkeeper,
            UUID blueGoalkeeper
    ) {
        Path file = getFile(server);
        Path directory = file.getParent();

        StoredData stored = new StoredData();
        stored.redTeamName = sanitizeName(redTeamName, "Equipo Rojo");
        stored.blueTeamName = sanitizeName(blueTeamName, "Equipo Azul");
        stored.redPlayers = toStringList(redPlayers);
        stored.bluePlayers = toStringList(bluePlayers);
        stored.redGoalkeeper = redGoalkeeper == null
                ? null
                : redGoalkeeper.toString();
        stored.blueGoalkeeper = blueGoalkeeper == null
                ? null
                : blueGoalkeeper.toString();

        try {
            Files.createDirectories(directory);

            Path temporaryFile =
                    directory.resolve("teams.json.tmp");

            try (Writer writer = Files.newBufferedWriter(temporaryFile)) {
                GSON.toJson(stored, writer);
            }

            try {
                Files.move(
                        temporaryFile,
                        file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException ignored) {
                Files.move(
                        temporaryFile,
                        file,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException exception) {
            System.err.println(
                    "[" + CraftersSoccer.MOD_ID
                            + "] No se pudo guardar teams.json."
            );
            exception.printStackTrace();
        }
    }

    private static Path getFile(
            MinecraftServer server
    ) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig")
                .resolve(CraftersSoccer.MOD_ID)
                .resolve("teams.json");
    }

    private static LinkedHashSet<UUID> parseUuidSet(
            List<String> values
    ) {
        LinkedHashSet<UUID> result = new LinkedHashSet<>();

        if (values == null) {
            return result;
        }

        for (String value : values) {
            UUID uuid = parseUuid(value);
            if (uuid != null) {
                result.add(uuid);
            }
        }

        return result;
    }

    private static UUID parseUuid(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<String> toStringList(
            Set<UUID> values
    ) {
        List<String> result = new ArrayList<>();

        for (UUID uuid : values) {
            result.add(uuid.toString());
        }

        return result;
    }

    private static String sanitizeName(
            String value,
            String fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String cleaned = value
                .replaceAll("§.", "")
                .trim()
                .replaceAll("\\s+", " ");

        if (cleaned.isBlank()) {
            return fallback;
        }

        return cleaned.length() > 24
                ? cleaned.substring(0, 24)
                : cleaned;
    }

    public record TeamData(
            String redTeamName,
            String blueTeamName,
            LinkedHashSet<UUID> redPlayers,
            LinkedHashSet<UUID> bluePlayers,
            UUID redGoalkeeper,
            UUID blueGoalkeeper
    ) {
        public static TeamData defaults() {
            return new TeamData(
                    "Equipo Rojo",
                    "Equipo Azul",
                    new LinkedHashSet<>(),
                    new LinkedHashSet<>(),
                    null,
                    null
            );
        }
    }

    private static final class StoredData {
        private int version = 1;
        private String redTeamName = "Equipo Rojo";
        private String blueTeamName = "Equipo Azul";
        private List<String> redPlayers = new ArrayList<>();
        private List<String> bluePlayers = new ArrayList<>();
        private String redGoalkeeper;
        private String blueGoalkeeper;
    }
}
