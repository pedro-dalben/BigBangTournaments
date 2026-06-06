package com.bigbang_tournaments.storage;

import com.bigbang_tournaments.model.TournamentSnapshot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SnapshotStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIR_NAME = "bigbang_tournaments";

    private static Path getStorageDirectory(MinecraftServer server) {
        Path serverConfig = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        return serverConfig.resolve(DIR_NAME);
    }

    public static Path getSnapshotPath(MinecraftServer server, UUID playerUuid) {
        return getStorageDirectory(server).resolve(playerUuid.toString() + ".json");
    }

    public static synchronized TournamentSnapshot loadSnapshot(MinecraftServer server, UUID playerUuid) {
        Path path = getSnapshotPath(server, playerUuid);
        if (!Files.exists(path)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            TournamentSnapshot snapshot = GSON.fromJson(reader, TournamentSnapshot.class);
            if (snapshot != null && snapshot.getSchemaVersion() <= 0) {
                snapshot.setSchemaVersion(1);
            }
            return snapshot;
        } catch (Exception e) {
            LOGGER.error("Failed to load snapshot for player UUID " + playerUuid, e);
            return null;
        }
    }

    public static synchronized void saveSnapshot(MinecraftServer server, TournamentSnapshot snapshot) throws IOException {
        UUID playerUuid = snapshot.getPlayerUuid();
        Path dir = getStorageDirectory(server);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path targetPath = getSnapshotPath(server, playerUuid);
        Path tempPath = dir.resolve(playerUuid.toString() + ".json.tmp");

        // Write atomically: first write to .tmp, then move to final destination
        try {
            try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            }
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                LOGGER.warn("Atomic move not supported for player UUID {}, falling back to regular replace", playerUuid);
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write snapshot atomically for player UUID " + playerUuid, e);
            // Cleanup temp file if exists
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            throw e;
        }
    }

    public static synchronized boolean deleteSnapshot(MinecraftServer server, UUID playerUuid) {
        Path path = getSnapshotPath(server, playerUuid);
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.error("Failed to delete snapshot for player UUID " + playerUuid, e);
            return false;
        }
    }

    public static boolean hasSnapshot(MinecraftServer server, UUID playerUuid) {
        return Files.exists(getSnapshotPath(server, playerUuid));
    }

    public static List<TournamentSnapshot> listSnapshots(MinecraftServer server) {
        List<TournamentSnapshot> snapshots = new ArrayList<>();
        Path dir = getStorageDirectory(server);
        if (!Files.exists(dir)) {
            return snapshots;
        }

        try (var stream = Files.list(dir)) {
            stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().equals("tournament_state.json"))
                    .filter(path -> !path.getFileName().toString().equals("tournament_config.json"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String uuidString = fileName.substring(0, fileName.length() - ".json".length());
                        try {
                            UUID uuid = UUID.fromString(uuidString);
                            TournamentSnapshot snapshot = loadSnapshot(server, uuid);
                            if (snapshot != null) {
                                snapshots.add(snapshot);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to list snapshots", e);
        }

        return snapshots;
    }
}
