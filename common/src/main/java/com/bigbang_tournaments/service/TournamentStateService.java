package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentBattleRecord;
import com.bigbang_tournaments.model.TournamentBattleStatus;
import com.bigbang_tournaments.model.TournamentCheckInStatus;
import com.bigbang_tournaments.model.TournamentConfig;
import com.bigbang_tournaments.model.TournamentParticipantRecord;
import com.bigbang_tournaments.model.TournamentParticipantStatus;
import com.bigbang_tournaments.model.TournamentRuleViolation;
import com.bigbang_tournaments.model.TournamentState;
import com.bigbang_tournaments.storage.TournamentConfigStorage;
import com.bigbang_tournaments.storage.TournamentStateStorage;
import com.bigbang_tournaments.util.TournamentMessages;
import com.bigbang_tournaments.util.PermissionHelper;
import com.cobblemon.mod.common.api.scheduling.ScheduledTask;
import com.cobblemon.mod.common.api.scheduling.ServerRealTimeTaskTracker;
import com.mojang.authlib.GameProfile;
import kotlin.Unit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TournamentStateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TournamentStateService.class);
    private static final Map<MinecraftServer, TournamentState> STATE_CACHE = new HashMap<>();
    private static final Map<MinecraftServer, TournamentConfig> CONFIG_CACHE = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, ScheduledTask>> PENDING_TASKS = new HashMap<>();

    private TournamentStateService() {
    }

    public static TournamentConfig getConfig(MinecraftServer server) {
        return CONFIG_CACHE.computeIfAbsent(server, TournamentConfigStorage::loadOrCreate);
    }

    public static TournamentState getState(MinecraftServer server) {
        return STATE_CACHE.computeIfAbsent(server, TournamentStateStorage::loadOrCreate);
    }

    public static int getAdminPermissionLevel(MinecraftServer server) {
        return getConfig(server).getAdminPermissionLevel();
    }

    public static void saveState(MinecraftServer server) {
        TournamentStateStorage.save(server, getState(server));
    }

    public static void saveConfig(MinecraftServer server) {
        TournamentConfigStorage.save(server, getConfig(server));
    }

    public static boolean requiresElementTournamentType(String tournamentType) {
        return TournamentModeRegistry.resolve(tournamentType).requiresElementAssignment();
    }

    public static boolean isCheckInOpen(TournamentState state) {
        return state != null && "CHECK_IN".equals(state.getTournamentPhase());
    }

    public static boolean isReady(TournamentState state) {
        return state != null && "READY".equals(state.getTournamentPhase());
    }

    public static String normalizeTournamentType(String tournamentType) {
        if (tournamentType == null || tournamentType.trim().isEmpty()) {
            return "standard";
        }
        String lower = tournamentType.toLowerCase().trim();
        if ("singletype".equals(lower) || "singleelement".equals(lower) || "monotype".equals(lower)) {
            return "singletype";
        }
        if ("doubles".equals(lower) || "2v2".equals(lower) || "duplas".equals(lower)) {
            return "doubles";
        }
        if ("regulation_i_doubles".equals(lower) || "regulation_i".equals(lower) || "vgc_doubles".equals(lower) || "vgc_reg_i".equals(lower)) {
            return "regulation_i_doubles";
        }
        if ("standard".equals(lower)) {
            return "standard";
        }
        return tournamentType;
    }

    public static TournamentParticipantRecord upsertParticipant(MinecraftServer server, GameProfile profile) {
        TournamentState state = getState(server);
        TournamentParticipantRecord record = getParticipant(server, profile.getId()).orElseGet(() -> {
            TournamentParticipantRecord created = new TournamentParticipantRecord(profile.getId(), profile.getName());
            state.getParticipants().add(created);
            return created;
        });
        record.setPlayerName(profile.getName());

        boolean needsElement = requiresElementTournamentType(state.getTournamentType());
        if (needsElement && record.getAssignedElement() == null) {
            String assignedElement = chooseElementForNewParticipant(state);
            record.setAssignedElement(assignedElement);
            record.setRollsUsed(0);
        }

        record.setUpdatedAt(System.currentTimeMillis());
        saveState(server);
        return record;
    }

    public static synchronized String assignElementToExistingParticipant(MinecraftServer server, TournamentParticipantRecord record) {
        TournamentState state = getState(server);
        boolean needsElement = requiresElementTournamentType(state.getTournamentType());
        if (needsElement && record.getAssignedElement() == null) {
            String assignedElement = chooseElementForNewParticipant(state);
            record.setAssignedElement(assignedElement);
            record.setRollsUsed(0);
            saveState(server);
            return assignedElement;
        }
        return record.getAssignedElement();
    }

    public static TournamentParticipantRecord upsertParticipant(ServerPlayer player) {
        return upsertParticipant(Objects.requireNonNull(player.getServer()), player.getGameProfile());
    }

    public static Optional<TournamentParticipantRecord> getParticipant(MinecraftServer server, UUID playerUuid) {
        return getState(server).getParticipants().stream()
                .filter(record -> playerUuid.equals(record.getPlayerUuid()))
                .findFirst();
    }

    public static Optional<TournamentParticipantRecord> getParticipantByName(MinecraftServer server, String playerName) {
        return getState(server).getParticipants().stream()
                .filter(record -> record.getPlayerName() != null && record.getPlayerName().equalsIgnoreCase(playerName))
                .findFirst();
    }

    public static boolean removeParticipant(MinecraftServer server, UUID playerUuid) {
        boolean removed = getState(server).getParticipants().removeIf(record -> playerUuid.equals(record.getPlayerUuid()));
        if (removed) {
            cancelPendingValidation(server, playerUuid);
            saveState(server);
            if (isCheckInOpen(getState(server))) {
                checkIfAllCheckedIn(server);
            }
        }
        return removed;
    }

    public static List<TournamentParticipantRecord> listParticipants(MinecraftServer server) {
        return new ArrayList<>(getState(server).getParticipants());
    }

    public static boolean isPrepared(MinecraftServer server, UUID playerUuid) {
        return getParticipant(server, playerUuid).map(TournamentParticipantRecord::isPrepared).orElse(false);
    }

    public static boolean isRosterLocked(MinecraftServer server, UUID playerUuid) {
        return getParticipant(server, playerUuid).map(TournamentParticipantRecord::isRosterLocked).orElse(false);
    }

    public static boolean isPartyLockedForTournament(MinecraftServer server, UUID playerUuid) {
        if (isRosterLocked(server, playerUuid)) {
            return true;
        }
        return TournamentBattleService.isPlayerInBattleSession(playerUuid);
    }

    public static void markPending(MinecraftServer server, ServerPlayer player, int level, List<TournamentRuleViolation> violations) {
        TournamentParticipantRecord record = upsertParticipant(player);
        long now = System.currentTimeMillis();
        record.setPrepared(false);
        record.setRosterLocked(false);
        record.setPendingValidation(true);
        record.setPreparedLevel(level);
        record.setStatus(TournamentParticipantStatus.PENDING_VALIDATION);
        clearTeamComposition(record);
        record.setPendingSince(now);
        record.setUpdatedAt(now);
        record.setLastViolations(new ArrayList<>(violations));
        saveState(server);
        schedulePendingValidation(server, player.getUUID(), level);
    }

    public static void markPrepared(MinecraftServer server, ServerPlayer player, int level) {
        TournamentParticipantRecord record = upsertParticipant(player);
        long now = System.currentTimeMillis();
        record.setPrepared(true);
        record.setRosterLocked(true);
        record.setPendingValidation(false);
        record.setPreparedLevel(level);
        record.setStatus(TournamentParticipantStatus.PREPARED);
        record.setPendingSince(0L);
        record.setNextValidationAt(0L);
        record.setUpdatedAt(now);
        record.setLastViolations(new ArrayList<>());
        cancelPendingValidation(server, player.getUUID());
        saveState(server);
    }

    public static void unlock(MinecraftServer server, UUID playerUuid) {
        getParticipant(server, playerUuid).ifPresent(record -> {
            record.setPrepared(false);
            record.setRosterLocked(false);
            record.setPendingValidation(false);
            record.setStatus(TournamentParticipantStatus.UNLOCKED);
            clearTeamComposition(record);
            record.setPendingSince(0L);
            record.setNextValidationAt(0L);
            record.setUpdatedAt(System.currentTimeMillis());
            record.setLastViolations(new ArrayList<>());
            cancelPendingValidation(server, playerUuid);
            saveState(server);
        });
    }

    public static void markRestored(MinecraftServer server, UUID playerUuid) {
        getParticipant(server, playerUuid).ifPresent(record -> {
            record.setPrepared(false);
            record.setRosterLocked(false);
            record.setPendingValidation(false);
            record.setStatus(TournamentParticipantStatus.RESTORED);
            clearTeamComposition(record);
            record.setPendingSince(0L);
            record.setNextValidationAt(0L);
            record.setUpdatedAt(System.currentTimeMillis());
            cancelPendingValidation(server, playerUuid);
            saveState(server);
        });
    }

    public static void markCheckInAwaiting(MinecraftServer server, UUID playerUuid) {
        getParticipant(server, playerUuid).ifPresent(record -> {
            record.setCheckInStatus(TournamentCheckInStatus.AWAITING);
            record.setCheckedInAt(0L);
            record.setUpdatedAt(System.currentTimeMillis());
            saveState(server);
        });
    }

    public static void markCheckInConfirmed(MinecraftServer server, UUID playerUuid) {
        getParticipant(server, playerUuid).ifPresent(record -> {
            record.setCheckInStatus(TournamentCheckInStatus.CHECKED_IN);
            record.setCheckedInAt(System.currentTimeMillis());
            record.setUpdatedAt(System.currentTimeMillis());
            saveState(server);
        });
    }

    public static void markCheckInAbsent(MinecraftServer server, UUID playerUuid) {
        getParticipant(server, playerUuid).ifPresent(record -> {
            record.setCheckInStatus(TournamentCheckInStatus.ABSENT);
            record.setUpdatedAt(System.currentTimeMillis());
            saveState(server);
        });
    }

    public static void resetCheckInState(MinecraftServer server, UUID playerUuid) {
        getParticipant(server, playerUuid).ifPresent(record -> {
            record.setCheckInStatus(TournamentCheckInStatus.NOT_STARTED);
            record.setCheckedInAt(0L);
            record.setUpdatedAt(System.currentTimeMillis());
            saveState(server);
        });
    }

    public static void applyTeamComposition(MinecraftServer server, UUID playerUuid, String compositionMode, UUID jokerPokemonUuid, String jokerSpeciesName) {
        getParticipant(server, playerUuid).ifPresent(record -> {
            record.setTeamCompositionMode(compositionMode);
            record.setJokerPokemonUuid(jokerPokemonUuid);
            record.setJokerSpeciesName(jokerSpeciesName);
            record.setUpdatedAt(System.currentTimeMillis());
            saveState(server);
        });
    }

    public static void clearTeamComposition(TournamentParticipantRecord record) {
        record.setTeamCompositionMode(null);
        record.setJokerPokemonUuid(null);
        record.setJokerSpeciesName(null);
    }

    public static boolean areAllParticipantsPrepared(MinecraftServer server) {
        List<TournamentParticipantRecord> participants = getState(server).getParticipants();
        return !participants.isEmpty() && participants.stream().allMatch(TournamentParticipantRecord::isPrepared);
    }

    public static void setActiveBattle(MinecraftServer server, TournamentBattleRecord battleRecord) {
        battleRecord.setUpdatedAt(System.currentTimeMillis());
        getState(server).setActiveBattle(battleRecord);
        saveState(server);
    }

    public static TournamentBattleRecord getActiveBattle(MinecraftServer server) {
        return getState(server).getActiveBattle();
    }

    public static void archiveActiveBattle(MinecraftServer server) {
        TournamentState state = getState(server);
        TournamentBattleRecord activeBattle = state.getActiveBattle();
        if (activeBattle != null) {
            activeBattle.setUpdatedAt(System.currentTimeMillis());
            state.getBattleHistory().add(activeBattle);
            state.setActiveBattle(null);
            saveState(server);
        }
    }

    public static void startupRecovery(MinecraftServer server) {
        TournamentState state = getState(server);
        long now = System.currentTimeMillis();

        TournamentBattleRecord activeBattle = state.getActiveBattle();
        if (activeBattle != null && activeBattle.getStatus() != TournamentBattleStatus.FINISHED) {
            activeBattle.setInterruptedByRestart(true);
            activeBattle.setStatus(TournamentBattleStatus.INTERRUPTED);
            activeBattle.setUpdatedAt(now);
            LOGGER.warn("Tournament active battle marked as interrupted after restart");
            saveState(server);
        }

        for (TournamentParticipantRecord participant : state.getParticipants()) {
            if (!participant.isPendingValidation()) {
                continue;
            }

            long nextValidationAt = participant.getNextValidationAt();
            float delaySeconds = nextValidationAt > now ? (nextValidationAt - now) / 1000F : 1F;
            schedulePendingValidation(server, participant.getPlayerUuid(), participant.getPreparedLevel(), delaySeconds);
        }
        handleCheckInRecovery(server);
    }

    public static void shutdown(MinecraftServer server) {
        Map<UUID, ScheduledTask> scheduledTasks = PENDING_TASKS.remove(server);
        if (scheduledTasks != null) {
            scheduledTasks.values().forEach(ScheduledTask::expire);
        }
        cancelCheckInTasks(server);
        STATE_CACHE.remove(server);
        CONFIG_CACHE.remove(server);
    }

    public static void schedulePendingValidation(MinecraftServer server, UUID playerUuid, int level) {
        float delaySeconds = getConfig(server).getCorrectionWindowSeconds();
        schedulePendingValidation(server, playerUuid, level, delaySeconds);
    }

    private static void schedulePendingValidation(MinecraftServer server, UUID playerUuid, int level, float delaySeconds) {
        cancelPendingValidation(server, playerUuid);
        Map<UUID, ScheduledTask> serverTasks = PENDING_TASKS.computeIfAbsent(server, ignored -> new HashMap<>());
        ScheduledTask task = ServerRealTimeTaskTracker.INSTANCE.after(delaySeconds, () -> {
            handlePendingValidation(server, playerUuid, level);
            return Unit.INSTANCE;
        });
        serverTasks.put(playerUuid, task);
        getParticipant(server, playerUuid).ifPresent(record -> {
            record.setNextValidationAt(System.currentTimeMillis() + Math.round(delaySeconds * 1000F));
            record.setUpdatedAt(System.currentTimeMillis());
            saveState(server);
        });
    }

    public static void cancelPendingValidation(MinecraftServer server, UUID playerUuid) {
        Map<UUID, ScheduledTask> serverTasks = PENDING_TASKS.get(server);
        if (serverTasks == null) {
            return;
        }
        ScheduledTask task = serverTasks.remove(playerUuid);
        if (task != null) {
            task.expire();
        }
    }

    private static void handlePendingValidation(MinecraftServer server, UUID playerUuid, int level) {
        Optional<TournamentParticipantRecord> optionalRecord = getParticipant(server, playerUuid);
        if (optionalRecord.isEmpty() || !optionalRecord.get().isPendingValidation()) {
            cancelPendingValidation(server, playerUuid);
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player == null) {
            LOGGER.info("Skipping automatic revalidation for {} because player is offline", playerUuid);
            schedulePendingValidation(server, playerUuid, level);
            return;
        }

        List<TournamentRuleViolation> violations = TournamentRulesValidator.validatePlayer(player, level, false, false);
        if (!violations.isEmpty()) {
            optionalRecord.get().setLastViolations(new ArrayList<>(violations));
            optionalRecord.get().setUpdatedAt(System.currentTimeMillis());
            saveState(server);
            TournamentMessages.broadcastInvalidTeam(server, player.getGameProfile().getName());
            TournamentMessages.sendInvalidTeam(player, TournamentRulesValidator.toReasonList(violations));
            schedulePendingValidation(server, playerUuid, level);
            return;
        }

        PokemonTeamService.PrepareResult prepareResult = PokemonTeamService.prepareTeam(player, level, true);
        if (prepareResult.getStatus() == PokemonTeamService.PrepareResult.Status.SUCCESS) {
            markPrepared(server, player, level);
            TournamentMessages.broadcastPrepared(server, player.getGameProfile().getName());
            if (areAllParticipantsPrepared(server)) {
                TournamentMessages.broadcastAllPrepared(server);
            }
            return;
        }

        LOGGER.warn("Automatic prepare failed for {}", player.getGameProfile().getName());
        schedulePendingValidation(server, playerUuid, level);
    }

    public static final List<String> ELEMENTS = List.of(
        "💧 Água", "🔥 Fogo", "🌿 Planta", "⚡ Elétrico", "⚙️ Aço", "👻 Fantasma",
        "🧚 Fada", "🐉 Dragão", "☠️ Venenoso", "🧠 Psíquico", "🥊 Lutador", "🌑 Sombrio", "🌎 Terra"
    );

    public static boolean isSameElement(String element1, String element2) {
        if (element1 == null || element2 == null) {
            return false;
        }
        return cleanElement(element1).equals(cleanElement(element2));
    }

    public static String getCanonicalElement(String element) {
        if (element == null) {
            return null;
        }
        for (String el : ELEMENTS) {
            if (isSameElement(el, element)) {
                return el;
            }
        }
        return element;
    }

    private static String cleanElement(String element) {
        if (element == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(element, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    public static synchronized String registerPlayer(MinecraftServer server, ServerPlayer player) {
        TournamentState state = getState(server);
        if (state.getTournamentName() == null || state.getTournamentName().trim().isEmpty()) {
            throw new IllegalStateException("Nao ha nenhum campeonato agendado no momento.");
        }

        UUID uuid = player.getUUID();
        Optional<TournamentParticipantRecord> existing = getParticipant(server, uuid);

        boolean needsElement = requiresElementTournamentType(state.getTournamentType());

        if (existing.isPresent()) {
            throw new IllegalArgumentException("Voce ja esta participando desse campeonato.");
        }

        TournamentParticipantRecord record = new TournamentParticipantRecord(uuid, player.getGameProfile().getName());
        String assignedElement = null;

        if (needsElement) {
            assignedElement = chooseElementForNewParticipant(state);
            record.setAssignedElement(assignedElement);
            record.setRollsUsed(0);
        }

        if (isCheckInOpen(state)) {
            record.setCheckInStatus(TournamentCheckInStatus.AWAITING);
        }

        state.getParticipants().add(record);
        saveState(server);

        return assignedElement;
    }

    private static String chooseElementForNewParticipant(TournamentState state) {
        boolean isSingleType = "singletype".equals(TournamentModeRegistry.resolve(state.getTournamentType()).id());
        Map<String, Integer> counts = new HashMap<>();
        for (String element : ELEMENTS) {
            counts.put(element, 0);
        }
        for (TournamentParticipantRecord participant : state.getParticipants()) {
            String element = getCanonicalElement(participant.getAssignedElement());
            if (element != null && counts.containsKey(element)) {
                counts.put(element, counts.get(element) + 1);
            }
        }

        if (isSingleType) {
            List<String> candidates = new ArrayList<>();
            for (String element : ELEMENTS) {
                if (counts.get(element) == 0) {
                    candidates.add(element);
                }
            }
            if (candidates.isEmpty()) {
                throw new IllegalStateException("Nao ha mais tipos disponiveis para este campeonato. Limite de 13 participantes atingido.");
            }
            int index = (int) (Math.random() * candidates.size());
            return candidates.get(index);
        }

        int min = counts.values().stream().min(Integer::compare).orElse(0);
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == min) {
                candidates.add(entry.getKey());
            }
        }

        int index = (int) (Math.random() * candidates.size());
        return candidates.get(index);
    }

    public static synchronized String rerollElement(MinecraftServer server, ServerPlayer player) {
        TournamentState state = getState(server);
        if (state.getTournamentName() == null || state.getTournamentName().trim().isEmpty()) {
            throw new IllegalStateException("Nao ha nenhum campeonato ativo no momento.");
        }
        if (!requiresElementTournamentType(state.getTournamentType())) {
            throw new IllegalStateException("Este comando so esta disponivel para campeonatos do tipo singleelement ou monotype.");
        }

        UUID uuid = player.getUUID();
        TournamentParticipantRecord record = getParticipant(server, uuid)
                .orElseThrow(() -> new IllegalArgumentException("Voce nao esta inscrito no campeonato."));

        TournamentConfig config = getConfig(server);
        int maxRolls = PermissionHelper.getMaxRolls(player, config.getDefaultRerolls());
        if (record.getRollsUsed() >= maxRolls) {
            throw new IllegalStateException("Voce ja atingiu o limite de sortear novamente (" + record.getRollsUsed() + "/" + maxRolls + ").");
        }

        String oldElement = getCanonicalElement(record.getAssignedElement());

        Map<String, Integer> counts = new HashMap<>();
        for (String element : ELEMENTS) {
            counts.put(element, 0);
        }
        for (TournamentParticipantRecord participant : state.getParticipants()) {
            if (participant.getPlayerUuid().equals(uuid)) {
                continue;
            }
            String element = getCanonicalElement(participant.getAssignedElement());
            if (element != null && counts.containsKey(element)) {
                counts.put(element, counts.get(element) + 1);
            }
        }

        boolean isSingleType = "singletype".equals(TournamentModeRegistry.resolve(state.getTournamentType()).id());
        List<String> candidates = new ArrayList<>();
        if (isSingleType) {
            for (String element : ELEMENTS) {
                if (counts.get(element) == 0 && !isSameElement(element, oldElement)) {
                    candidates.add(element);
                }
            }
            if (candidates.isEmpty()) {
                throw new IllegalStateException("Nao ha outros tipos disponiveis para sortear.");
            }
        } else {
            int min = counts.values().stream().min(Integer::compare).orElse(0);
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() == min && !isSameElement(entry.getKey(), oldElement)) {
                    candidates.add(entry.getKey());
                }
            }

            if (candidates.isEmpty()) {
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() == min + 1 && !isSameElement(entry.getKey(), oldElement)) {
                        candidates.add(entry.getKey());
                    }
                }
            }

            if (candidates.isEmpty()) {
                for (String element : ELEMENTS) {
                    if (!isSameElement(element, oldElement)) {
                        candidates.add(element);
                    }
                }
            }
        }

        int index = (int) (Math.random() * candidates.size());
        String newElement = candidates.get(index);
        if (isSameElement(newElement, oldElement)) {
            throw new IllegalStateException("Nao foi possivel sortear um elemento diferente do atual. Tente novamente.");
        }

        record.setAssignedElement(newElement);
        record.setRollsUsed(record.getRollsUsed() + 1);
        saveState(server);

        return newElement;
    }

    private static final Map<MinecraftServer, List<ScheduledTask>> CHECK_IN_TASKS = new HashMap<>();

    public static void cancelCheckInTasks(MinecraftServer server) {
        List<ScheduledTask> tasks = CHECK_IN_TASKS.remove(server);
        if (tasks != null) {
            for (ScheduledTask task : tasks) {
                task.expire();
            }
        }
    }

    public static void scheduleCheckInTasks(MinecraftServer server, long deadlineMs) {
        cancelCheckInTasks(server);
        List<ScheduledTask> tasks = new ArrayList<>();
        CHECK_IN_TASKS.put(server, tasks);

        long now = System.currentTimeMillis();
        long totalRemainingSec = (deadlineMs - now) / 1000L;

        if (totalRemainingSec > 180) {
            long delay = totalRemainingSec - 180;
            ScheduledTask task = ServerRealTimeTaskTracker.INSTANCE.after(delay, () -> {
                broadcastCountdown(server, 3, true);
                return Unit.INSTANCE;
            });
            tasks.add(task);
        }
        if (totalRemainingSec > 60) {
            long delay = totalRemainingSec - 60;
            ScheduledTask task = ServerRealTimeTaskTracker.INSTANCE.after(delay, () -> {
                broadcastCountdown(server, 1, true);
                return Unit.INSTANCE;
            });
            tasks.add(task);
        }
        if (totalRemainingSec > 30) {
            long delay = totalRemainingSec - 30;
            ScheduledTask task = ServerRealTimeTaskTracker.INSTANCE.after(delay, () -> {
                broadcastCountdown(server, 30, false);
                return Unit.INSTANCE;
            });
            tasks.add(task);
        }
        if (totalRemainingSec > 10) {
            long delay = totalRemainingSec - 10;
            ScheduledTask task = ServerRealTimeTaskTracker.INSTANCE.after(delay, () -> {
                broadcastCountdown(server, 10, false);
                return Unit.INSTANCE;
            });
            tasks.add(task);
        }

        long finalDelay = Math.max(1, totalRemainingSec);
        ScheduledTask finalTask = ServerRealTimeTaskTracker.INSTANCE.after(finalDelay, () -> {
            endCheckIn(server);
            return Unit.INSTANCE;
        });
        tasks.add(finalTask);
    }

    public static void broadcastCountdown(MinecraftServer server, int value, boolean isMinutes) {
        TournamentState state = getState(server);
        if (!"CHECK_IN".equals(state.getTournamentPhase())) {
            return;
        }

        List<String> pendingNames = new ArrayList<>();
        for (TournamentParticipantRecord part : state.getParticipants()) {
            if (part.getCheckInStatus() == TournamentCheckInStatus.AWAITING || part.getCheckInStatus() == TournamentCheckInStatus.NOT_STARTED) {
                pendingNames.add(part.getPlayerName());
            }
        }

        if (pendingNames.isEmpty()) {
            return;
        }

        String pendingListStr = pendingNames.stream().map(name -> "- " + name).collect(java.util.stream.Collectors.joining("\n"));

        net.minecraft.network.chat.Component countdownComponent;
        if (isMinutes) {
            if (value == 1) {
                countdownComponent = TournamentMessages.translatable("commands.tournament.checkin.countdown.minute", pendingListStr);
            } else {
                countdownComponent = TournamentMessages.translatable("commands.tournament.checkin.countdown.minutes", value, pendingListStr);
            }
        } else {
            countdownComponent = TournamentMessages.translatable("commands.tournament.checkin.countdown.seconds", value, pendingListStr);
        }

        server.getPlayerList().broadcastSystemMessage(countdownComponent, false);
    }

    public static void endCheckIn(MinecraftServer server) {
        TournamentState state = getState(server);
        if (!"CHECK_IN".equals(state.getTournamentPhase())) {
            return;
        }

        state.setTournamentPhase("READY");

        List<TournamentParticipantRecord> present = new ArrayList<>();
        List<TournamentParticipantRecord> absent = new ArrayList<>();

        for (TournamentParticipantRecord part : state.getParticipants()) {
            if (part.getCheckInStatus() == TournamentCheckInStatus.CHECKED_IN) {
                present.add(part);
            } else if (part.getCheckInStatus() == TournamentCheckInStatus.AWAITING || part.getCheckInStatus() == TournamentCheckInStatus.NOT_STARTED) {
                part.setCheckInStatus(TournamentCheckInStatus.ABSENT);
                absent.add(part);
            } else if (part.getCheckInStatus() == TournamentCheckInStatus.ABSENT) {
                absent.add(part);
            } else {
                part.setCheckInStatus(TournamentCheckInStatus.CHECKED_IN);
                present.add(part);
            }
        }

        cancelCheckInTasks(server);
        saveState(server);

        boolean isSingleType = "singletype".equals(TournamentModeRegistry.resolve(state.getTournamentType()).id());

        net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal(TournamentMessages.resolve("commands.tournament.checkin.ended.header"));

        for (TournamentParticipantRecord p : present) {
            if (isSingleType) {
                message.append("\n").append(TournamentMessages.plain("commands.tournament.checkin.ended.present.singletype", p.getPlayerName(), p.getAssignedElement()));
            } else {
                message.append("\n").append(TournamentMessages.plain("commands.tournament.checkin.ended.present.standard", p.getPlayerName()));
            }
        }

        message.append(TournamentMessages.plain("commands.tournament.checkin.ended.absent_header"));

        for (TournamentParticipantRecord a : absent) {
            message.append("\n").append(TournamentMessages.plain("commands.tournament.checkin.ended.absent.item", a.getPlayerName()));
        }

        message.append(TournamentMessages.plain("commands.tournament.checkin.ended.footer"));

        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    public static void checkIfAllCheckedIn(MinecraftServer server) {
        TournamentState state = getState(server);
        if (!"CHECK_IN".equals(state.getTournamentPhase())) {
            return;
        }
        boolean allConfirmed = true;
        for (TournamentParticipantRecord part : state.getParticipants()) {
            if (part.getCheckInStatus() == TournamentCheckInStatus.AWAITING || part.getCheckInStatus() == TournamentCheckInStatus.NOT_STARTED) {
                allConfirmed = false;
                break;
            }
        }

        if (allConfirmed) {
            cancelCheckInTasks(server);
            state.setTournamentPhase("READY");
            saveState(server);

            server.getPlayerList().broadcastSystemMessage(TournamentMessages.translatable("commands.tournament.checkin.all_confirmed"), false);
        }
    }

    public static void handleCheckInRecovery(MinecraftServer server) {
        TournamentState state = getState(server);
        if (!"CHECK_IN".equals(state.getTournamentPhase())) {
            return;
        }
        long now = System.currentTimeMillis();
        long deadline = state.getCheckInDeadline();
        if (deadline <= now) {
            endCheckIn(server);
        } else {
            scheduleCheckInTasks(server, deadline);
        }
    }
}
