package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.*;
import com.bigbang_tournaments.storage.TournamentBattleSessionStorage;
import com.bigbang_tournaments.util.TournamentMessages;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.SuccessfulBattleStart;
import com.cobblemon.mod.common.net.serverhandling.battle.SpectateBattleHandler;
import kotlin.Unit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TournamentBattleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TournamentBattleService.class);
    private static final Map<UUID, AreaLock> AREA_LOCKS = new HashMap<>();
    private static final ConcurrentMap<UUID, TimerTaskInfo> PENDING_TIMERS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, UUID> ACTIVE_SESSION_BY_PLAYER = new ConcurrentHashMap<>();

    private TournamentBattleService() {
    }

    private static class TimerTaskInfo {
        final UUID sessionId;
        final com.cobblemon.mod.common.api.scheduling.ScheduledTask task;

        TimerTaskInfo(UUID sessionId, com.cobblemon.mod.common.api.scheduling.ScheduledTask task) {
            this.sessionId = sessionId;
            this.task = task;
        }
    }

    public static void setArenaPos1(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        arena.setPos1(capturePosition(player));
        TournamentStateService.saveState(server);
        TournamentMessages.send(player, "Posicao 1 da arena definida.");
    }

    public static void setArenaPos2(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        arena.setPos2(capturePosition(player));
        TournamentStateService.saveState(server);
        TournamentMessages.send(player, "Posicao 2 da arena definida.");
    }

    public static void setArenaSpectator(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        arena.setSpectator(capturePosition(player));
        TournamentStateService.saveState(server);
        TournamentMessages.send(player, "Arquibancada definida.");
    }

    public static String arenaInfo(MinecraftServer server) {
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        return "pos1=" + formatPosition(arena.getPos1()) + ", pos2=" + formatPosition(arena.getPos2()) + ", spectator=" + formatPosition(arena.getSpectator());
    }

    public static int startBattle(CommandSourceStack source, ServerPlayer player1, ServerPlayer player2) {
        MinecraftServer server = source.getServer();
        TournamentArenaState arena = TournamentStateService.getState(server).getArena();
        if (!arena.isComplete()) {
            TournamentMessages.sendArenaIncomplete(source);
            return 0;
        }

        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle != null && activeBattle.getStatus() != TournamentBattleStatus.FINISHED) {
            TournamentMessages.sendFailure(source, "Ja existe uma partida ativa ou pendente de resolucao.");
            return 0;
        }

        if (ACTIVE_SESSION_BY_PLAYER.containsKey(player1.getUUID()) || ACTIVE_SESSION_BY_PLAYER.containsKey(player2.getUUID())) {
            TournamentMessages.sendFailure(source, "Um dos jogadores ja esta em uma sessao de batalha de torneio.");
            return 0;
        }

        Optional<TournamentParticipantRecord> record1 = TournamentStateService.getParticipant(server, player1.getUUID());
        Optional<TournamentParticipantRecord> record2 = TournamentStateService.getParticipant(server, player2.getUUID());
        if (record1.isEmpty() || !record1.get().isPrepared()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player1.getGameProfile().getName() + " ainda nao esta preparado.");
            return 0;
        }
        if (record2.isEmpty() || !record2.get().isPrepared()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player2.getGameProfile().getName() + " ainda nao esta preparado.");
            return 0;
        }
        if (!record1.get().isRosterLocked()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player1.getGameProfile().getName() + " nao esta com o roster travado.");
            return 0;
        }
        if (!record2.get().isRosterLocked()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player2.getGameProfile().getName() + " nao esta com o roster travado.");
            return 0;
        }

        List<TournamentRuleViolation> player1Violations = TournamentRulesValidator.validatePlayer(player1, record1.get().getPreparedLevel(), true);
        if (!player1Violations.isEmpty()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player1.getGameProfile().getName() + " esta com o time invalido: " + String.join("; ", TournamentRulesValidator.toReasonList(player1Violations)) + ".");
            return 0;
        }

        List<TournamentRuleViolation> player2Violations = TournamentRulesValidator.validatePlayer(player2, record2.get().getPreparedLevel(), true);
        if (!player2Violations.isEmpty()) {
            TournamentMessages.sendFailure(source, "A batalha nao pode comecar. " + player2.getGameProfile().getName() + " esta com o time invalido: " + String.join("; ", TournamentRulesValidator.toReasonList(player2Violations)) + ".");
            return 0;
        }

        PokemonTeamService.healPlayerTeam(player1);
        PokemonTeamService.healPlayerTeam(player2);
        teleportToPosition(player1, arena.getPos1());
        teleportToPosition(player2, arena.getPos2());
        enableAreaLock(server, player1, arena.getPos1());
        enableAreaLock(server, player2, arena.getPos2());

        TournamentBattleRecord battleRecord = new TournamentBattleRecord();
        battleRecord.setPlayer1Uuid(player1.getUUID());
        battleRecord.setPlayer1Name(player1.getGameProfile().getName());
        battleRecord.setPlayer2Uuid(player2.getUUID());
        battleRecord.setPlayer2Name(player2.getGameProfile().getName());
        battleRecord.setCreatedAt(System.currentTimeMillis());
        battleRecord.setUpdatedAt(System.currentTimeMillis());

        TournamentState state = TournamentStateService.getState(server);
        String modeId = state != null ? TournamentModeRegistry.resolve(state.getTournamentType()).id() : "standard";
        boolean isDoubles = "doubles".equals(modeId) || "regulation_i_doubles".equals(modeId);

        if (isDoubles) {
            TournamentBattleSession session = new TournamentBattleSession(
                    player1.getUUID(), player1.getGameProfile().getName(),
                    player2.getUUID(), player2.getGameProfile().getName(),
                    modeId
            );

            try {
                session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
                TeamPreviewConfig previewConfig = TournamentStateService.getConfig(server).getTeamPreview();
                long expiresAt = System.currentTimeMillis() + (previewConfig.getDurationSeconds() * 1000L);
                session.setPreviewExpiresAt(expiresAt);
                TournamentBattleSessionStorage.saveSession(server, session);

                ACTIVE_SESSION_BY_PLAYER.put(player1.getUUID(), session.getSessionId());
                ACTIVE_SESSION_BY_PLAYER.put(player2.getUUID(), session.getSessionId());

                battleRecord.setStatus(TournamentBattleStatus.TEAM_PREVIEW);
                TournamentStateService.setActiveBattle(server, battleRecord);
                TournamentMessages.broadcastNextBattle(server, player1.getGameProfile().getName(), player2.getGameProfile().getName());

                startTeamPreviewPhase(server, session, player1, player2, previewConfig);
            } catch (Exception e) {
                LOGGER.error("Failed to start doubles team preview", e);
                ACTIVE_SESSION_BY_PLAYER.remove(player1.getUUID());
                ACTIVE_SESSION_BY_PLAYER.remove(player2.getUUID());
                TournamentBattleSessionStorage.deleteSession(server, session.getSessionId());
                releaseAreaLocks(server, battleRecord);
                TournamentStateService.archiveActiveBattle(server);
                TournamentMessages.sendFailure(source, "Erro ao iniciar o Team Preview.");
                return 0;
            }
        } else {
            battleRecord.setStatus(TournamentBattleStatus.COUNTDOWN);
            TournamentStateService.setActiveBattle(server, battleRecord);
            TournamentMessages.broadcastNextBattle(server, player1.getGameProfile().getName(), player2.getGameProfile().getName());
            TournamentMessages.broadcastBattleCountdown(server, player1.getGameProfile().getName(), player2.getGameProfile().getName(), 10);
            scheduleCountdown(server, battleRecord, player1, player2, 5);
            scheduleCountdown(server, battleRecord, player1, player2, 3);
            scheduleCountdown(server, battleRecord, player1, player2, 2);
            scheduleCountdown(server, battleRecord, player1, player2, 1);
            com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(10F, () -> {
                beginCobblemonBattle(server, player1, player2);
                return Unit.INSTANCE;
            });
        }
        return 1;
    }

    public static int registerManualWin(CommandSourceStack source, ServerPlayer winner) {
        MinecraftServer server = source.getServer();
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null) {
            TournamentMessages.sendFailure(source, "Nao ha batalha ativa para registrar resultado.");
            return 0;
        }

        UUID winnerUuid = winner.getUUID();
        if (!winnerUuid.equals(activeBattle.getPlayer1Uuid()) && !winnerUuid.equals(activeBattle.getPlayer2Uuid())) {
            TournamentMessages.sendFailure(source, "O jogador informado nao participa da batalha ativa.");
            return 0;
        }

        UUID sessionId = ACTIVE_SESSION_BY_PLAYER.get(winnerUuid);
        if (sessionId != null) {
            TournamentBattleFinalizationService.safeFinalize(server, sessionId, "manual_win");
            ACTIVE_SESSION_BY_PLAYER.remove(activeBattle.getPlayer1Uuid());
            ACTIVE_SESSION_BY_PLAYER.remove(activeBattle.getPlayer2Uuid());
        }

        activeBattle.setWinnerUuid(winnerUuid);
        activeBattle.setWinnerName(winner.getGameProfile().getName());
        boolean isPlayer1 = winnerUuid.equals(activeBattle.getPlayer1Uuid());
        activeBattle.setLoserUuid(isPlayer1 ? activeBattle.getPlayer2Uuid() : activeBattle.getPlayer1Uuid());
        activeBattle.setLoserName(isPlayer1 ? activeBattle.getPlayer2Name() : activeBattle.getPlayer1Name());
        activeBattle.setManualResult(true);
        activeBattle.setStatus(TournamentBattleStatus.FINISHED);
        activeBattle.setUpdatedAt(System.currentTimeMillis());
        releaseAreaLocks(server, activeBattle);
        sendPlayersToSpectatorArea(server, activeBattle);
        TournamentMessages.broadcastManualWin(server, activeBattle.getWinnerName());
        TournamentStateService.archiveActiveBattle(server);
        return 1;
    }

    public static void spectate(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        TournamentPosition spectatorPos = TournamentStateService.getState(server).getArena().getSpectator();
        if (spectatorPos == null) {
            TournamentMessages.sendSpectatorMissing(player);
            return;
        }

        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        boolean shouldTeleport = activeBattle == null
                || activeBattle.getStatus() != TournamentBattleStatus.ACTIVE
                || !isWithinSpectatorRange(player, spectatorPos, server);

        if (shouldTeleport) {
            teleportToPosition(player, spectatorPos);
            TournamentMessages.sendSpectatorTeleported(player);
        }

        if (activeBattle == null || activeBattle.getStatus() != TournamentBattleStatus.ACTIVE) {
            return;
        }

        ServerPlayer target = server.getPlayerList().getPlayer(activeBattle.getPlayer1Uuid());
        if (target == null || target.getUUID().equals(player.getUUID())) {
            target = server.getPlayerList().getPlayer(activeBattle.getPlayer2Uuid());
        }
        if (target != null && !target.getUUID().equals(player.getUUID())) {
            SpectateBattleHandler.INSTANCE.spectateBattle(target, player);
        }
    }

    public static void handlePlayerTick(ServerPlayer player) {
        AreaLock areaLock = AREA_LOCKS.get(player.getUUID());
        if (areaLock == null) {
            return;
        }

        if (!player.level().dimension().location().toString().equals(areaLock.position().getDimension())) {
            teleportToPosition(player, areaLock.position());
            return;
        }

        double dx = player.getX() - areaLock.position().getX();
        double dy = player.getY() - areaLock.position().getY();
        double dz = player.getZ() - areaLock.position().getZ();
        double maxDistanceSq = areaLock.radius() * areaLock.radius();
        if ((dx * dx) + (dy * dy) + (dz * dz) > maxDistanceSq) {
            teleportToPosition(player, areaLock.position());
            long now = System.currentTimeMillis();
            if (now - areaLock.lastWarningAt() >= 2000L) {
                TournamentMessages.sendArenaBoundary(player);
                AREA_LOCKS.put(player.getUUID(), new AreaLock(areaLock.position(), areaLock.radius(), now));
            }
        }
    }

    public static void handleBattleStarted(PokemonBattle battle) {
        if (!battle.isPvP()) {
            return;
        }
        MinecraftServer server = battle.getPlayers().isEmpty() ? null : battle.getPlayers().get(0).getServer();
        if (server == null) {
            return;
        }
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null) {
            return;
        }
        UUID sessionId = findSessionForPlayers(server, battle.getPlayerUUIDs());
        if (sessionId == null) {
            if (containsPlayers(activeBattle, battle.getPlayerUUIDs())) {
                activeBattle.setBattleId(battle.getBattleId().toString());
                activeBattle.setStatus(TournamentBattleStatus.ACTIVE);
                activeBattle.setUpdatedAt(System.currentTimeMillis());
                TournamentStateService.setActiveBattle(server, activeBattle);
            }
            return;
        }

        TournamentBattleSession session = TournamentBattleSessionStorage.loadSession(server, sessionId);
        if (session != null) {
            session.setBattleId(battle.getBattleId().toString());
            session.setCobblemonBattleReference(battle.getBattleId().toString());
            session.transitionTo(TournamentBattleStatus.ACTIVE);
            try {
                TournamentBattleSessionStorage.saveSession(server, session);
            } catch (IOException e) {
                LOGGER.error("Failed to save session after battle started", e);
            }
        }

        if (containsPlayers(activeBattle, battle.getPlayerUUIDs())) {
            activeBattle.setBattleId(battle.getBattleId().toString());
            activeBattle.setStatus(TournamentBattleStatus.ACTIVE);
            activeBattle.setUpdatedAt(System.currentTimeMillis());
            TournamentStateService.setActiveBattle(server, activeBattle);
        }
    }

    public static void handleBattleVictory(BattleVictoryEvent event) {
        PokemonBattle battle = event.getBattle();
        MinecraftServer server = battle.getPlayers().isEmpty() ? null : battle.getPlayers().get(0).getServer();
        if (server == null) {
            return;
        }

        UUID sessionId = findSessionForPlayers(server, battle.getPlayerUUIDs());
        if (sessionId != null) {
            TournamentBattleFinalizationService.safeFinalize(server, sessionId, "battle_victory");
            for (UUID uuid : battle.getPlayerUUIDs()) {
                ACTIVE_SESSION_BY_PLAYER.remove(uuid);
            }
        }

        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null || !containsPlayers(activeBattle, battle.getPlayerUUIDs())) {
            return;
        }

        UUID winnerUuid = null;
        UUID loserUuid = null;
        for (var winner : event.getWinners()) {
            winnerUuid = winner.getUuid();
            break;
        }
        for (var loser : event.getLosers()) {
            loserUuid = loser.getUuid();
            break;
        }

        if (winnerUuid == null || loserUuid == null) {
            activeBattle.setStatus(TournamentBattleStatus.MANUAL_RESULT_REQUIRED);
            activeBattle.setUpdatedAt(System.currentTimeMillis());
            TournamentStateService.setActiveBattle(server, activeBattle);
            releaseAreaLocks(server, activeBattle);
            TournamentMessages.broadcast(server, "A batalha entre " + activeBattle.getPlayer1Name() + " e " + activeBattle.getPlayer2Name() + " terminou, mas o vencedor nao pode ser detectado automaticamente. Staff deve registrar o resultado manualmente.");
            return;
        }

        activeBattle.setWinnerUuid(winnerUuid);
        activeBattle.setLoserUuid(loserUuid);
        activeBattle.setWinnerName(winnerUuid.equals(activeBattle.getPlayer1Uuid()) ? activeBattle.getPlayer1Name() : activeBattle.getPlayer2Name());
        activeBattle.setLoserName(loserUuid.equals(activeBattle.getPlayer1Uuid()) ? activeBattle.getPlayer1Name() : activeBattle.getPlayer2Name());
        activeBattle.setBattleId(battle.getBattleId().toString());
        activeBattle.setStatus(TournamentBattleStatus.FINISHED);
        activeBattle.setUpdatedAt(System.currentTimeMillis());
        battle.saveBattleLog();
        releaseAreaLocks(server, activeBattle);
        sendPlayersToSpectatorArea(server, activeBattle);
        TournamentMessages.broadcastBattleWin(server, activeBattle.getWinnerName(), activeBattle.getLoserName());
        TournamentStateService.archiveActiveBattle(server);
    }

    public static void handleBattleFled(BattleFledEvent event) {
        ServerPlayer player = event.getPlayer().getEntity();
        if (player == null || player.getServer() == null) {
            return;
        }
        UUID sessionId = ACTIVE_SESSION_BY_PLAYER.get(player.getUUID());
        if (sessionId != null) {
            TournamentBattleFinalizationService.safeFinalize(player.getServer(), sessionId, "fled");
            ACTIVE_SESSION_BY_PLAYER.remove(player.getUUID());
            UUID opponentUuid = findOpponentInSession(player.getServer(), sessionId, player.getUUID());
            if (opponentUuid != null) {
                ACTIVE_SESSION_BY_PLAYER.remove(opponentUuid);
            }
        }
        markManualResultRequired(player.getServer(), player.getUUID(), "fuga");
    }

    public static void handleDisconnect(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        UUID sessionId = ACTIVE_SESSION_BY_PLAYER.get(player.getUUID());
        if (sessionId != null) {
            TournamentBattleFinalizationService.safeFinalize(server, sessionId, "player_disconnected");
            ACTIVE_SESSION_BY_PLAYER.remove(player.getUUID());
        }

        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null) {
            return;
        }
        if (!player.getUUID().equals(activeBattle.getPlayer1Uuid()) && !player.getUUID().equals(activeBattle.getPlayer2Uuid())) {
            return;
        }

        UUID opponentUuid = player.getUUID().equals(activeBattle.getPlayer1Uuid()) ? activeBattle.getPlayer2Uuid() : activeBattle.getPlayer1Uuid();
        String opponentName = player.getUUID().equals(activeBattle.getPlayer1Uuid()) ? activeBattle.getPlayer2Name() : activeBattle.getPlayer1Name();
        activeBattle.setStatus(TournamentBattleStatus.MANUAL_RESULT_REQUIRED);
        activeBattle.setUpdatedAt(System.currentTimeMillis());
        TournamentStateService.setActiveBattle(server, activeBattle);
        releaseAreaLocks(server, activeBattle);
        TournamentMessages.broadcastDisconnect(server, player.getGameProfile().getName(), opponentName);
        LOGGER.warn("Player {} disconnected during tournament battle against {}", player.getUUID(), opponentUuid);
    }

    public static void handleRestartRecovery(MinecraftServer server) {
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle != null && activeBattle.isInterruptedByRestart()) {
            TournamentMessages.broadcastInterruptedRestart(server);
        }

        List<TournamentBattleSession> activeSessions = TournamentBattleSessionStorage.listActiveSessions(server);
        for (TournamentBattleSession session : activeSessions) {
            LOGGER.info("Found active session {} in state {} during restart recovery", session.getSessionId(), session.getState());
            if (session.getState().isPartyModified()) {
                ServerPlayer p1 = server.getPlayerList().getPlayer(session.getPlayerOneUuid());
                ServerPlayer p2 = server.getPlayerList().getPlayer(session.getPlayerTwoUuid());
                if (p1 != null && p2 != null) {
                    TournamentBattleFinalizationService.safeFinalize(server, session.getSessionId(), "restart_recovery");
                } else {
                    session.setState(TournamentBattleStatus.RESTORE_PENDING);
                    session.setFinalizationReason("restart_recovery_offline");
                    try {
                        TournamentBattleSessionStorage.saveSession(server, session);
                    } catch (IOException e) {
                        LOGGER.error("Failed to save session during recovery", e);
                    }
                }
            } else {
                session.setState(TournamentBattleStatus.CANCELLED);
                session.setFinalizationReason("restart_recovery");
                try {
                    TournamentBattleSessionStorage.saveSession(server, session);
                    TournamentBattleSessionStorage.deleteSession(server, session.getSessionId());
                } catch (IOException e) {
                    LOGGER.error("Failed to clean up session during recovery", e);
                }
            }
        }

        restoreAllOriginalPartiesFromDisk(server);
    }

    public static void handleLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        restorePlayerOriginalParty(server, player.getUUID());

        List<TournamentBattleSession> activeSessions = TournamentBattleSessionStorage.listActiveSessions(server);
        for (TournamentBattleSession session : activeSessions) {
            if (player.getUUID().equals(session.getPlayerOneUuid()) || player.getUUID().equals(session.getPlayerTwoUuid())) {
                if (session.getState() == TournamentBattleStatus.RESTORE_PENDING
                        || session.getState() == TournamentBattleStatus.RESTORING
                        || session.getState() == TournamentBattleStatus.FAILED) {

                    UUID otherUuid = player.getUUID().equals(session.getPlayerOneUuid())
                            ? session.getPlayerTwoUuid() : session.getPlayerOneUuid();
                    ServerPlayer otherPlayer = server.getPlayerList().getPlayer(otherUuid);

                    if (otherPlayer != null || session.getState() == TournamentBattleStatus.FAILED) {
                        TournamentBattleFinalizationService.safeFinalize(server, session.getSessionId(), "login_recovery");
                        ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerOneUuid());
                        ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerTwoUuid());
                    }
                }
            }
        }
    }

    public static boolean isPlayerInBattleSession(UUID playerUuid) {
        return ACTIVE_SESSION_BY_PLAYER.containsKey(playerUuid);
    }

    public static Set<UUID> getPlayersInBattleSessions() {
        return ACTIVE_SESSION_BY_PLAYER.keySet();
    }

    private static void scheduleCountdown(MinecraftServer server, TournamentBattleRecord activeBattle, ServerPlayer player1, ServerPlayer player2, int secondsLeft) {
        float delay = 10F - secondsLeft;
        com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(delay, () -> {
            TournamentBattleRecord current = TournamentStateService.getActiveBattle(server);
            if (current != null && current.getStatus() == TournamentBattleStatus.COUNTDOWN
                    && current.getPlayer1Uuid().equals(activeBattle.getPlayer1Uuid())
                    && current.getPlayer2Uuid().equals(activeBattle.getPlayer2Uuid())) {
                TournamentMessages.broadcastBattleCountdown(server, player1.getGameProfile().getName(), player2.getGameProfile().getName(), secondsLeft);
            }
            return Unit.INSTANCE;
        });
    }

    private static void beginCobblemonBattle(MinecraftServer server, ServerPlayer player1, ServerPlayer player2) {
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null || activeBattle.getStatus() != TournamentBattleStatus.COUNTDOWN) {
            return;
        }

        TournamentState state = TournamentStateService.getState(server);
        com.cobblemon.mod.common.battles.BattleFormat format = com.cobblemon.mod.common.battles.BattleFormat.Companion.getGEN_9_SINGLES();
        if (state != null) {
            String modeId = TournamentModeRegistry.resolve(state.getTournamentType()).id();
            if ("doubles".equals(modeId) || "regulation_i_doubles".equals(modeId)) {
                format = com.cobblemon.mod.common.battles.BattleFormat.Companion.getGEN_9_DOUBLES();
            }
        }

        BattleStartResult result = BattleBuilder.INSTANCE.pvp1v1(player1, player2, null, null, format, false, false);
        if (result instanceof SuccessfulBattleStart successfulBattleStart) {
            PokemonBattle battle = successfulBattleStart.getBattle();
            activeBattle.setBattleId(battle.getBattleId().toString());
            activeBattle.setStatus(TournamentBattleStatus.ACTIVE);
            activeBattle.setUpdatedAt(System.currentTimeMillis());
            TournamentStateService.setActiveBattle(server, activeBattle);
            TournamentMessages.broadcastBattleStarted(server, player1.getGameProfile().getName(), player2.getGameProfile().getName());
            return;
        }

        LOGGER.warn("Cobblemon PvP battle start failed between {} and {}", player1.getGameProfile().getName(), player2.getGameProfile().getName());

        UUID sessionId = ACTIVE_SESSION_BY_PLAYER.get(player1.getUUID());
        if (sessionId != null) {
            TournamentBattleFinalizationService.safeFinalize(server, sessionId, "battle_start_failed");
            ACTIVE_SESSION_BY_PLAYER.remove(player1.getUUID());
            ACTIVE_SESSION_BY_PLAYER.remove(player2.getUUID());
        }

        releaseAreaLocks(server, activeBattle);
        TournamentStateService.archiveActiveBattle(server);
    }

    private static void markManualResultRequired(MinecraftServer server, UUID participantUuid, String reason) {
        TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
        if (activeBattle == null) {
            return;
        }
        if (!participantUuid.equals(activeBattle.getPlayer1Uuid()) && !participantUuid.equals(activeBattle.getPlayer2Uuid())) {
            return;
        }
        activeBattle.setStatus(TournamentBattleStatus.MANUAL_RESULT_REQUIRED);
        activeBattle.setUpdatedAt(System.currentTimeMillis());
        TournamentStateService.setActiveBattle(server, activeBattle);
        releaseAreaLocks(server, activeBattle);
        LOGGER.warn("Tournament battle marked for manual result because of {}", reason);
    }

    private static TournamentPosition capturePosition(ServerPlayer player) {
        return new TournamentPosition(
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    private static String formatPosition(TournamentPosition position) {
        if (position == null) {
            return "unset";
        }
        return position.getDimension() + "@" + position.getX() + "," + position.getY() + "," + position.getZ();
    }

    private static void teleportToPosition(ServerPlayer player, TournamentPosition position) {
        if (player.getServer() == null || position == null) {
            return;
        }
        ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(position.getDimension())));
        if (level == null) {
            return;
        }
        player.teleportTo(level, position.getX(), position.getY(), position.getZ(), position.getYaw(), position.getPitch());
    }

    private static boolean isWithinSpectatorRange(ServerPlayer player, TournamentPosition spectatorPos, MinecraftServer server) {
        if (spectatorPos == null || player.getServer() == null) {
            return false;
        }

        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(spectatorPos.getDimension())));
        if (level == null || !player.level().dimension().location().equals(level.dimension().location())) {
            return false;
        }

        double dx = player.getX() - spectatorPos.getX();
        double dy = player.getY() - spectatorPos.getY();
        double dz = player.getZ() - spectatorPos.getZ();
        double maxDistance = Math.max(24.0D, TournamentStateService.getConfig(server).getArenaRadius() * 6.0D);
        return (dx * dx) + (dy * dy) + (dz * dz) <= maxDistance * maxDistance;
    }

    private static void enableAreaLock(MinecraftServer server, ServerPlayer player, TournamentPosition center) {
        AREA_LOCKS.put(player.getUUID(), new AreaLock(center, TournamentStateService.getConfig(server).getArenaRadius(), 0L));
    }

    private static void releaseAreaLocks(MinecraftServer server, TournamentBattleRecord battleRecord) {
        AREA_LOCKS.remove(battleRecord.getPlayer1Uuid());
        AREA_LOCKS.remove(battleRecord.getPlayer2Uuid());
    }

    private static void sendPlayersToSpectatorArea(MinecraftServer server, TournamentBattleRecord battleRecord) {
        TournamentPosition spectatorPos = TournamentStateService.getState(server).getArena().getSpectator();
        if (spectatorPos == null) {
            return;
        }

        com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(1F, () -> {
            ServerPlayer player1 = server.getPlayerList().getPlayer(battleRecord.getPlayer1Uuid());
            ServerPlayer player2 = server.getPlayerList().getPlayer(battleRecord.getPlayer2Uuid());
            if (player1 != null) {
                teleportToPosition(player1, spectatorPos);
            }
            if (player2 != null) {
                teleportToPosition(player2, spectatorPos);
            }
            return Unit.INSTANCE;
        });
    }

    private static boolean containsPlayers(TournamentBattleRecord activeBattle, Iterable<UUID> playerUuids) {
        boolean hasPlayer1 = false;
        boolean hasPlayer2 = false;
        for (UUID playerUuid : playerUuids) {
            if (playerUuid.equals(activeBattle.getPlayer1Uuid())) {
                hasPlayer1 = true;
            }
            if (playerUuid.equals(activeBattle.getPlayer2Uuid())) {
                hasPlayer2 = true;
            }
        }
        return hasPlayer1 && hasPlayer2;
    }

    private static UUID findSessionForPlayers(MinecraftServer server, Iterable<UUID> playerUuids) {
        List<UUID> uuids = new ArrayList<>();
        for (UUID uuid : playerUuids) uuids.add(uuid);
        if (uuids.size() < 2) return null;

        UUID id1 = ACTIVE_SESSION_BY_PLAYER.get(uuids.get(0));
        UUID id2 = ACTIVE_SESSION_BY_PLAYER.get(uuids.get(1));
        if (id1 != null && id1.equals(id2)) {
            TournamentBattleSession session = TournamentBattleSessionStorage.loadSession(server, id1);
            if (session != null) return id1;
        }
        return null;
    }

    private static UUID findOpponentInSession(MinecraftServer server, UUID sessionId, UUID playerUuid) {
        TournamentBattleSession session = TournamentBattleSessionStorage.loadSession(server, sessionId);
        if (session == null) return null;
        if (playerUuid.equals(session.getPlayerOneUuid())) return session.getPlayerTwoUuid();
        if (playerUuid.equals(session.getPlayerTwoUuid())) return session.getPlayerOneUuid();
        return null;
    }

    private static void startTeamPreviewPhase(MinecraftServer server, TournamentBattleSession session,
                                               ServerPlayer player1, ServerPlayer player2,
                                               TeamPreviewConfig previewConfig) {
        sendTeamPreview(server, session, player1, player2, previewConfig);
        sendTeamPreview(server, session, player2, player1, previewConfig);
        com.bigbang_tournaments.menu.TeamPreviewMenu.open(player1, player2, session, previewConfig);
        com.bigbang_tournaments.menu.TeamPreviewMenu.open(player2, player1, session, previewConfig);
        scheduleTeamPreviewTimeout(server, session, previewConfig.getDurationSeconds());
    }

    private static void sendTeamPreview(MinecraftServer server, TournamentBattleSession session,
                                        ServerPlayer viewer, ServerPlayer owner,
                                        TeamPreviewConfig config) {
        List<Pokemon> team = PokemonTeamService.listPartyPokemon(owner);

        viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l========================================="));
        viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e§lTEAM PREVIEW - Time de " + owner.getGameProfile().getName() + ":"));

        int idx = 1;
        for (Pokemon p : team) {
            StringBuilder line = new StringBuilder();
            line.append("§a").append(idx).append(". ");

            if (config.isRevealSpecies()) {
                String speciesName = p.getSpecies() != null ? p.getSpecies().getName() : "Unknown";
                line.append("§f").append(speciesName);
            } else {
                line.append("§f???");
            }

            if (config.isRevealHeldItems()) {
                net.minecraft.world.item.ItemStack heldItem = p.heldItem();
                String heldItemId = PokemonTeamService.getHeldItemId(heldItem);
                if (!heldItemId.isBlank()) {
                    String itemDisplay = heldItemId.contains(":") ? heldItemId.split(":")[1] : heldItemId;
                    itemDisplay = itemDisplay.replace("_", " ");
                    line.append(" §7[Item: ").append(capitalizeWord(itemDisplay)).append("]");
                }
            }

            if (config.isRevealAbilities()) {
                String abilityStr = p.getAbility() != null ? p.getAbility().getName() : "";
                if (!abilityStr.isEmpty()) {
                    line.append(" §7(Habilidade: ").append(abilityStr).append(")");
                }
            }

            viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal(line.toString()));
            idx++;
        }

        viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l-----------------------------------------"));
        viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eEscolha exatamente 4 Pok\u00e9mon para usar na batalha!"));
        viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eUse: §6/tournament select <1-6> <1-6> <1-6> <1-6>"));

        net.minecraft.network.chat.MutableComponent helperMsg = net.minecraft.network.chat.Component.literal("§d[Clique aqui para sugerir o comando de selecao]");
        helperMsg.setStyle(helperMsg.getStyle()
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/tournament select 1 2 3 4"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("Sugere o comando /tournament select 1 2 3 4"))));
        viewer.sendSystemMessage(helperMsg);

        net.minecraft.network.chat.MutableComponent guiMsg = net.minecraft.network.chat.Component.literal("§a§l[CLIQUE AQUI PARA ABRIR O MENU VISUAL DE SELEÇÃO]");
        guiMsg.setStyle(guiMsg.getStyle()
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/tournament menu"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.literal("Abre o menu visual de escolha de Pokemons"))));
        viewer.sendSystemMessage(guiMsg);

        viewer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l========================================="));
    }

    private static String capitalizeWord(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static int selectTeam(ServerPlayer player, int slot1, int slot2, int slot3, int slot4) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }

        UUID playerUuid = player.getUUID();
        UUID sessionId = ACTIVE_SESSION_BY_PLAYER.get(playerUuid);
        if (sessionId == null) {
            TournamentMessages.send(player, "Voce nao esta em uma fase de selecao de time ativa.");
            return 0;
        }

        TournamentBattleSession session = TournamentBattleSessionStorage.loadSession(server, sessionId);
        if (session == null) {
            TournamentMessages.send(player, "Sessao de batalha nao encontrada.");
            ACTIVE_SESSION_BY_PLAYER.remove(playerUuid);
            return 0;
        }

        if (!session.getState().isPreviewActive()) {
            TournamentMessages.send(player, "Nao ha uma fase de selecao de time ativa no momento.");
            return 0;
        }

        if (System.currentTimeMillis() > session.getPreviewExpiresAt()) {
            TournamentMessages.send(player, "O tempo de selecao ja esgotou.");
            return 0;
        }

        boolean isPlayerOne = playerUuid.equals(session.getPlayerOneUuid());
        boolean alreadySelected = isPlayerOne
                ? (session.getPlayerOneSelection() != null && !session.getPlayerOneSelection().isEmpty())
                : (session.getPlayerTwoSelection() != null && !session.getPlayerTwoSelection().isEmpty());
        if (alreadySelected) {
            TournamentMessages.send(player, "Voce ja confirmou sua selecao de time.");
            return 0;
        }

        List<Pokemon> party = PokemonTeamService.listPartyPokemon(player);
        Set<Integer> slots = new LinkedHashSet<>(List.of(slot1, slot2, slot3, slot4));
        if (slots.size() != 4) {
            TournamentMessages.send(player, "Voce deve selecionar exatamente 4 Pokemon distintos.");
            return 0;
        }

        for (int slot : slots) {
            if (slot < 1 || slot > party.size()) {
                TournamentMessages.send(player, "Slot invalido: " + slot + ". Seu time tem " + party.size() + " Pokemon.");
                return 0;
            }
        }

        List<String> pokemonIdentities = new ArrayList<>();
        for (int slot : slots) {
            Pokemon p = party.get(slot - 1);
            pokemonIdentities.add(p.getUuid().toString() + "|" + (p.getSpecies() != null ? p.getSpecies().getName() : "Unknown"));
        }

        List<Integer> selection = new ArrayList<>(slots);
        if (isPlayerOne) {
            session.setPlayerOneSelection(selection);
            session.setPlayerOnePokemonIdentities(pokemonIdentities);
            session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED);
        } else {
            session.setPlayerTwoSelection(selection);
            session.setPlayerTwoPokemonIdentities(pokemonIdentities);
            session.transitionTo(TournamentBattleStatus.PLAYER_TWO_SELECTED);
        }

        try {
            TournamentBattleSessionStorage.saveSession(server, session);
        } catch (IOException e) {
            LOGGER.error("Failed to save session after selection", e);
            TournamentMessages.send(player, "Erro ao salvar selecao. Tente novamente.");
            return 0;
        }

        TournamentMessages.send(player, "Time selecionado com sucesso! Aguardando oponente...");

        UUID opponentUuid = isPlayerOne ? session.getPlayerTwoUuid() : session.getPlayerOneUuid();
        ServerPlayer opponent = server.getPlayerList().getPlayer(opponentUuid);
        if (opponent != null) {
            TournamentMessages.send(opponent, player.getGameProfile().getName() + " confirmou a selecao do time.");
        }

        if (session.bothSelected()) {
            applySelectionsAndStartBattle(server, session);
        }

        return 1;
    }

    public static int openTeamPreviewMenu(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }

        UUID playerUuid = player.getUUID();
        UUID sessionId = ACTIVE_SESSION_BY_PLAYER.get(playerUuid);
        if (sessionId == null) {
            TournamentMessages.send(player, "Voce nao esta em uma fase de selecao de time ativa.");
            return 0;
        }

        TournamentBattleSession session = TournamentBattleSessionStorage.loadSession(server, sessionId);
        if (session == null || !session.getState().isPreviewActive()) {
            TournamentMessages.send(player, "Nao ha uma fase de selecao de time ativa no momento.");
            return 0;
        }

        boolean isPlayerOne = playerUuid.equals(session.getPlayerOneUuid());
        UUID opponentUuid = isPlayerOne ? session.getPlayerTwoUuid() : session.getPlayerOneUuid();
        ServerPlayer opponent = server.getPlayerList().getPlayer(opponentUuid);
        if (opponent == null) {
            TournamentMessages.send(player, "O seu oponente esta offline no momento.");
            return 0;
        }

        TeamPreviewConfig config = TournamentStateService.getConfig(server).getTeamPreview();
        com.bigbang_tournaments.menu.TeamPreviewMenu.open(player, opponent, session, config);
        return 1;
    }

    private static void scheduleTeamPreviewTimeout(MinecraftServer server, TournamentBattleSession session, int durationSeconds) {
        UUID sessionId = session.getSessionId();

        PENDING_TIMERS.values().removeIf(info -> info.sessionId.equals(sessionId));

        com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after((float) durationSeconds, () -> {
            TimerTaskInfo current = PENDING_TIMERS.get(sessionId);
            if (current == null) {
                return Unit.INSTANCE;
            }

            TournamentBattleSession loaded = TournamentBattleSessionStorage.loadSession(server, sessionId);
            if (loaded == null || loaded.getState().isTerminal()) {
                PENDING_TIMERS.remove(sessionId);
                return Unit.INSTANCE;
            }

            if (!loaded.getState().isPreviewActive()) {
                PENDING_TIMERS.remove(sessionId);
                return Unit.INSTANCE;
            }

            TeamPreviewConfig previewConfig = TournamentStateService.getConfig(server).getTeamPreview();
            boolean p1Unselected = loaded.getPlayerOneSelection() == null || loaded.getPlayerOneSelection().isEmpty();
            boolean p2Unselected = loaded.getPlayerTwoSelection() == null || loaded.getPlayerTwoSelection().isEmpty();

            if (p1Unselected) {
                List<Integer> autoSelect = "FIRST_FOUR".equals(previewConfig.getAutoSelectStrategy())
                        ? List.of(1, 2, 3, 4) : List.of(1, 2, 3, 4);
                loaded.setPlayerOneSelection(autoSelect);
                ServerPlayer p1 = server.getPlayerList().getPlayer(loaded.getPlayerOneUuid());
                if (p1 != null) {
                    TournamentMessages.send(p1, "Tempo esgotado! Seus primeiros 4 Pokemon foram selecionados automaticamente.");
                }
            }
            if (p2Unselected) {
                List<Integer> autoSelect = "FIRST_FOUR".equals(previewConfig.getAutoSelectStrategy())
                        ? List.of(1, 2, 3, 4) : List.of(1, 2, 3, 4);
                loaded.setPlayerTwoSelection(autoSelect);
                ServerPlayer p2 = server.getPlayerList().getPlayer(loaded.getPlayerTwoUuid());
                if (p2 != null) {
                    TournamentMessages.send(p2, "Tempo esgotado! Seus primeiros 4 Pokemon foram selecionados automaticamente.");
                }
            }

            PENDING_TIMERS.remove(sessionId);
            applySelectionsAndStartBattle(server, loaded);
            return Unit.INSTANCE;
        });

        PENDING_TIMERS.put(sessionId, new TimerTaskInfo(sessionId,
                com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after((float) durationSeconds, () -> Unit.INSTANCE)));

        for (int warningTime : List.of(30, 10, 5)) {
            if (warningTime >= durationSeconds) continue;
            float warningDelay = (float) durationSeconds - warningTime;
            com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(warningDelay, () -> {
                TournamentBattleSession loaded = TournamentBattleSessionStorage.loadSession(server, sessionId);
                if (loaded == null || loaded.getState().isTerminal()) return Unit.INSTANCE;
                if (!loaded.getState().isPreviewActive()) return Unit.INSTANCE;

                if (loaded.getPlayerOneSelection() == null || loaded.getPlayerOneSelection().isEmpty()) {
                    ServerPlayer p1 = server.getPlayerList().getPlayer(loaded.getPlayerOneUuid());
                    if (p1 != null) TournamentMessages.send(p1, "Voce tem " + warningTime + " segundos para selecionar seu time!");
                }
                if (loaded.getPlayerTwoSelection() == null || loaded.getPlayerTwoSelection().isEmpty()) {
                    ServerPlayer p2 = server.getPlayerList().getPlayer(loaded.getPlayerTwoUuid());
                    if (p2 != null) TournamentMessages.send(p2, "Voce tem " + warningTime + " segundos para selecionar seu time!");
                }
                return Unit.INSTANCE;
            });
        }
    }

    private static void applySelectionsAndStartBattle(MinecraftServer server, TournamentBattleSession session) {
        ServerPlayer player1 = server.getPlayerList().getPlayer(session.getPlayerOneUuid());
        ServerPlayer player2 = server.getPlayerList().getPlayer(session.getPlayerTwoUuid());

        if (player1 == null || player2 == null) {
            LOGGER.warn("Cannot apply selections: one or both players offline for session {}", session.getSessionId());
            TournamentBattleFinalizationService.safeFinalize(server, session.getSessionId(), "player_offline_before_swap");
            ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerOneUuid());
            ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerTwoUuid());
            return;
        }

        try {
            if (!session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES)) {
                LOGGER.error("Could not transition session {} to PREPARING_PARTIES", session.getSessionId());
                TournamentBattleFinalizationService.safeFinalize(server, session.getSessionId(), "state_transition_error");
                ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerOneUuid());
                ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerTwoUuid());
                return;
            }
            TournamentBattleSessionStorage.saveSession(server, session);

            TeamPreviewPartySwapService.SwapResult swapResult = TeamPreviewPartySwapService.saveSnapshotAndSwap(server, session);

            if (swapResult == TeamPreviewPartySwapService.SwapResult.SUCCESS) {
                if (!session.transitionTo(TournamentBattleStatus.BATTLE_STARTING)) {
                    LOGGER.error("Could not transition session {} to BATTLE_STARTING after successful swap", session.getSessionId());
                    TournamentBattleFinalizationService.safeFinalize(server, session.getSessionId(), "post_swap_transition_error");
                    ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerOneUuid());
                    ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerTwoUuid());
                    return;
                }
                TournamentBattleSessionStorage.saveSession(server, session);

                session.transitionTo(TournamentBattleStatus.COUNTDOWN);
                TournamentBattleSessionStorage.saveSession(server, session);

                TournamentBattleRecord activeBattle = TournamentStateService.getActiveBattle(server);
                if (activeBattle != null) {
                    activeBattle.setStatus(TournamentBattleStatus.COUNTDOWN);
                    TournamentStateService.setActiveBattle(server, activeBattle);
                }

                player1.closeContainer();
                player2.closeContainer();

                List<String> p1Names = session.getPlayerOnePokemonIdentities() != null ? session.getPlayerOnePokemonIdentities().stream()
                        .map(id -> id.contains("|") ? id.substring(id.indexOf('|') + 1) : id)
                        .toList() : List.of();
                List<String> p2Names = session.getPlayerTwoPokemonIdentities() != null ? session.getPlayerTwoPokemonIdentities().stream()
                        .map(id -> id.contains("|") ? id.substring(id.indexOf('|') + 1) : id)
                        .toList() : List.of();

                TournamentMessages.broadcast(server, "§6§l=========================================");
                TournamentMessages.broadcast(server, "§e§lSELEÇÃO DE DUPLAS CONFIRMADA!");
                TournamentMessages.broadcast(server, "§a" + player1.getGameProfile().getName() + " §fescolheu: §b" + String.join(", ", p1Names));
                TournamentMessages.broadcast(server, "§a" + player2.getGameProfile().getName() + " §fescolheu: §b" + String.join(", ", p2Names));
                TournamentMessages.broadcast(server, "§6§l=========================================");

                TournamentMessages.broadcastBattleCountdown(server, player1.getGameProfile().getName(), player2.getGameProfile().getName(), 10);

                com.cobblemon.mod.common.api.scheduling.ServerTaskTracker.INSTANCE.after(10F, () -> {
                    beginCobblemonBattle(server, player1, player2);
                    return Unit.INSTANCE;
                });
            } else {
                LOGGER.error("Party swap failed for session {} with result {}", session.getSessionId(), swapResult);
                TournamentBattleFinalizationService.safeFinalize(server, session.getSessionId(), "party_swap_failed");
                ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerOneUuid());
                ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerTwoUuid());
                TournamentMessages.broadcast(server, "Erro ao preparar as equipes para a batalha. As equipes originais foram restauradas.");
            }
        } catch (Exception e) {
            LOGGER.error("Fatal error during battle start for session {}", session.getSessionId(), e);
            TournamentBattleFinalizationService.safeFinalize(server, session.getSessionId(), "fatal_error");
            ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerOneUuid());
            ACTIVE_SESSION_BY_PLAYER.remove(session.getPlayerTwoUuid());
            TournamentMessages.broadcast(server, "Erro fatal ao iniciar a batalha. As equipes foram restauradas.");
        }
    }

    public static void restorePlayerOriginalParty(MinecraftServer server, UUID playerUuid) {
        List<Pokemon> original = PokemonTeamService.loadOriginalPartyFromDisk(server, playerUuid);
        if (original == null) {
            return;
        }

        try {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) {
                return;
            }
            PartyStore party = com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage().getParty(player);
            if (party != null) {
                for (int i = party.size() - 1; i >= 0; i--) {
                    Pokemon p = party.get(i);
                    if (p != null) {
                        party.remove(p);
                    }
                }
                for (Pokemon p : original) {
                    party.add(p);
                }
            }
            PokemonTeamService.deleteOriginalPartyFile(server, playerUuid);
        } catch (Exception e) {
            LOGGER.error("Failed to restore original party for player UUID " + playerUuid, e);
        }
    }

    private static void restoreAllOriginalPartiesFromDisk(MinecraftServer server) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("bigbang_tournaments").resolve("original_parties");
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".dat"))
                  .forEach(path -> {
                      String fileName = path.getFileName().toString();
                      String uuidString = fileName.substring(0, fileName.length() - ".dat".length());
                      try {
                          UUID uuid = UUID.fromString(uuidString);
                          restorePlayerOriginalParty(server, uuid);
                          LOGGER.info("Restored original party for player UUID {} during server recovery.", uuid);
                      } catch (Exception e) {
                          LOGGER.error("Failed to restore player party during recovery for file: " + fileName, e);
                      }
                  });
        } catch (IOException e) {
            LOGGER.error("Failed to list original parties directory during restart recovery", e);
        }
    }

    private record AreaLock(TournamentPosition position, int radius, long lastWarningAt) {
    }
}
