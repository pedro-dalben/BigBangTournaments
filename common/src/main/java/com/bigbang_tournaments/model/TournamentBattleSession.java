package com.bigbang_tournaments.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TournamentBattleSession {
    private UUID sessionId;
    private String battleId;
    private UUID tournamentId;
    private String battleFormat;
    private UUID playerOneUuid;
    private UUID playerTwoUuid;
    private String playerOneNameSnapshot;
    private String playerTwoNameSnapshot;
    private TournamentBattleStatus state;
    private long createdAt;
    private long updatedAt;
    private long previewExpiresAt;
    private List<Integer> playerOneSelection;
    private List<Integer> playerTwoSelection;
    private List<String> playerOnePokemonIdentities;
    private List<String> playerTwoPokemonIdentities;
    private String playerOneSnapshotPath;
    private String playerTwoSnapshotPath;
    private String playerOneSnapshotChecksum;
    private String playerTwoSnapshotChecksum;
    private String cobblemonBattleReference;
    private String finalizationReason;
    private Long restoredAt;
    private int schemaVersion;

    public TournamentBattleSession() {
        this.sessionId = UUID.randomUUID();
        this.state = TournamentBattleStatus.CREATED;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.schemaVersion = 1;
    }

    public TournamentBattleSession(UUID playerOneUuid, String playerOneName,
                                    UUID playerTwoUuid, String playerTwoName,
                                    String battleFormat) {
        this();
        this.playerOneUuid = playerOneUuid;
        this.playerOneNameSnapshot = playerOneName;
        this.playerTwoUuid = playerTwoUuid;
        this.playerTwoNameSnapshot = playerTwoName;
        this.battleFormat = battleFormat;
    }

    public boolean transitionTo(TournamentBattleStatus newState) {
        if (newState == this.state) {
            return false;
        }
        if (!isValidTransition(this.state, newState)) {
            return false;
        }
        this.state = newState;
        this.updatedAt = System.currentTimeMillis();
        return true;
    }

    public boolean tryFinalize(String reason) {
        if (state.isTerminal()) {
            return false;
        }
        if (state == TournamentBattleStatus.FAILED) {
            this.finalizationReason = reason;
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        if (transitionTo(TournamentBattleStatus.RESTORE_PENDING)) {
            this.finalizationReason = reason;
            return true;
        }
        return false;
    }

    public boolean tryCancel() {
        if (state.isTerminal()) {
            return false;
        }
        if (state.isPartyModified()) {
            return transitionTo(TournamentBattleStatus.RESTORE_PENDING);
        }
        return transitionTo(TournamentBattleStatus.CANCELLED);
    }

    public static boolean isValidTransition(TournamentBattleStatus from, TournamentBattleStatus to) {
        if (from == null) return to == TournamentBattleStatus.CREATED;
        if (from.isTerminal()) return false;
        if (from == to) return false;

        return switch (from) {
            case CREATED -> to == TournamentBattleStatus.TEAM_PREVIEW || to == TournamentBattleStatus.CANCELLED || to == TournamentBattleStatus.FAILED;
            case TEAM_PREVIEW -> to == TournamentBattleStatus.PLAYER_ONE_SELECTED
                    || to == TournamentBattleStatus.PLAYER_TWO_SELECTED
                    || to == TournamentBattleStatus.CANCELLED
                    || to == TournamentBattleStatus.FAILED;
            case PLAYER_ONE_SELECTED -> to == TournamentBattleStatus.PLAYER_TWO_SELECTED
                    || to == TournamentBattleStatus.PREPARING_PARTIES
                    || to == TournamentBattleStatus.CANCELLED
                    || to == TournamentBattleStatus.FAILED;
            case PLAYER_TWO_SELECTED -> to == TournamentBattleStatus.PLAYER_ONE_SELECTED
                    || to == TournamentBattleStatus.PREPARING_PARTIES
                    || to == TournamentBattleStatus.CANCELLED
                    || to == TournamentBattleStatus.FAILED;
            case PREPARING_PARTIES -> to == TournamentBattleStatus.PARTIES_SWAPPED
                    || to == TournamentBattleStatus.RESTORE_PENDING
                    || to == TournamentBattleStatus.FAILED;
            case PARTIES_SWAPPED -> to == TournamentBattleStatus.BATTLE_STARTING
                    || to == TournamentBattleStatus.RESTORE_PENDING
                    || to == TournamentBattleStatus.FAILED;
            case BATTLE_STARTING -> to == TournamentBattleStatus.COUNTDOWN
                    || to == TournamentBattleStatus.ACTIVE
                    || to == TournamentBattleStatus.RESTORE_PENDING
                    || to == TournamentBattleStatus.FAILED;
            case COUNTDOWN -> to == TournamentBattleStatus.ACTIVE
                    || to == TournamentBattleStatus.RESTORE_PENDING
                    || to == TournamentBattleStatus.CANCELLED
                    || to == TournamentBattleStatus.FAILED;
            case ACTIVE -> to == TournamentBattleStatus.RESTORE_PENDING
                    || to == TournamentBattleStatus.FINISHED
                    || to == TournamentBattleStatus.MANUAL_RESULT_REQUIRED
                    || to == TournamentBattleStatus.CANCELLED
                    || to == TournamentBattleStatus.FAILED;
            case RESTORE_PENDING -> to == TournamentBattleStatus.RESTORING
                    || to == TournamentBattleStatus.CANCELLED
                    || to == TournamentBattleStatus.FAILED;
            case RESTORING -> to == TournamentBattleStatus.RESTORED
                    || to == TournamentBattleStatus.FAILED;
            case FAILED -> to == TournamentBattleStatus.RESTORE_PENDING
                    || to == TournamentBattleStatus.CANCELLED;
            case MANUAL_RESULT_REQUIRED -> to == TournamentBattleStatus.RESTORE_PENDING
                    || to == TournamentBattleStatus.FINISHED
                    || to == TournamentBattleStatus.CANCELLED;
            case INTERRUPTED -> to == TournamentBattleStatus.RESTORE_PENDING
                    || to == TournamentBattleStatus.FINISHED
                    || to == TournamentBattleStatus.CANCELLED;
            default -> false;
        };
    }

    public boolean bothSelected() {
        return playerOneSelection != null && !playerOneSelection.isEmpty()
                && playerTwoSelection != null && !playerTwoSelection.isEmpty();
    }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }

    public UUID getTournamentId() { return tournamentId; }
    public void setTournamentId(UUID tournamentId) { this.tournamentId = tournamentId; }

    public String getBattleFormat() { return battleFormat; }
    public void setBattleFormat(String battleFormat) { this.battleFormat = battleFormat; }

    public UUID getPlayerOneUuid() { return playerOneUuid; }
    public void setPlayerOneUuid(UUID playerOneUuid) { this.playerOneUuid = playerOneUuid; }

    public UUID getPlayerTwoUuid() { return playerTwoUuid; }
    public void setPlayerTwoUuid(UUID playerTwoUuid) { this.playerTwoUuid = playerTwoUuid; }

    public String getPlayerOneNameSnapshot() { return playerOneNameSnapshot; }
    public void setPlayerOneNameSnapshot(String n) { this.playerOneNameSnapshot = n; }

    public String getPlayerTwoNameSnapshot() { return playerTwoNameSnapshot; }
    public void setPlayerTwoNameSnapshot(String n) { this.playerTwoNameSnapshot = n; }

    public TournamentBattleStatus getState() { return state; }
    public void setState(TournamentBattleStatus state) { this.state = state; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long t) { this.createdAt = t; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long t) { this.updatedAt = t; }

    public long getPreviewExpiresAt() { return previewExpiresAt; }
    public void setPreviewExpiresAt(long t) { this.previewExpiresAt = t; }

    public List<Integer> getPlayerOneSelection() { return playerOneSelection; }
    public void setPlayerOneSelection(List<Integer> s) { this.playerOneSelection = s; }

    public List<Integer> getPlayerTwoSelection() { return playerTwoSelection; }
    public void setPlayerTwoSelection(List<Integer> s) { this.playerTwoSelection = s; }

    public List<String> getPlayerOnePokemonIdentities() { return playerOnePokemonIdentities; }
    public void setPlayerOnePokemonIdentities(List<String> ids) { this.playerOnePokemonIdentities = ids; }

    public List<String> getPlayerTwoPokemonIdentities() { return playerTwoPokemonIdentities; }
    public void setPlayerTwoPokemonIdentities(List<String> ids) { this.playerTwoPokemonIdentities = ids; }

    public String getPlayerOneSnapshotPath() { return playerOneSnapshotPath; }
    public void setPlayerOneSnapshotPath(String p) { this.playerOneSnapshotPath = p; }

    public String getPlayerTwoSnapshotPath() { return playerTwoSnapshotPath; }
    public void setPlayerTwoSnapshotPath(String p) { this.playerTwoSnapshotPath = p; }

    public String getPlayerOneSnapshotChecksum() { return playerOneSnapshotChecksum; }
    public void setPlayerOneSnapshotChecksum(String c) { this.playerOneSnapshotChecksum = c; }

    public String getPlayerTwoSnapshotChecksum() { return playerTwoSnapshotChecksum; }
    public void setPlayerTwoSnapshotChecksum(String c) { this.playerTwoSnapshotChecksum = c; }

    public String getCobblemonBattleReference() { return cobblemonBattleReference; }
    public void setCobblemonBattleReference(String r) { this.cobblemonBattleReference = r; }

    public String getFinalizationReason() { return finalizationReason; }
    public void setFinalizationReason(String r) { this.finalizationReason = r; }

    public Long getRestoredAt() { return restoredAt; }
    public void setRestoredAt(Long t) { this.restoredAt = t; }

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int v) { this.schemaVersion = v; }
}
