package com.bigbang_tournaments.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TournamentParticipantRecord {
    private UUID playerUuid;
    private String playerName;
    private TournamentParticipantStatus status = TournamentParticipantStatus.REGISTERED;
    private int preparedLevel;
    private boolean prepared;
    private boolean rosterLocked;
    private boolean pendingValidation;
    private long createdAt;
    private long updatedAt;
    private long pendingSince;
    private long nextValidationAt;
    private List<TournamentRuleViolation> lastViolations = new ArrayList<>();

    public TournamentParticipantRecord() {
    }

    public TournamentParticipantRecord(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public TournamentParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentParticipantStatus status) {
        this.status = status;
    }

    public int getPreparedLevel() {
        return preparedLevel;
    }

    public void setPreparedLevel(int preparedLevel) {
        this.preparedLevel = preparedLevel;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public void setPrepared(boolean prepared) {
        this.prepared = prepared;
    }

    public boolean isRosterLocked() {
        return rosterLocked;
    }

    public void setRosterLocked(boolean rosterLocked) {
        this.rosterLocked = rosterLocked;
    }

    public boolean isPendingValidation() {
        return pendingValidation;
    }

    public void setPendingValidation(boolean pendingValidation) {
        this.pendingValidation = pendingValidation;
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

    public long getPendingSince() {
        return pendingSince;
    }

    public void setPendingSince(long pendingSince) {
        this.pendingSince = pendingSince;
    }

    public long getNextValidationAt() {
        return nextValidationAt;
    }

    public void setNextValidationAt(long nextValidationAt) {
        this.nextValidationAt = nextValidationAt;
    }

    public List<TournamentRuleViolation> getLastViolations() {
        return lastViolations;
    }

    public void setLastViolations(List<TournamentRuleViolation> lastViolations) {
        this.lastViolations = lastViolations;
    }

    private String assignedElement;
    private int rollsUsed;

    public String getAssignedElement() {
        return assignedElement;
    }

    public void setAssignedElement(String assignedElement) {
        this.assignedElement = assignedElement;
    }

    public int getRollsUsed() {
        return rollsUsed;
    }

    public void setRollsUsed(int rollsUsed) {
        this.rollsUsed = rollsUsed;
    }
}
