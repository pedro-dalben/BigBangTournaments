package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentBattleRecord;
import com.bigbang_tournaments.model.TournamentBattleStatus;
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

    public static TournamentParticipantRecord upsertParticipant(MinecraftServer server, GameProfile profile) {
        TournamentState state = getState(server);
        TournamentParticipantRecord record = getParticipant(server, profile.getId()).orElseGet(() -> {
            TournamentParticipantRecord created = new TournamentParticipantRecord(profile.getId(), profile.getName());
            state.getParticipants().add(created);
            return created;
        });
        record.setPlayerName(profile.getName());
        record.setUpdatedAt(System.currentTimeMillis());
        saveState(server);
        return record;
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

    public static void markPending(MinecraftServer server, ServerPlayer player, int level, List<TournamentRuleViolation> violations) {
        TournamentParticipantRecord record = upsertParticipant(player);
        long now = System.currentTimeMillis();
        record.setPrepared(false);
        record.setRosterLocked(false);
        record.setPendingValidation(true);
        record.setPreparedLevel(level);
        record.setStatus(TournamentParticipantStatus.PENDING_VALIDATION);
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
            record.setPendingSince(0L);
            record.setNextValidationAt(0L);
            record.setUpdatedAt(System.currentTimeMillis());
            cancelPendingValidation(server, playerUuid);
            saveState(server);
        });
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
    }

    public static void shutdown(MinecraftServer server) {
        Map<UUID, ScheduledTask> scheduledTasks = PENDING_TASKS.remove(server);
        if (scheduledTasks != null) {
            scheduledTasks.values().forEach(ScheduledTask::expire);
        }
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

    public static synchronized String registerPlayer(MinecraftServer server, ServerPlayer player) {
        TournamentState state = getState(server);
        if (state.getTournamentName() == null || state.getTournamentName().trim().isEmpty()) {
            throw new IllegalStateException("Nao ha nenhum campeonato agendado no momento.");
        }

        UUID uuid = player.getUUID();
        if (getParticipant(server, uuid).isPresent()) {
            throw new IllegalArgumentException("Voce ja esta inscrito neste campeonato.");
        }

        TournamentParticipantRecord record = new TournamentParticipantRecord(uuid, player.getGameProfile().getName());
        String assignedElement = null;

        if ("singleelement".equalsIgnoreCase(state.getTournamentType())) {
            assignedElement = chooseElementForNewParticipant(state);
            record.setAssignedElement(assignedElement);
            record.setRollsUsed(0);
        }

        state.getParticipants().add(record);
        saveState(server);

        return assignedElement;
    }

    private static String chooseElementForNewParticipant(TournamentState state) {
        Map<String, Integer> counts = new HashMap<>();
        for (String element : ELEMENTS) {
            counts.put(element, 0);
        }
        for (TournamentParticipantRecord participant : state.getParticipants()) {
            if (participant.getAssignedElement() != null && counts.containsKey(participant.getAssignedElement())) {
                counts.put(participant.getAssignedElement(), counts.get(participant.getAssignedElement()) + 1);
            }
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
        if (!"singleelement".equalsIgnoreCase(state.getTournamentType())) {
            throw new IllegalStateException("Este comando so esta disponivel para campeonatos do tipo singleelement.");
        }

        UUID uuid = player.getUUID();
        TournamentParticipantRecord record = getParticipant(server, uuid)
                .orElseThrow(() -> new IllegalArgumentException("Voce nao esta inscrito no campeonato."));

        TournamentConfig config = getConfig(server);
        int maxRolls = PermissionHelper.getMaxRolls(player, config.getDefaultRerolls());
        if (record.getRollsUsed() >= maxRolls) {
            throw new IllegalStateException("Voce ja atingiu o limite de sortear novamente (" + record.getRollsUsed() + "/" + maxRolls + ").");
        }

        String oldElement = record.getAssignedElement() != null ? record.getAssignedElement() : "";
        
        Map<String, Integer> counts = new HashMap<>();
        for (String element : ELEMENTS) {
            counts.put(element, 0);
        }
        for (TournamentParticipantRecord participant : state.getParticipants()) {
            if (participant.getPlayerUuid().equals(uuid)) {
                continue;
            }
            if (participant.getAssignedElement() != null && counts.containsKey(participant.getAssignedElement())) {
                counts.put(participant.getAssignedElement(), counts.get(participant.getAssignedElement()) + 1);
            }
        }

        int min = counts.values().stream().min(Integer::compare).orElse(0);
        
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == min && !entry.getKey().equals(oldElement)) {
                candidates.add(entry.getKey());
            }
        }

        if (candidates.isEmpty()) {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (entry.getValue() == min + 1 && !entry.getKey().equals(oldElement)) {
                    candidates.add(entry.getKey());
                }
            }
        }

        if (candidates.isEmpty()) {
            for (String element : ELEMENTS) {
                if (!element.equals(oldElement)) {
                    candidates.add(element);
                }
            }
        }

        int index = (int) (Math.random() * candidates.size());
        String newElement = candidates.get(index);

        record.setAssignedElement(newElement);
        record.setRollsUsed(record.getRollsUsed() + 1);
        saveState(server);

        return newElement;
    }
}
