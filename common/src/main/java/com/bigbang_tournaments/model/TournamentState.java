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
    private String tournamentPhase = "SCHEDULED";
    private long checkInStartedAt;
    private long checkInDeadline;

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

    public String getTournamentPhase() {
        return tournamentPhase;
    }

    public void setTournamentPhase(String tournamentPhase) {
        this.tournamentPhase = tournamentPhase;
    }

    public long getCheckInStartedAt() {
        return checkInStartedAt;
    }

    public void setCheckInStartedAt(long checkInStartedAt) {
        this.checkInStartedAt = checkInStartedAt;
    }

    public long getCheckInDeadline() {
        return checkInDeadline;
    }

    public void setCheckInDeadline(long checkInDeadline) {
        this.checkInDeadline = checkInDeadline;
    }

    public boolean normalizeAndMigrate() {
        boolean changed = false;
        if (tournamentType == null || tournamentType.trim().isEmpty()) {
            tournamentType = "standard";
            changed = true;
        } else {
            String normalizedType = tournamentType.toLowerCase().trim();
            if ("singleelement".equals(normalizedType) || "monotype".equals(normalizedType)) {
                normalizedType = "singletype";
            } else if ("2v2".equals(normalizedType) || "duplas".equals(normalizedType)) {
                normalizedType = "doubles";
            } else if ("regulation_i".equals(normalizedType) || "vgc_doubles".equals(normalizedType) || "vgc_reg_i".equals(normalizedType)) {
                normalizedType = "regulation_i_doubles";
            }
            if (!tournamentType.equals(normalizedType)) {
                tournamentType = normalizedType;
                changed = true;
            }
        }

        if (tournamentPhase == null || tournamentPhase.trim().isEmpty()) {
            tournamentPhase = "SCHEDULED";
            changed = true;
        }

        if (participants == null) {
            participants = new ArrayList<>();
            changed = true;
        }

        for (TournamentParticipantRecord participant : participants) {
            if (participant == null) {
                continue;
            }
            if (participant.getCheckInStatus() == null || participant.getCheckInStatus() == TournamentCheckInStatus.NOT_STARTED) {
                TournamentParticipantStatus legacyStatus = participant.getStatus() != null ? participant.getStatus() : TournamentParticipantStatus.REGISTERED;
                TournamentCheckInStatus migratedStatus = switch (legacyStatus) {
                    case CHECKED_IN -> TournamentCheckInStatus.CHECKED_IN;
                    case ABSENT -> TournamentCheckInStatus.ABSENT;
                    case AWAITING_CHECK_IN -> TournamentCheckInStatus.AWAITING;
                    default -> TournamentCheckInStatus.NOT_STARTED;
                };
                if (legacyStatus == TournamentParticipantStatus.CHECKED_IN || legacyStatus == TournamentParticipantStatus.ABSENT || legacyStatus == TournamentParticipantStatus.AWAITING_CHECK_IN) {
                    participant.setCheckInStatus(migratedStatus);
                    changed = true;
                }
            }
            if (participant.getCheckInStatus() == TournamentCheckInStatus.CHECKED_IN && participant.getCheckedInAt() <= 0L) {
                participant.setCheckedInAt(System.currentTimeMillis());
                changed = true;
            }
        }

        return changed;
    }
}
