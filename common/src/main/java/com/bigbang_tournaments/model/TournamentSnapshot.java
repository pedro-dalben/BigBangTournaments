package com.bigbang_tournaments.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TournamentSnapshot {
    private UUID playerUuid;
    private String playerName;
    private long createdAt;
    private long updatedAt;
    private int preparedLevel;
    private String status;
    private List<PokemonSnapshot> party = new ArrayList<>();

    public TournamentSnapshot() {
    }

    public TournamentSnapshot(UUID playerUuid, String playerName, long createdAt, long updatedAt, int preparedLevel, String status, List<PokemonSnapshot> party) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.preparedLevel = preparedLevel;
        this.status = status;
        this.party = party;
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

    public List<PokemonSnapshot> getParty() {
        return party;
    }

    public void setParty(List<PokemonSnapshot> party) {
        this.party = party;
    }
}
