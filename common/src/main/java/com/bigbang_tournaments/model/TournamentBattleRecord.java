package com.bigbang_tournaments.model;

import java.util.UUID;

public class TournamentBattleRecord {
    private UUID player1Uuid;
    private String player1Name;
    private UUID player2Uuid;
    private String player2Name;
    private UUID winnerUuid;
    private String winnerName;
    private UUID loserUuid;
    private String loserName;
    private String battleId;
    private TournamentBattleStatus status = TournamentBattleStatus.COUNTDOWN;
    private boolean manualResult;
    private boolean interruptedByRestart;
    private long createdAt;
    private long updatedAt;

    public UUID getPlayer1Uuid() {
        return player1Uuid;
    }

    public void setPlayer1Uuid(UUID player1Uuid) {
        this.player1Uuid = player1Uuid;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public void setPlayer1Name(String player1Name) {
        this.player1Name = player1Name;
    }

    public UUID getPlayer2Uuid() {
        return player2Uuid;
    }

    public void setPlayer2Uuid(UUID player2Uuid) {
        this.player2Uuid = player2Uuid;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }

    public UUID getWinnerUuid() {
        return winnerUuid;
    }

    public void setWinnerUuid(UUID winnerUuid) {
        this.winnerUuid = winnerUuid;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public UUID getLoserUuid() {
        return loserUuid;
    }

    public void setLoserUuid(UUID loserUuid) {
        this.loserUuid = loserUuid;
    }

    public String getLoserName() {
        return loserName;
    }

    public void setLoserName(String loserName) {
        this.loserName = loserName;
    }

    public String getBattleId() {
        return battleId;
    }

    public void setBattleId(String battleId) {
        this.battleId = battleId;
    }

    public TournamentBattleStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentBattleStatus status) {
        this.status = status;
    }

    public boolean isManualResult() {
        return manualResult;
    }

    public void setManualResult(boolean manualResult) {
        this.manualResult = manualResult;
    }

    public boolean isInterruptedByRestart() {
        return interruptedByRestart;
    }

    public void setInterruptedByRestart(boolean interruptedByRestart) {
        this.interruptedByRestart = interruptedByRestart;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
