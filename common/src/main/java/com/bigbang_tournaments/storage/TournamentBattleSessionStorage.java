package com.bigbang_tournaments.storage;

import com.bigbang_tournaments.model.TournamentBattleSession;
import com.bigbang_tournaments.model.TournamentBattleStatus;
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

public class TournamentBattleSessionStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(TournamentBattleSessionStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SESSION_DIR = "team_preview_sessions";

    private static Path getSessionsDir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig")
                .resolve("bigbang_tournaments")
                .resolve(SESSION_DIR);
    }

    private static Path getSessionDir(MinecraftServer server, UUID sessionId) {
        return getSessionsDir(server).resolve(sessionId.toString());
    }

    private static Path getSessionFile(MinecraftServer server, UUID sessionId) {
        return getSessionDir(server, sessionId).resolve("session.json");
    }

    public static synchronized void saveSession(MinecraftServer server, TournamentBattleSession session) throws IOException {
        UUID sessionId = session.getSessionId();
        Path sessionDir = getSessionDir(server, sessionId);
        if (!Files.exists(sessionDir)) {
            Files.createDirectories(sessionDir);
        }

        Path targetPath = getSessionFile(server, sessionId);
        Path tempPath = sessionDir.resolve("session.json.tmp");

        try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
            GSON.toJson(session, writer);
            writer.flush();
        }
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static synchronized TournamentBattleSession loadSession(MinecraftServer server, UUID sessionId) {
        Path file = getSessionFile(server, sessionId);
        if (!Files.exists(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, TournamentBattleSession.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load session {}", sessionId, e);
            return null;
        }
    }

    public static synchronized boolean deleteSession(MinecraftServer server, UUID sessionId) {
        Path sessionDir = getSessionDir(server, sessionId);
        if (!Files.exists(sessionDir)) {
            return false;
        }
        try {
            try (var stream = Files.list(sessionDir)) {
                stream.forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                });
            }
            Files.deleteIfExists(sessionDir);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to delete session directory for {}", sessionId, e);
            return false;
        }
    }

    public static List<TournamentBattleSession> listActiveSessions(MinecraftServer server) {
        List<TournamentBattleSession> sessions = new ArrayList<>();
        Path dir = getSessionsDir(server);
        if (!Files.exists(dir)) {
            return sessions;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isDirectory).forEach(sessionDir -> {
                String dirName = sessionDir.getFileName().toString();
                try {
                    UUID sessionId = UUID.fromString(dirName);
                    TournamentBattleSession session = loadSession(server, sessionId);
                    if (session != null && !session.getState().isTerminal()) {
                        sessions.add(session);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to list session directories", e);
        }
        return sessions;
    }

    public static boolean snapshotExists(MinecraftServer server, UUID sessionId, UUID playerUuid) {
        Path snapshotFile = getSnapshotFile(server, sessionId, playerUuid);
        return Files.exists(snapshotFile);
    }

    public static Path getSnapshotFile(MinecraftServer server, UUID sessionId, UUID playerUuid) {
        return getSessionDir(server, sessionId).resolve(playerUuid.toString() + ".nbt");
    }

    public static Path getSessionDirPath(MinecraftServer server, UUID sessionId) {
        return getSessionDir(server, sessionId);
    }
}
