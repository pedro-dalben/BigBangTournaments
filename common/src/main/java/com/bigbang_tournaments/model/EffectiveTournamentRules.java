package com.bigbang_tournaments.model;

import java.util.ArrayList;
import java.util.List;

public class EffectiveTournamentRules {
    private final boolean banLegendaries;
    private final boolean banMythicals;
    private final List<String> bannedSpecies;
    private final List<String> bannedItems;
    private final boolean itemClauseEnabled;
    private final boolean speciesClauseEnabled;
    private final boolean allowMega;
    private final boolean allowTera;
    private final boolean allowDynamax;
    private final boolean allowZMove;
    private final boolean singleSpecialMechanicPerTeam;

    public EffectiveTournamentRules(
            boolean banLegendaries,
            boolean banMythicals,
            List<String> bannedSpecies,
            List<String> bannedItems,
            boolean itemClauseEnabled,
            boolean speciesClauseEnabled,
            boolean allowMega,
            boolean allowTera,
            boolean allowDynamax,
            boolean allowZMove,
            boolean singleSpecialMechanicPerTeam) {
        this.banLegendaries = banLegendaries;
        this.banMythicals = banMythicals;
        this.bannedSpecies = bannedSpecies == null ? List.of() : List.copyOf(new ArrayList<>(bannedSpecies));
        this.bannedItems = bannedItems == null ? List.of() : List.copyOf(new ArrayList<>(bannedItems));
        this.itemClauseEnabled = itemClauseEnabled;
        this.speciesClauseEnabled = speciesClauseEnabled;
        this.allowMega = allowMega;
        this.allowTera = allowTera;
        this.allowDynamax = allowDynamax;
        this.allowZMove = allowZMove;
        this.singleSpecialMechanicPerTeam = singleSpecialMechanicPerTeam;
    }

    public boolean isBanLegendaries() {
        return banLegendaries;
    }

    public boolean isBanMythicals() {
        return banMythicals;
    }

    public List<String> getBannedSpecies() {
        return bannedSpecies;
    }

    public List<String> getBannedItems() {
        return bannedItems;
    }

    public boolean isItemClauseEnabled() {
        return itemClauseEnabled;
    }

    public boolean isSpeciesClauseEnabled() {
        return speciesClauseEnabled;
    }

    public boolean isAllowMega() {
        return allowMega;
    }

    public boolean isAllowTera() {
        return allowTera;
    }

    public boolean isAllowDynamax() {
        return allowDynamax;
    }

    public boolean isAllowZMove() {
        return allowZMove;
    }

    public boolean isSingleSpecialMechanicPerTeam() {
        return singleSpecialMechanicPerTeam;
    }
}
