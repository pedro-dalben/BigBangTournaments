package com.bigbang_tournaments.model;

public class TeamPreviewConfig {
    private int durationSeconds = 60;
    private String autoSelectStrategy = "FIRST_FOUR";
    private boolean revealSpecies = true;
    private boolean revealHeldItems = true;
    private boolean revealAbilities = true;

    public TeamPreviewConfig() {}

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int v) { this.durationSeconds = v; }

    public String getAutoSelectStrategy() { return autoSelectStrategy; }
    public void setAutoSelectStrategy(String v) { this.autoSelectStrategy = v; }

    public boolean isRevealSpecies() { return revealSpecies; }
    public void setRevealSpecies(boolean v) { this.revealSpecies = v; }

    public boolean isRevealHeldItems() { return revealHeldItems; }
    public void setRevealHeldItems(boolean v) { this.revealHeldItems = v; }

    public boolean isRevealAbilities() { return revealAbilities; }
    public void setRevealAbilities(boolean v) { this.revealAbilities = v; }
}
