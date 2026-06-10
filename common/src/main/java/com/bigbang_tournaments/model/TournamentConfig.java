package com.bigbang_tournaments.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TournamentConfig {
    private static final List<String> DEFAULT_BANNED_SPECIES = List.of(
            "calyrex-ice",
            "calyrex-shadow",
            "deoxys-attack",
            "groudon-primal",
            "koraidon",
            "kyogre-primal",
            "kyurem-black",
            "kyurem-white",
            "marshadow",
            "mewtwo",
            "miraidon",
            "necrozma-dawn",
            "necrozma-dusk",
            "rayquaza-mega",
            "terapagos",
            "xerneas",
            "yveltal",
            "zacian",
            "zamazenta",
            "zygarde-mega"
    );

    private int schemaVersion = 2;
    private List<Integer> allowedLevels = new ArrayList<>(Arrays.asList(50, 100));
    private int correctionWindowSeconds = 300;
    private int arenaRadius = 3;
    private boolean banLegendaries = true;
    private boolean banMythicals = true;
    private List<String> bannedSpecies = new ArrayList<>(DEFAULT_BANNED_SPECIES);
    private List<String> bannedItems = new ArrayList<>();
    private boolean itemClauseEnabled = true;
    private boolean speciesClauseEnabled = true;
    private boolean allowMega = true;
    private boolean allowTera = true;
    private boolean allowDynamax = true;
    private boolean allowZMove = true;
    private boolean singleSpecialMechanicPerTeam = true;
    private int reconnectWindowSeconds = 300;
    private int adminPermissionLevel = 2;
    private int defaultRerolls = 1;
    private String registeredLoginMessage = "Falta %d dias pra o campeonato!";
    private String unregisteredLoginMessage = "ira acontecer um campeonato no dia %s as %s, para se inscrever digite /participarcampeonato";
    private String broadcastRegistrationMessage = "O jogador %s se inscreveu no campeonato!";

    public int getDefaultRerolls() {
        return defaultRerolls;
    }

    public void setDefaultRerolls(int defaultRerolls) {
        this.defaultRerolls = defaultRerolls;
    }

    public String getRegisteredLoginMessage() {
        return registeredLoginMessage;
    }

    public void setRegisteredLoginMessage(String registeredLoginMessage) {
        this.registeredLoginMessage = registeredLoginMessage;
    }

    public String getUnregisteredLoginMessage() {
        return unregisteredLoginMessage;
    }

    public void setUnregisteredLoginMessage(String unregisteredLoginMessage) {
        this.unregisteredLoginMessage = unregisteredLoginMessage;
    }

    public String getBroadcastRegistrationMessage() {
        return broadcastRegistrationMessage;
    }

    public void setBroadcastRegistrationMessage(String broadcastRegistrationMessage) {
        this.broadcastRegistrationMessage = broadcastRegistrationMessage;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public List<Integer> getAllowedLevels() {
        return allowedLevels;
    }

    public void setAllowedLevels(List<Integer> allowedLevels) {
        this.allowedLevels = allowedLevels;
    }

    public int getCorrectionWindowSeconds() {
        return correctionWindowSeconds;
    }

    public void setCorrectionWindowSeconds(int correctionWindowSeconds) {
        this.correctionWindowSeconds = correctionWindowSeconds;
    }

    public int getArenaRadius() {
        return arenaRadius;
    }

    public void setArenaRadius(int arenaRadius) {
        this.arenaRadius = arenaRadius;
    }

    public boolean isBanLegendaries() {
        return banLegendaries;
    }

    public void setBanLegendaries(boolean banLegendaries) {
        this.banLegendaries = banLegendaries;
    }

    public boolean isBanMythicals() {
        return banMythicals;
    }

    public void setBanMythicals(boolean banMythicals) {
        this.banMythicals = banMythicals;
    }

    public List<String> getBannedSpecies() {
        return bannedSpecies;
    }

    public void setBannedSpecies(List<String> bannedSpecies) {
        this.bannedSpecies = bannedSpecies;
    }

    public List<String> getBannedItems() {
        return bannedItems;
    }

    public void setBannedItems(List<String> bannedItems) {
        this.bannedItems = bannedItems;
    }

    public boolean isItemClauseEnabled() {
        return itemClauseEnabled;
    }

    public void setItemClauseEnabled(boolean itemClauseEnabled) {
        this.itemClauseEnabled = itemClauseEnabled;
    }

    public boolean isSpeciesClauseEnabled() {
        return speciesClauseEnabled;
    }

    public void setSpeciesClauseEnabled(boolean speciesClauseEnabled) {
        this.speciesClauseEnabled = speciesClauseEnabled;
    }

    public boolean isAllowMega() {
        return allowMega;
    }

    public void setAllowMega(boolean allowMega) {
        this.allowMega = allowMega;
    }

    public boolean isAllowTera() {
        return allowTera;
    }

    public void setAllowTera(boolean allowTera) {
        this.allowTera = allowTera;
    }

    public boolean isAllowDynamax() {
        return allowDynamax;
    }

    public void setAllowDynamax(boolean allowDynamax) {
        this.allowDynamax = allowDynamax;
    }

    public boolean isAllowZMove() {
        return allowZMove;
    }

    public void setAllowZMove(boolean allowZMove) {
        this.allowZMove = allowZMove;
    }

    public boolean isSingleSpecialMechanicPerTeam() {
        return singleSpecialMechanicPerTeam;
    }

    public void setSingleSpecialMechanicPerTeam(boolean singleSpecialMechanicPerTeam) {
        this.singleSpecialMechanicPerTeam = singleSpecialMechanicPerTeam;
    }

    public int getReconnectWindowSeconds() {
        return reconnectWindowSeconds;
    }

    public void setReconnectWindowSeconds(int reconnectWindowSeconds) {
        this.reconnectWindowSeconds = reconnectWindowSeconds;
    }

    public int getAdminPermissionLevel() {
        return adminPermissionLevel;
    }

    public void setAdminPermissionLevel(int adminPermissionLevel) {
        this.adminPermissionLevel = adminPermissionLevel;
    }

    public boolean normalizeAndMigrate() {
        boolean changed = false;

        if (allowedLevels == null || allowedLevels.isEmpty()) {
            allowedLevels = new ArrayList<>(Arrays.asList(50, 100));
            changed = true;
        }
        if (bannedSpecies == null) {
            bannedSpecies = new ArrayList<>();
            changed = true;
        }
        if (bannedItems == null) {
            bannedItems = new ArrayList<>();
            changed = true;
        }

        if (schemaVersion < 2) {
            changed |= seedDefaultBannedSpecies();
            schemaVersion = 2;
            changed = true;
        }

        if (registeredLoginMessage == null) {
            registeredLoginMessage = "Falta %d dias pra o campeonato!";
            changed = true;
        }
        if (unregisteredLoginMessage == null) {
            unregisteredLoginMessage = "ira acontecer um campeonato no dia %s as %s, para se inscrever digite /participarcampeonato";
            changed = true;
        }
        if (broadcastRegistrationMessage == null) {
            broadcastRegistrationMessage = "O jogador %s se inscreveu no campeonato!";
            changed = true;
        }
        if (defaultRerolls <= 0) {
            defaultRerolls = 1;
            changed = true;
        }

        changed |= deduplicateEntries(bannedSpecies);
        changed |= deduplicateEntries(bannedItems);
        return changed;
    }

    public static List<String> defaultBannedSpecies() {
        return DEFAULT_BANNED_SPECIES;
    }

    private boolean seedDefaultBannedSpecies() {
        boolean changed = false;
        for (String bannedEntry : DEFAULT_BANNED_SPECIES) {
            if (!containsNormalized(bannedSpecies, bannedEntry)) {
                bannedSpecies.add(bannedEntry);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean containsNormalized(List<String> values, String candidate) {
        String normalizedCandidate = normalize(candidate);
        for (String value : values) {
            if (normalize(value).equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean deduplicateEntries(List<String> values) {
        if (values == null || values.size() < 2) {
            return false;
        }

        Set<String> seen = new LinkedHashSet<>();
        boolean changed = false;
        List<String> deduplicated = new ArrayList<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized.isBlank() || seen.contains(normalized)) {
                changed = true;
                continue;
            }
            seen.add(normalized);
            deduplicated.add(value);
        }
        if (changed) {
            values.clear();
            values.addAll(deduplicated);
        }
        return changed;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "");
    }
}
