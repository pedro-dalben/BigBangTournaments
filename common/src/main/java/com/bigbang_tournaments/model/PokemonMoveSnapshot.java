package com.bigbang_tournaments.model;

import java.util.Objects;

public class PokemonMoveSnapshot {
    private String moveName;
    private int currentPp;
    private int maxPp;
    private int ppRaisedStages;

    public PokemonMoveSnapshot() {
    }

    public PokemonMoveSnapshot(String moveName, int currentPp, int maxPp, int ppRaisedStages) {
        this.moveName = moveName;
        this.currentPp = currentPp;
        this.maxPp = maxPp;
        this.ppRaisedStages = ppRaisedStages;
    }

    public String getMoveName() {
        return moveName;
    }

    public void setMoveName(String moveName) {
        this.moveName = moveName;
    }

    public int getCurrentPp() {
        return currentPp;
    }

    public void setCurrentPp(int currentPp) {
        this.currentPp = currentPp;
    }

    public int getMaxPp() {
        return maxPp;
    }

    public void setMaxPp(int maxPp) {
        this.maxPp = maxPp;
    }

    public int getPpRaisedStages() {
        return ppRaisedStages;
    }

    public void setPpRaisedStages(int ppRaisedStages) {
        this.ppRaisedStages = ppRaisedStages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PokemonMoveSnapshot that)) {
            return false;
        }
        return currentPp == that.currentPp
                && maxPp == that.maxPp
                && ppRaisedStages == that.ppRaisedStages
                && Objects.equals(moveName, that.moveName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moveName, currentPp, maxPp, ppRaisedStages);
    }
}
