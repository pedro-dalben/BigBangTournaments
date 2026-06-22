package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.PokemonMoveSnapshot;
import com.bigbang_tournaments.model.PokemonSnapshot;
import com.bigbang_tournaments.model.TournamentConfig;
import com.bigbang_tournaments.model.TournamentParticipantRecord;
import com.bigbang_tournaments.model.TournamentRuleViolation;
import com.bigbang_tournaments.model.TournamentSnapshot;
import com.bigbang_tournaments.model.TournamentTeamValidationResult;
import com.bigbang_tournaments.model.TournamentState;
import com.bigbang_tournaments.storage.SnapshotStorage;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.BenchedMove;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PokemonTeamService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokemonTeamService.class);

    private PokemonTeamService() {
    }

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

    public static TournamentSnapshot captureCurrentPartySnapshot(ServerPlayer player, int preparedLevel, boolean rosterLocked) {
        List<PokemonSnapshot> pokemonSnapshots = new ArrayList<>();
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        for (int slotIndex = 0; slotIndex < party.size(); slotIndex++) {
            Pokemon pokemon = party.get(slotIndex);
            if (pokemon == null) {
                continue;
            }
            pokemonSnapshots.add(capturePokemonSnapshot(pokemon, slotIndex));
        }

        long now = System.currentTimeMillis();
        return new TournamentSnapshot(
                2,
                player.getUUID(),
                player.getGameProfile().getName(),
                now,
                now,
                preparedLevel,
                "ACTIVE",
                rosterLocked,
                pokemonSnapshots
        );
    }

    public static PrepareResult prepareTeam(ServerPlayer player, int targetLevel, boolean force) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return new PrepareResult(PrepareResult.Status.ERROR, null);
        }

        if (!force && SnapshotStorage.hasSnapshot(server, player.getUUID())) {
            return new PrepareResult(PrepareResult.Status.ALREADY_HAS_SNAPSHOT, null);
        }

        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        List<Pokemon> partyPokemon = listPartyPokemon(player);
        if (partyPokemon.isEmpty()) {
            return new PrepareResult(PrepareResult.Status.EMPTY_PARTY, null);
        }

        TournamentConfig config = TournamentStateService.getConfig(server);
        TournamentState state = TournamentStateService.getState(server);
        TournamentTeamValidationResult validationResult = TournamentRulesValidator.analyzePlayer(player, targetLevel, config, null, false);
        if (!validationResult.isValid()) {
            LOGGER.warn(
                    "Preparation rejected for {} because the team is not valid: {}",
                    player.getGameProfile().getName(),
                    String.join(" | ", TournamentRulesValidator.toReasonList(validationResult.getViolations()))
            );
            return new PrepareResult(PrepareResult.Status.ERROR, null);
        }

        TournamentSnapshot snapshot = captureCurrentPartySnapshot(player, targetLevel, true);
        try {
            SnapshotStorage.saveSnapshot(server, snapshot);
        } catch (IOException e) {
            LOGGER.error("Failed to save tournament snapshot for {}", player.getGameProfile().getName(), e);
            return new PrepareResult(PrepareResult.Status.ERROR, null);
        }

        try {
            applyLevelToParty(partyPokemon, targetLevel);
            party.heal();

            TournamentParticipantRecord participantRecord = TournamentStateService.getParticipant(server, player.getUUID()).orElse(null);
            if (participantRecord != null) {
                TournamentStateService.applyTeamComposition(
                        server,
                        player.getUUID(),
                        validationResult.getCompositionMode(),
                        validationResult.getJokerPokemonUuid(),
                        validationResult.getJokerSpeciesName()
                );
            }

            LOGGER.info("Prepared team for player {} to level {}", player.getGameProfile().getName(), targetLevel);
            return new PrepareResult(PrepareResult.Status.SUCCESS, snapshot);
        } catch (Exception e) {
            LOGGER.error("Failed to prepare team for {}", player.getGameProfile().getName(), e);
            SnapshotStorage.deleteSnapshot(server, player.getUUID());
            return new PrepareResult(PrepareResult.Status.ERROR, null);
        }
    }

    public static RestoreResult restoreTeam(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return new RestoreResult(RestoreResult.Status.ERROR, Collections.emptyList(), Collections.emptyList());
        }

        TournamentSnapshot snapshot = SnapshotStorage.loadSnapshot(server, player.getUUID());
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

        party.heal();
        SnapshotStorage.deleteSnapshot(server, player.getUUID());

        RestoreResult.Status status = missingNames.isEmpty() ? RestoreResult.Status.SUCCESS : RestoreResult.Status.PARTIAL;
        LOGGER.info("Restored team for player {}. Status: {}, restored: {}, missing: {}",
                player.getGameProfile().getName(), status, restoredNames.size(), missingNames.size());
        return new RestoreResult(status, restoredNames, missingNames);
    }

    public static ValidateResult validateTeam(ServerPlayer player, int expectedLevel) {
        List<TournamentRuleViolation> violations = TournamentRulesValidator.validatePlayer(player, expectedLevel, false);
        return new ValidateResult(violations.isEmpty(), TournamentRulesValidator.toReasonList(violations));
    }

    public static List<String> comparePartyAgainstSnapshot(ServerPlayer player, TournamentSnapshot snapshot) {
        Map<UUID, PokemonSnapshot> expectedByUuid = new LinkedHashMap<>();
        for (PokemonSnapshot pokemonSnapshot : snapshot.getParty()) {
            expectedByUuid.put(pokemonSnapshot.getPokemonUuid(), pokemonSnapshot);
        }

        Map<UUID, Pokemon> currentByUuid = new LinkedHashMap<>();
        for (Pokemon pokemon : listPartyPokemon(player)) {
            currentByUuid.put(pokemon.getUuid(), pokemon);
        }

        List<String> changes = new ArrayList<>();
        for (PokemonSnapshot expected : snapshot.getParty()) {
            Pokemon current = currentByUuid.remove(expected.getPokemonUuid());
            if (current == null) {
                changes.add("Pokemon removido do roster travado: " + expected.getSpecies() + ".");
                continue;
            }

            compareField(changes, expected.getSpecies(), current.getSpecies() != null ? current.getSpecies().getName() : "Unknown",
                    "Species alterada para " + current.getSpecies().getName() + ".");
            compareField(changes, expected.getForm(), current.getForm() != null ? current.getForm().getName() : "",
                    "Forma alterada para " + (current.getForm() != null ? current.getForm().getName() : "") + ".");
            compareField(changes, expected.getHeldItem(), getHeldItemId(current.heldItem()),
                    "Item alterado em " + expected.getSpecies() + ": " + getHeldItemId(current.heldItem()) + ".");
            compareField(changes, expected.getAbility(), current.getAbility() != null ? current.getAbility().getName() : "",
                    "Habilidade alterada em " + expected.getSpecies() + ": " + (current.getAbility() != null ? current.getAbility().getName() : "") + ".");
            compareField(changes, expected.getNature(), current.getNature() != null ? current.getNature().getName().toString() : "",
                    "Nature alterada em " + expected.getSpecies() + ".");
            compareField(changes, expected.getMintedNature(), current.getMintedNature() != null ? current.getMintedNature().getName().toString() : "",
                    "Minted nature alterada em " + expected.getSpecies() + ".");
            compareField(changes, expected.getTeraType(), current.getTeraType() != null ? current.getTeraType().getName() : "",
                    "Tera type alterado em " + expected.getSpecies() + ".");

            if (expected.isGmaxFactor() != current.getGmaxFactor()) {
                changes.add("Gmax factor alterado em " + expected.getSpecies() + ".");
            }
            if (expected.getDynamaxLevel() != current.getDmaxLevel()) {
                changes.add("Dynamax level alterado em " + expected.getSpecies() + ".");
            }
            if (expected.isShiny() != current.getShiny()) {
                changes.add("Shiny flag alterada em " + expected.getSpecies() + ".");
            }

            Set<String> expectedAspects = new LinkedHashSet<>(expected.getAspects());
            Set<String> currentAspects = new LinkedHashSet<>(current.getAspects());
            if (!expectedAspects.equals(currentAspects)) {
                changes.add("Aspects alterados em " + expected.getSpecies() + ": " + currentAspects + ".");
            }

            if (!Objects.equals(expected.getMoveSet(), captureMoveSnapshots(current.getMoveSet()))) {
                changes.add("Moveset alterado em " + expected.getSpecies() + ".");
            }
            if (!Objects.equals(expected.getBenchedMoves(), captureBenchedMoveSnapshots(current))) {
                changes.add("Benched moves alterados em " + expected.getSpecies() + ".");
            }
            if (!Objects.equals(expected.getEvs(), captureEvs(current))) {
                changes.add("EVs alterados em " + expected.getSpecies() + ".");
            }
            if (!Objects.equals(expected.getIvs(), captureIvs(current))) {
                changes.add("IVs alterados em " + expected.getSpecies() + ".");
            }
            if (!Objects.equals(expected.getHyperTrainedIvs(), captureHyperTrainedIvs(current))) {
                changes.add("Hyper trained IVs alterados em " + expected.getSpecies() + ".");
            }
        }

        for (Pokemon unexpected : currentByUuid.values()) {
            String speciesName = unexpected.getSpecies() != null ? unexpected.getSpecies().getName() : "Unknown";
            changes.add("Pokemon adicionado ao roster travado: " + speciesName + ".");
        }

        return changes;
    }

    public static boolean healPlayerTeam(ServerPlayer player) {
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        if (party != null) {
            party.heal();
            return true;
        }
        return false;
    }

    public static String getHeldItemId(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
    }

    public static List<Pokemon> listPartyPokemon(ServerPlayer player) {
        List<Pokemon> partyPokemon = new ArrayList<>();
        PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        for (int slotIndex = 0; slotIndex < party.size(); slotIndex++) {
            Pokemon pokemon = party.get(slotIndex);
            if (pokemon != null) {
                partyPokemon.add(pokemon);
            }
        }
        return partyPokemon;
    }

    private static PokemonSnapshot capturePokemonSnapshot(Pokemon pokemon, int slotIndex) {
        return new PokemonSnapshot(
                pokemon.getUuid(),
                slotIndex,
                pokemon.showdownId(),
                pokemon.getSpecies() != null ? pokemon.getSpecies().getName() : "Unknown",
                pokemon.getSpecies() != null && pokemon.getSpecies().getResourceIdentifier() != null
                        ? pokemon.getSpecies().getResourceIdentifier().toString()
                        : "",
                pokemon.getForm() != null ? pokemon.getForm().getName() : "",
                new ArrayList<>(pokemon.getAspects()),
                pokemon.getLevel(),
                pokemon.getExperience(),
                getHeldItemId(pokemon.heldItem()),
                pokemon.getAbility() != null ? pokemon.getAbility().getName() : "",
                pokemon.getNature() != null ? pokemon.getNature().getName().toString() : "",
                pokemon.getMintedNature() != null ? pokemon.getMintedNature().getName().toString() : "",
                captureMoveSnapshots(pokemon.getMoveSet()),
                captureBenchedMoveSnapshots(pokemon),
                captureEvs(pokemon),
                captureIvs(pokemon),
                captureHyperTrainedIvs(pokemon),
                pokemon.getShiny(),
                pokemon.getFriendship(),
                pokemon.getTeraType() != null ? pokemon.getTeraType().getName() : "",
                pokemon.getGmaxFactor(),
                pokemon.getDmaxLevel(),
                pokemon.getCurrentHealth(),
                pokemon.getStatus() != null && pokemon.getStatus().getStatus() != null
                        ? pokemon.getStatus().getStatus().toString()
                        : "",
                ""
        );
    }

    private static List<PokemonMoveSnapshot> captureMoveSnapshots(Iterable<Move> moveSet) {
        List<PokemonMoveSnapshot> moves = new ArrayList<>();
        for (Move move : moveSet) {
            if (move == null) {
                continue;
            }
            moves.add(new PokemonMoveSnapshot(
                    move.getName(),
                    move.getCurrentPp(),
                    move.getMaxPp(),
                    move.getRaisedPpStages()
            ));
        }
        return moves;
    }

    private static List<PokemonMoveSnapshot> captureBenchedMoveSnapshots(Pokemon pokemon) {
        List<PokemonMoveSnapshot> moves = new ArrayList<>();
        for (BenchedMove benchedMove : pokemon.getBenchedMoves()) {
            if (benchedMove == null || benchedMove.getMoveTemplate() == null) {
                continue;
            }
            int maxPp = benchedMove.getMoveTemplate().getMaxPp();
            moves.add(new PokemonMoveSnapshot(
                    benchedMove.getMoveTemplate().getName(),
                    maxPp,
                    maxPp,
                    benchedMove.getPpRaisedStages()
            ));
        }
        return moves;
    }

    private static Map<String, Integer> captureEvs(Pokemon pokemon) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (Stats stat : Stats.values()) {
            if (stat.getType() == com.cobblemon.mod.common.api.pokemon.stats.Stat.Type.PERMANENT) {
                stats.put(stat.getShowdownId().toLowerCase(Locale.ROOT), pokemon.getEvs().get(stat));
            }
        }
        return stats;
    }

    private static Map<String, Integer> captureIvs(Pokemon pokemon) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (Stats stat : Stats.values()) {
            if (stat.getType() == com.cobblemon.mod.common.api.pokemon.stats.Stat.Type.PERMANENT) {
                stats.put(stat.getShowdownId().toLowerCase(Locale.ROOT), pokemon.getIvs().get(stat));
            }
        }
        return stats;
    }

    private static Map<String, Integer> captureHyperTrainedIvs(Pokemon pokemon) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (Stats stat : Stats.values()) {
            if (stat.getType() == com.cobblemon.mod.common.api.pokemon.stats.Stat.Type.PERMANENT && pokemon.getIvs().isHyperTrained(stat)) {
                stats.put(stat.getShowdownId().toLowerCase(Locale.ROOT), pokemon.getIvs().getEffectiveBattleIV(stat));
            }
        }
        return stats;
    }

    private static void applyLevelToParty(Collection<Pokemon> partyPokemon, int targetLevel) {
        for (Pokemon pokemon : partyPokemon) {
            applyLevel(pokemon, targetLevel);
        }
    }

    private static void applyLevel(Pokemon pokemon, int level) {
        PokemonProperties properties = new PokemonProperties();
        properties.setLevel(level);
        properties.apply(pokemon);
    }

    private static void compareField(List<String> changes, String expected, String current, String message) {
        if (!Objects.equals(normalize(expected), normalize(current))) {
            changes.add(message);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
