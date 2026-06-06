package com.bigbang_tournaments.model;

public class TournamentArenaState {
    private TournamentPosition pos1;
    private TournamentPosition pos2;
    private TournamentPosition spectator;

    public TournamentPosition getPos1() {
        return pos1;
    }

    public void setPos1(TournamentPosition pos1) {
        this.pos1 = pos1;
    }

    public TournamentPosition getPos2() {
        return pos2;
    }

    public void setPos2(TournamentPosition pos2) {
        this.pos2 = pos2;
    }

    public TournamentPosition getSpectator() {
        return spectator;
    }

    public void setSpectator(TournamentPosition spectator) {
        this.spectator = spectator;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null && spectator != null;
    }
}
