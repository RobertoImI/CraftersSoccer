package org.crafterscr.crafterssoccer.field;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.crafterscr.crafterssoccer.CraftersSoccer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Almacena las canchas registradas.
 */
public final class SoccerFieldManager {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

    private static final Map<String, SoccerField> FIELDS =
            new LinkedHashMap<>();

    private static MinecraftServer loadedServer;
    private static boolean loaded;

    private SoccerFieldManager() {
    }

    public static String normalizeId(String id) {
        if (id == null) {
            return "";
        }

        return id.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_');
    }

    public static void ensureLoaded(
            MinecraftServer server
    ) {
        if (server == null) {
            return;
        }

        if (loaded && loadedServer == server) {
            return;
        }

        FIELDS.clear();

        loadedServer = server;
        loaded = true;

        load(server);
    }

    public static SoccerField createField(
            MinecraftServer server,
            String requestedId,
            String dimensionId
    ) {
        ensureLoaded(server);

        String normalizedId =
                normalizeId(requestedId);

        if (normalizedId.isBlank()
                || FIELDS.containsKey(normalizedId)) {
            return null;
        }

        SoccerField field =
                new SoccerField(
                        normalizedId,
                        dimensionId
                );

        FIELDS.put(
                normalizedId,
                field
        );

        save(server);

        return field;
    }

    public static SoccerField getField(
            MinecraftServer server,
            String requestedId
    ) {
        ensureLoaded(server);

        return FIELDS.get(
                normalizeId(requestedId)
        );
    }

    public static Collection<SoccerField> getFields(
            MinecraftServer server
    ) {
        ensureLoaded(server);

        List<SoccerField> result =
                new ArrayList<>(
                        FIELDS.values()
                );

        result.sort(
                Comparator.comparing(
                        SoccerField::getId
                )
        );

        return List.copyOf(result);
    }

    public static boolean removeField(
            MinecraftServer server,
            String requestedId
    ) {
        ensureLoaded(server);

        SoccerField removed =
                FIELDS.remove(
                        normalizeId(requestedId)
                );

        if (removed == null) {
            return false;
        }

        save(server);
        return true;
    }

    public static void save(
            MinecraftServer server
    ) {
        ensureLoaded(server);

        Path file = getFieldsFile(server);
        Path directory = file.getParent();

        try {
            Files.createDirectories(directory);

            FieldStorageData storageData =
                    new FieldStorageData();

            for (SoccerField field : FIELDS.values()) {
                storageData.fields.add(
                        FieldData.fromField(field)
                );
            }

            Path temporaryFile =
                    directory.resolve(
                            "fields.json.tmp"
                    );

            try (Writer writer =
                         Files.newBufferedWriter(
                                 temporaryFile
                         )) {

                GSON.toJson(
                        storageData,
                        writer
                );
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
                            + "] No se pudieron guardar las canchas."
            );

            exception.printStackTrace();
        }
    }

    private static void load(
            MinecraftServer server
    ) {
        Path file = getFieldsFile(server);

        if (!Files.exists(file)) {
            return;
        }

        try (Reader reader =
                     Files.newBufferedReader(file)) {

            FieldStorageData storageData =
                    GSON.fromJson(
                            reader,
                            FieldStorageData.class
                    );

            if (storageData == null
                    || storageData.fields == null) {
                return;
            }

            for (FieldData data : storageData.fields) {
                SoccerField field = data.toField();

                if (field == null) {
                    continue;
                }

                FIELDS.put(
                        normalizeId(field.getId()),
                        field
                );
            }

        } catch (Exception exception) {
            System.err.println(
                    "[" + CraftersSoccer.MOD_ID
                            + "] No se pudieron cargar las canchas."
            );

            exception.printStackTrace();
        }
    }

    private static Path getFieldsFile(
            MinecraftServer server
    ) {
        return server.getWorldPath(
                        LevelResource.ROOT
                )
                .resolve("serverconfig")
                .resolve(CraftersSoccer.MOD_ID)
                .resolve("fields.json");
    }

    private static final class FieldStorageData {

        private int version = 5;

        private List<FieldData> fields =
                new ArrayList<>();
    }

    private static final class FieldData {

        private String id;
        private String dimension;

        private PositionData fieldPosition1;
        private PositionData fieldPosition2;

        private PositionData center;
        private PositionData ballSpawn;

        private List<PositionData> redSpawns =
                new ArrayList<>();

        private List<PositionData> blueSpawns =
                new ArrayList<>();

        private PositionData redGoalPosition1;
        private PositionData redGoalPosition2;

        private PositionData blueGoalPosition1;
        private PositionData blueGoalPosition2;

        private PositionData redGoalkeeperAreaPosition1;
        private PositionData redGoalkeeperAreaPosition2;
        private PositionData blueGoalkeeperAreaPosition1;
        private PositionData blueGoalkeeperAreaPosition2;

        private PositionData redPenaltyPosition;
        private PositionData bluePenaltyPosition;

        private static FieldData fromField(
                SoccerField field
        ) {
            FieldData data = new FieldData();

            data.id = field.getId();
            data.dimension = field.getDimensionId();

            data.fieldPosition1 =
                    PositionData.fromBlockPos(
                            field.getFieldPosition1()
                    );

            data.fieldPosition2 =
                    PositionData.fromBlockPos(
                            field.getFieldPosition2()
                    );

            data.center =
                    PositionData.fromBlockPos(
                            field.getCenter()
                    );

            data.ballSpawn =
                    PositionData.fromBlockPos(
                            field.getBallSpawn()
                    );

            for (BlockPos position
                    : field.getRedSpawns()) {

                data.redSpawns.add(
                        PositionData.fromBlockPos(
                                position
                        )
                );
            }

            for (BlockPos position
                    : field.getBlueSpawns()) {

                data.blueSpawns.add(
                        PositionData.fromBlockPos(
                                position
                        )
                );
            }

            data.redGoalPosition1 =
                    PositionData.fromBlockPos(
                            field.getRedGoalPosition1()
                    );

            data.redGoalPosition2 =
                    PositionData.fromBlockPos(
                            field.getRedGoalPosition2()
                    );

            data.blueGoalPosition1 =
                    PositionData.fromBlockPos(
                            field.getBlueGoalPosition1()
                    );

            data.blueGoalPosition2 =
                    PositionData.fromBlockPos(
                            field.getBlueGoalPosition2()
                    );

            data.redGoalkeeperAreaPosition1 =
                    PositionData.fromBlockPos(
                            field.getRedGoalkeeperAreaPosition1()
                    );

            data.redGoalkeeperAreaPosition2 =
                    PositionData.fromBlockPos(
                            field.getRedGoalkeeperAreaPosition2()
                    );

            data.blueGoalkeeperAreaPosition1 =
                    PositionData.fromBlockPos(
                            field.getBlueGoalkeeperAreaPosition1()
                    );

            data.blueGoalkeeperAreaPosition2 =
                    PositionData.fromBlockPos(
                            field.getBlueGoalkeeperAreaPosition2()
                    );

            data.redPenaltyPosition =
                    PositionData.fromBlockPos(
                            field.getRedPenaltyPosition()
                    );

            data.bluePenaltyPosition =
                    PositionData.fromBlockPos(
                            field.getBluePenaltyPosition()
                    );

            return data;
        }

        private SoccerField toField() {
            String normalizedId =
                    SoccerFieldManager.normalizeId(id);

            if (normalizedId.isBlank()
                    || dimension == null
                    || dimension.isBlank()) {
                return null;
            }

            SoccerField field =
                    new SoccerField(
                            normalizedId,
                            dimension
                    );

            field.setFieldPosition1(
                    PositionData.toBlockPos(
                            fieldPosition1
                    )
            );

            field.setFieldPosition2(
                    PositionData.toBlockPos(
                            fieldPosition2
                    )
            );

            field.setCenter(
                    PositionData.toBlockPos(center)
            );

            field.setBallSpawn(
                    PositionData.toBlockPos(ballSpawn)
            );

            List<BlockPos> loadedRedSpawns =
                    new ArrayList<>();

            if (redSpawns != null) {
                for (PositionData positionData
                        : redSpawns) {

                    BlockPos position =
                            PositionData.toBlockPos(
                                    positionData
                            );

                    if (position != null) {
                        loadedRedSpawns.add(
                                position
                        );
                    }
                }
            }

            field.setRedSpawns(
                    loadedRedSpawns
            );

            List<BlockPos> loadedBlueSpawns =
                    new ArrayList<>();

            if (blueSpawns != null) {
                for (PositionData positionData
                        : blueSpawns) {

                    BlockPos position =
                            PositionData.toBlockPos(
                                    positionData
                            );

                    if (position != null) {
                        loadedBlueSpawns.add(
                                position
                        );
                    }
                }
            }

            field.setBlueSpawns(
                    loadedBlueSpawns
            );

            field.setRedGoalPosition1(
                    PositionData.toBlockPos(
                            redGoalPosition1
                    )
            );

            field.setRedGoalPosition2(
                    PositionData.toBlockPos(
                            redGoalPosition2
                    )
            );

            field.setBlueGoalPosition1(
                    PositionData.toBlockPos(
                            blueGoalPosition1
                    )
            );

            field.setBlueGoalPosition2(
                    PositionData.toBlockPos(
                            blueGoalPosition2
                    )
            );

            field.setRedGoalkeeperAreaPosition1(
                    PositionData.toBlockPos(
                            redGoalkeeperAreaPosition1
                    )
            );

            field.setRedGoalkeeperAreaPosition2(
                    PositionData.toBlockPos(
                            redGoalkeeperAreaPosition2
                    )
            );

            field.setBlueGoalkeeperAreaPosition1(
                    PositionData.toBlockPos(
                            blueGoalkeeperAreaPosition1
                    )
            );

            field.setBlueGoalkeeperAreaPosition2(
                    PositionData.toBlockPos(
                            blueGoalkeeperAreaPosition2
                    )
            );

            field.setRedPenaltyPosition(
                    PositionData.toBlockPos(
                            redPenaltyPosition
                    )
            );

            field.setBluePenaltyPosition(
                    PositionData.toBlockPos(
                            bluePenaltyPosition
                    )
            );

            return field;
        }
    }

    private static final class PositionData {

        private int x;
        private int y;
        private int z;

        private PositionData() {
        }

        private PositionData(
                int x,
                int y,
                int z
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static PositionData fromBlockPos(
                BlockPos position
        ) {
            if (position == null) {
                return null;
            }

            return new PositionData(
                    position.getX(),
                    position.getY(),
                    position.getZ()
            );
        }

        private static BlockPos toBlockPos(
                PositionData data
        ) {
            if (data == null) {
                return null;
            }

            return new BlockPos(
                    data.x,
                    data.y,
                    data.z
            );
        }
    }
}