package com.bigbang_tournaments.model;

import java.util.ArrayList;
import java.util.List;

public class TournamentState {
    private int schemaVersion = 1;
    private TournamentArenaState arena = new TournamentArenaState();
    private List<TournamentParticipantRecord> participants = new ArrayList<>();
    private TournamentBattleRecord activeBattle;
    private List<TournamentBattleRecord> battleHistory = new ArrayList<>();

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public TournamentArenaState getArena() {
        return arena;
    }

    public void setArena(TournamentArenaState arena) {
        this.arena = arena;
    }

    public List<TournamentParticipantRecord> getParticipants() {
        return participants;
    }

    public void setParticipants(List<TournamentParticipantRecord> participants) {
        this.participants = participants;
    }

    public TournamentBattleRecord getActiveBattle() {
        return activeBattle;
    }

    public void setActiveBattle(TournamentBattleRecord activeBattle) {
        this.activeBattle = activeBattle;
    }

    public List<TournamentBattleRecord> getBattleHistory() {
        return battleHistory;
    }

    public void setBattleHistory(List<TournamentBattleRecord> battleHistory) {
        this.battleHistory = battleHistory;
    }

    private String scheduledDate;
    private String scheduledTime;
    private String tournamentName;
    private String tournamentType;

    public String getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(String scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String getTournamentType() {
        return tournamentType;
    }

    public void setTournamentType(String tournamentType) {
        this.tournamentType = tournamentType;
    }
}
