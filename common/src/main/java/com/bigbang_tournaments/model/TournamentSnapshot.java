package com.bigbang_tournaments.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TournamentSnapshot {
    private int schemaVersion = 2;
    private UUID playerUuid;
    private String playerName;
    private long createdAt;
    private long updatedAt;
    private int preparedLevel;
    private String status;
    private boolean rosterLocked;
    private List<PokemonSnapshot> party = new ArrayList<>();

    public TournamentSnapshot() {
    }

    public TournamentSnapshot(int schemaVersion, UUID playerUuid, String playerName, long createdAt, long updatedAt,
                              int preparedLevel, String status, boolean rosterLocked, List<PokemonSnapshot> party) {
        this.schemaVersion = schemaVersion;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.preparedLevel = preparedLevel;
        this.status = status;
        this.rosterLocked = rosterLocked;
        this.party = party;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
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

    public int getPreparedLevel() {
        return preparedLevel;
    }

    public void setPreparedLevel(int preparedLevel) {
        this.preparedLevel = preparedLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isRosterLocked() {
        return rosterLocked;
    }

    public void setRosterLocked(boolean rosterLocked) {
        this.rosterLocked = rosterLocked;
    }

    public List<PokemonSnapshot> getParty() {
        return party;
    }

    public void setParty(List<PokemonSnapshot> party) {
        this.party = party;
    }
}
