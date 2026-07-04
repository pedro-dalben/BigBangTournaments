package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentBattleSession;
import com.bigbang_tournaments.model.TournamentBattleStatus;
import com.bigbang_tournaments.storage.TournamentBattleSessionStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TournamentBattleFinalizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TournamentBattleFinalizationService.class);
    private static final ConcurrentHashMap<UUID, Object> SESSION_LOCKS = new ConcurrentHashMap<>();

    public enum FinalizeResult {
        SUCCESS,
        ALREADY_FINALIZED,
        SESSION_NOT_FOUND,
        LOCK_FAILED,
        RESTORE_FAILED_P1,
        RESTORE_FAILED_P2,
        ERROR
    }

    public static FinalizeResult safeFinalize(MinecraftServer server, UUID sessionId, String reason) {
        return safeFinalize(server, sessionId, reason, null);
    }

    public static FinalizeResult safeFinalize(MinecraftServer server, UUID sessionId, String reason, ServerPlayer knownPlayer) {
        try {
            return finalizeSession(server, sessionId, reason, knownPlayer);
        } catch (Exception e) {
            LOGGER.error("Safe finalization caught exception for session {}", sessionId, e);
            return FinalizeResult.ERROR;
        }
    }

    public static FinalizeResult finalizeSession(MinecraftServer server, UUID sessionId, String reason) {
        return finalizeSession(server, sessionId, reason, null);
    }

    public static FinalizeResult finalizeSession(MinecraftServer server, UUID sessionId, String reason, ServerPlayer knownPlayer) {
        Object lock = SESSION_LOCKS.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            try {
                TournamentBattleSession session = TournamentBattleSessionStorage.loadSession(server, sessionId);
                if (session == null) {
                    LOGGER.warn("Finalization requested for non-existent session {}", sessionId);
                    return FinalizeResult.SESSION_NOT_FOUND;
                }

                if (session.getState().isTerminal()) {
                    LOGGER.info("Session {} already in terminal state {}, skipping finalization", sessionId, session.getState());
                    return FinalizeResult.ALREADY_FINALIZED;
                }

                if (session.getState() == TournamentBattleStatus.FAILED
                        && session.getPlayerOneSnapshotPath() == null
                        && session.getPlayerTwoSnapshotPath() == null) {
                    session.setState(TournamentBattleStatus.CANCELLED);
                    session.setFinalizationReason(reason);
                    session.setUpdatedAt(System.currentTimeMillis());
                    TournamentBattleSessionStorage.saveSession(server, session);
                    return FinalizeResult.SUCCESS;
                }

                if (!session.tryFinalize(reason)) {
                    LOGGER.warn("Could not transition session {} to RESTORE_PENDING from state {}", sessionId, session.getState());
                    if (session.getState().isTerminal()) {
                        return FinalizeResult.ALREADY_FINALIZED;
                    }
                    session.setState(TournamentBattleStatus.RESTORE_PENDING);
                    session.setFinalizationReason(reason);
                }
                TournamentBattleSessionStorage.saveSession(server, session);

                boolean p1Restored = false;
                boolean p2Restored = false;

                try {
                    if (session.getPlayerOneSnapshotPath() != null) {
                        Path p1Snap = TournamentBattleSessionStorage.getSnapshotFile(server, sessionId, session.getPlayerOneUuid());
                        if (Files.exists(p1Snap)) {
                            p1Restored = TeamPreviewPartySwapService.restorePlayerFromDisk(server, session, session.getPlayerOneUuid(), knownPlayer);
                            if (p1Restored) {
                                try { Files.deleteIfExists(p1Snap); } catch (Exception ignored) {}
                            }
                        } else {
                            p1Restored = true;
                        }
                    } else {
                        p1Restored = true;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to restore player 1 in session {}", sessionId, e);
                }

                try {
                    if (session.getPlayerTwoSnapshotPath() != null) {
                        Path p2Snap = TournamentBattleSessionStorage.getSnapshotFile(server, sessionId, session.getPlayerTwoUuid());
                        if (Files.exists(p2Snap)) {
                            p2Restored = TeamPreviewPartySwapService.restorePlayerFromDisk(server, session, session.getPlayerTwoUuid(), knownPlayer);
                            if (p2Restored) {
                                try { Files.deleteIfExists(p2Snap); } catch (Exception ignored) {}
                            }
                        } else {
                            p2Restored = true;
                        }
                    } else {
                        p2Restored = true;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to restore player 2 in session {}", sessionId, e);
                }

                if (!p1Restored && !p2Restored && session.getPlayerOneSnapshotPath() == null && session.getPlayerTwoSnapshotPath() == null) {
                    session.setState(TournamentBattleStatus.CANCELLED);
                } else if (p1Restored && p2Restored) {
                    session.transitionTo(TournamentBattleStatus.RESTORED);
                    session.setRestoredAt(System.currentTimeMillis());
                } else if (session.getState() == TournamentBattleStatus.RESTORE_PENDING) {
                    session.setState(TournamentBattleStatus.RESTORING);
                    if (!p1Restored && session.getPlayerOneSnapshotPath() != null) {
                        LOGGER.error("Player 1 restore failed in session {}", sessionId);
                    }
                    if (!p2Restored && session.getPlayerTwoSnapshotPath() != null) {
                        LOGGER.error("Player 2 restore failed in session {}", sessionId);
                    }
                    session.setState(TournamentBattleStatus.FAILED);
                }
                session.setUpdatedAt(System.currentTimeMillis());
                TournamentBattleSessionStorage.saveSession(server, session);

                if (session.getState() == TournamentBattleStatus.RESTORED) {
                    TournamentBattleSessionStorage.deleteSession(server, sessionId);
                    TournamentBattleService.removeActiveSession(session.getPlayerOneUuid());
                    TournamentBattleService.removeActiveSession(session.getPlayerTwoUuid());
                    LOGGER.info("Session {} finalized and cleaned up. Reason: {}", sessionId, reason);
                }

                if (p1Restored && p2Restored) {
                    return FinalizeResult.SUCCESS;
                }
                return FinalizeResult.RESTORE_FAILED_P1;
            } catch (Exception e) {
                LOGGER.error("Fatal error during session finalization for {}", sessionId, e);
                return FinalizeResult.ERROR;
            } finally {
                SESSION_LOCKS.remove(sessionId);
            }
        }
    }
}
