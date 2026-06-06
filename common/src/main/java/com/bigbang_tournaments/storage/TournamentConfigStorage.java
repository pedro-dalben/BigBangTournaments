package com.bigbang_tournaments.storage;

import com.bigbang_tournaments.model.TournamentConfig;
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

public final class TournamentConfigStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(TournamentConfigStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIR_NAME = "bigbang_tournaments";
    private static final String FILE_NAME = "tournament_config.json";

    private TournamentConfigStorage() {
    }

    private static Path getStorageDirectory(MinecraftServer server) {
        Path serverConfig = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        return serverConfig.resolve(DIR_NAME);
    }

    public static Path getConfigPath(MinecraftServer server) {
        return getStorageDirectory(server).resolve(FILE_NAME);
    }

    public static synchronized TournamentConfig loadOrCreate(MinecraftServer server) {
        Path path = getConfigPath(server);
        if (!Files.exists(path)) {
            TournamentConfig config = new TournamentConfig();
            config.normalizeAndMigrate();
            save(server, config);
            return config;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            TournamentConfig config = GSON.fromJson(reader, TournamentConfig.class);
            if (config == null) {
                config = new TournamentConfig();
            }
            if (config.normalizeAndMigrate()) {
                save(server, config);
            }
            return config;
        } catch (Exception e) {
            LOGGER.error("Failed to load tournament config, using defaults", e);
            TournamentConfig config = new TournamentConfig();
            config.normalizeAndMigrate();
            return config;
        }
    }

    public static synchronized void save(MinecraftServer server, TournamentConfig config) {
        Path dir = getStorageDirectory(server);
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            try (Writer writer = Files.newBufferedWriter(getConfigPath(server), StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save tournament config", e);
        }
    }
}
