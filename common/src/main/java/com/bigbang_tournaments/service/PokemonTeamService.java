package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.PokemonSnapshot;
import com.bigbang_tournaments.model.TournamentSnapshot;
import com.bigbang_tournaments.storage.SnapshotStorage;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class PokemonTeamService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokemonTeamService.class);

    public static class PrepareResult {
        public enum Status {
            SUCCESS,
            ALREADY_HAS_SNAPSHOT,
            EMPTY_PARTY,
            ERROR
        }

        private final Status status;
        private final TournamentSnapshot snapshot;

        public PrepareResult(Status status, TournamentSnapshot snapshot) {
            this.status = status;
            this.snapshot = snapshot;
        }

        public Status getStatus() {
            return status;
        }

        public TournamentSnapshot getSnapshot() {
            return snapshot;
        }
    }

    public static class RestoreResult {
        public enum Status {
            SUCCESS,
            PARTIAL,
            NO_SNAPSHOT,
            ERROR
        }

        private final Status status;
        private final List<String> restoredPokemon;
        private final List<String> missingPokemon;

        public RestoreResult(Status status, List<String> restoredPokemon, List<String> missingPokemon) {
            this.status = status;
            this.restoredPokemon = restoredPokemon;
            this.missingPokemon = missingPokemon;
        }

        public Status getStatus() {
            return status;
        }

        public List<String> getRestoredPokemon() {
            return restoredPokemon;
        }

        public List<String> getMissingPokemon() {
            return missingPokemon;
        }
    }

    public static class ValidateResult {
        private final boolean valid;
        private final List<String> invalidReasons;

        public ValidateResult(boolean valid, List<String> invalidReasons) {
            this.valid = valid;
            this.invalidReasons = invalidReasons;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getInvalidReasons() {
            return invalidReasons;
        }
    }

    /**
     * Prepares a player's team for a tournament: captures snapshot, sets level, and heals them.
     */
    public static PrepareResult prepareTeam(ServerPlayer player, int targetLevel, boolean force) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return new PrepareResult(PrepareResult.Status.ERROR, null);
        }

        UUID playerUuid = player.getUUID();
        if (!force && SnapshotStorage.hasSnapshot(server, playerUuid)) {
            return new PrepareResult(PrepareResult.Status.ALREADY_HAS_SNAPSHOT, null);
        }

        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        List<PokemonSnapshot> pokemonSnapshots = new ArrayList<>();
        List<Pokemon> partyPokemon = new ArrayList<>();

        for (int slotIndex = 0; slotIndex < party.size(); slotIndex++) {
            Pokemon pokemon = party.get(slotIndex);
            if (pokemon == null) {
                continue;
            }

            partyPokemon.add(pokemon);

            String species = pokemon.getSpecies() != null ? pokemon.getSpecies().getName() : "Unknown";
            String form = pokemon.getForm() != null ? pokemon.getForm().getName() : "";
            String heldItem = getHeldItemId(pokemon.heldItem());

            PokemonSnapshot snapshot = new PokemonSnapshot(
                    pokemon.getUuid(),
                    slotIndex,
                    pokemon.getLevel(),
                    species,
                    form,
                    pokemon.getShiny(),
                    heldItem,
                    pokemon.getCurrentHealth(),
                    ""
            );
            pokemonSnapshots.add(snapshot);
        }

        if (pokemonSnapshots.isEmpty()) {
            return new PrepareResult(PrepareResult.Status.EMPTY_PARTY, null);
        }

        long now = System.currentTimeMillis();
        TournamentSnapshot tournamentSnapshot = new TournamentSnapshot(
                playerUuid,
                player.getGameProfile().getName(),
                now,
                now,
                targetLevel,
                "ACTIVE",
                pokemonSnapshots
        );

        try {
            // Save snapshot to disk
            SnapshotStorage.saveSnapshot(server, tournamentSnapshot);
        } catch (IOException e) {
            LOGGER.error("Failed to save tournament snapshot for " + player.getGameProfile().getName(), e);
            return new PrepareResult(PrepareResult.Status.ERROR, null);
        }

        applyLevelToParty(partyPokemon, targetLevel);

        // Heal the entire party
        party.heal();

        LOGGER.info("Prepared team for player {} to level {}", player.getGameProfile().getName(), targetLevel);
        return new PrepareResult(PrepareResult.Status.SUCCESS, tournamentSnapshot);
    }

    /**
     * Restores a player's team to their original levels based on the saved snapshot.
     */
    public static RestoreResult restoreTeam(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return new RestoreResult(RestoreResult.Status.ERROR, Collections.emptyList(), Collections.emptyList());
        }

        UUID playerUuid = player.getUUID();
        TournamentSnapshot snapshot = SnapshotStorage.loadSnapshot(server, playerUuid);
        if (snapshot == null) {
            return new RestoreResult(RestoreResult.Status.NO_SNAPSHOT, Collections.emptyList(), Collections.emptyList());
        }

        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Map<UUID, Pokemon> activePokemonMap = new HashMap<>();
        for (Pokemon pokemon : party) {
            if (pokemon != null) {
                activePokemonMap.put(pokemon.getUuid(), pokemon);
            }
        }

        List<String> restoredNames = new ArrayList<>();
        List<String> missingNames = new ArrayList<>();

        for (PokemonSnapshot pSnap : snapshot.getParty()) {
            Pokemon pokemon = activePokemonMap.get(pSnap.getPokemonUuid());
            String speciesName = pSnap.getSpecies() != null ? pSnap.getSpecies() : "Unknown";
            String displayName = speciesName + " (Slot " + (pSnap.getSlot() + 1) + ")";
            if (pokemon != null) {
                applyLevel(pokemon, pSnap.getOriginalLevel());
                restoredNames.add(displayName);
            } else {
                missingNames.add(displayName);
            }
        }

        // Heal the party
        party.heal();

        // Delete snapshot
        SnapshotStorage.deleteSnapshot(server, playerUuid);

        RestoreResult.Status status = missingNames.isEmpty() ? RestoreResult.Status.SUCCESS : RestoreResult.Status.PARTIAL;
        LOGGER.info("Restored team for player {}. Status: {}, Restored: {}, Missing: {}",
                player.getGameProfile().getName(), status, restoredNames.size(), missingNames.size());

        return new RestoreResult(status, restoredNames, missingNames);
    }

    /**
     * Validates if a player's team complies with tournament rules (e.g. all Pokemon are at the expected level).
     */
    public static ValidateResult validateTeam(ServerPlayer player, int expectedLevel) {
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        List<String> invalidReasons = new ArrayList<>();

        boolean hasAnyPokemon = false;
        for (int slotIndex = 0; slotIndex < party.size(); slotIndex++) {
            Pokemon pokemon = party.get(slotIndex);
            if (pokemon == null) {
                invalidReasons.add(String.format("Slot %d is empty.", slotIndex + 1));
                continue;
            }

            hasAnyPokemon = true;
            if (pokemon.getLevel() != expectedLevel) {
                String speciesName = pokemon.getSpecies() != null ? pokemon.getSpecies().getName() : "Unknown";
                invalidReasons.add(String.format("Slot %d: %s is level %d (expected %d)",
                        slotIndex + 1, speciesName, pokemon.getLevel(), expectedLevel));
            }
        }

        if (!hasAnyPokemon) {
            invalidReasons.add("The party is empty.");
        }

        return new ValidateResult(invalidReasons.isEmpty(), invalidReasons);
    }

    private static void applyLevelToParty(List<Pokemon> partyPokemon, int targetLevel) {
        for (Pokemon pokemon : partyPokemon) {
            applyLevel(pokemon, targetLevel);
        }
    }

    private static void applyLevel(Pokemon pokemon, int level) {
        PokemonProperties properties = new PokemonProperties();
        properties.setLevel(level);
        properties.apply(pokemon);
    }

    private static String getHeldItemId(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
    }
}
