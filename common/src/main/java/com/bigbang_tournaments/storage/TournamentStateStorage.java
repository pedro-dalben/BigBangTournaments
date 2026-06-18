package com.bigbang_tournaments.storage;

import com.bigbang_tournaments.model.TournamentState;
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
import java.nio.file.Files;
import java.nio.file.Path;

public final class TournamentStateStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(TournamentStateStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIR_NAME = "bigbang_tournaments";
    private static final String FILE_NAME = "tournament_state.json";

    private TournamentStateStorage() {
    }

    private static Path getStorageDirectory(MinecraftServer server) {
        Path serverConfig = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        return serverConfig.resolve(DIR_NAME);
    }

    public static Path getStatePath(MinecraftServer server) {
        return getStorageDirectory(server).resolve(FILE_NAME);
    }

    public static synchronized TournamentState loadOrCreate(MinecraftServer server) {
        Path path = getStatePath(server);
        if (!Files.exists(path)) {
            TournamentState state = new TournamentState();
            save(server, state);
            return state;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            TournamentState state = GSON.fromJson(reader, TournamentState.class);
            if (state == null) {
                state = new TournamentState();
            }
            if (state.normalizeAndMigrate()) {
                save(server, state);
            }
            return state;
        } catch (Exception e) {
            LOGGER.error("Failed to load tournament state, using empty state", e);
            TournamentState state = new TournamentState();
            state.normalizeAndMigrate();
            return state;
        }
    }

    public static synchronized void save(MinecraftServer server, TournamentState state) {
        Path dir = getStorageDirectory(server);
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            try (Writer writer = Files.newBufferedWriter(getStatePath(server), StandardCharsets.UTF_8)) {
                GSON.toJson(state, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save tournament state", e);
        }
    }
}
