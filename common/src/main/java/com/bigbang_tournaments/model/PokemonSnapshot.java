package com.bigbang_tournaments.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PokemonSnapshot {
    private UUID pokemonUuid;
    private int slot;
    private String showdownId;
    private String species;
    private String speciesIdentifier;
    private String form;
    private List<String> aspects = new ArrayList<>();
    private int originalLevel;
    private int originalExperience;
    private String heldItem;
    private String ability;
    private String nature;
    private String mintedNature;
    private List<PokemonMoveSnapshot> moveSet = new ArrayList<>();
    private List<PokemonMoveSnapshot> benchedMoves = new ArrayList<>();
    private Map<String, Integer> evs = new LinkedHashMap<>();
    private Map<String, Integer> ivs = new LinkedHashMap<>();
    private Map<String, Integer> hyperTrainedIvs = new LinkedHashMap<>();
    private boolean shiny;
    private int friendship;
    private String teraType;
    private boolean gmaxFactor;
    private int dynamaxLevel;
    private int originalHp;
    private String status;
    private String notes;

    public PokemonSnapshot() {
    }

    public PokemonSnapshot(UUID pokemonUuid, int slot, String showdownId, String species, String speciesIdentifier,
                           String form, List<String> aspects, int originalLevel, int originalExperience,
                           String heldItem, String ability, String nature, String mintedNature,
                           List<PokemonMoveSnapshot> moveSet, List<PokemonMoveSnapshot> benchedMoves,
                           Map<String, Integer> evs, Map<String, Integer> ivs, Map<String, Integer> hyperTrainedIvs,
                           boolean shiny, int friendship, String teraType, boolean gmaxFactor, int dynamaxLevel,
                           int originalHp, String status, String notes) {
        this.pokemonUuid = pokemonUuid;
        this.slot = slot;
        this.showdownId = showdownId;
        this.species = species;
        this.speciesIdentifier = speciesIdentifier;
        this.form = form;
        this.aspects = aspects != null ? aspects : new ArrayList<>();
        this.originalLevel = originalLevel;
        this.originalExperience = originalExperience;
        this.heldItem = heldItem;
        this.ability = ability;
        this.nature = nature;
        this.mintedNature = mintedNature;
        this.moveSet = moveSet != null ? moveSet : new ArrayList<>();
        this.benchedMoves = benchedMoves != null ? benchedMoves : new ArrayList<>();
        this.evs = evs != null ? evs : new LinkedHashMap<>();
        this.ivs = ivs != null ? ivs : new LinkedHashMap<>();
        this.hyperTrainedIvs = hyperTrainedIvs != null ? hyperTrainedIvs : new LinkedHashMap<>();
        this.shiny = shiny;
        this.friendship = friendship;
        this.teraType = teraType;
        this.gmaxFactor = gmaxFactor;
        this.dynamaxLevel = dynamaxLevel;
        this.originalHp = originalHp;
        this.status = status;
        this.notes = notes;
    }

    public UUID getPokemonUuid() {
        return pokemonUuid;
    }

    public void setPokemonUuid(UUID pokemonUuid) {
        this.pokemonUuid = pokemonUuid;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getShowdownId() {
        return showdownId;
    }

    public void setShowdownId(String showdownId) {
        this.showdownId = showdownId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSpeciesIdentifier() {
        return speciesIdentifier;
    }

    public void setSpeciesIdentifier(String speciesIdentifier) {
        this.speciesIdentifier = speciesIdentifier;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public List<String> getAspects() {
        return aspects;
    }

    public void setAspects(List<String> aspects) {
        this.aspects = aspects;
    }

    public int getOriginalLevel() {
        return originalLevel;
    }

    public void setOriginalLevel(int originalLevel) {
        this.originalLevel = originalLevel;
    }

    public int getOriginalExperience() {
        return originalExperience;
    }

    public void setOriginalExperience(int originalExperience) {
        this.originalExperience = originalExperience;
    }

    public String getHeldItem() {
        return heldItem;
    }

    public void setHeldItem(String heldItem) {
        this.heldItem = heldItem;
    }

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public String getMintedNature() {
        return mintedNature;
    }

    public void setMintedNature(String mintedNature) {
        this.mintedNature = mintedNature;
    }

    public List<PokemonMoveSnapshot> getMoveSet() {
        return moveSet;
    }

    public void setMoveSet(List<PokemonMoveSnapshot> moveSet) {
        this.moveSet = moveSet;
    }

    public List<PokemonMoveSnapshot> getBenchedMoves() {
        return benchedMoves;
    }

    public void setBenchedMoves(List<PokemonMoveSnapshot> benchedMoves) {
        this.benchedMoves = benchedMoves;
    }

    public Map<String, Integer> getEvs() {
        return evs;
    }

    public void setEvs(Map<String, Integer> evs) {
        this.evs = evs;
    }

    public Map<String, Integer> getIvs() {
        return ivs;
    }

    public void setIvs(Map<String, Integer> ivs) {
        this.ivs = ivs;
    }

    public Map<String, Integer> getHyperTrainedIvs() {
        return hyperTrainedIvs;
    }

    public void setHyperTrainedIvs(Map<String, Integer> hyperTrainedIvs) {
        this.hyperTrainedIvs = hyperTrainedIvs;
    }

    public boolean isShiny() {
        return shiny;
    }

    public void setShiny(boolean shiny) {
        this.shiny = shiny;
    }

    public int getFriendship() {
        return friendship;
    }

    public void setFriendship(int friendship) {
        this.friendship = friendship;
    }

    public String getTeraType() {
        return teraType;
    }

    public void setTeraType(String teraType) {
        this.teraType = teraType;
    }

    public boolean isGmaxFactor() {
        return gmaxFactor;
    }

    public void setGmaxFactor(boolean gmaxFactor) {
        this.gmaxFactor = gmaxFactor;
    }

    public int getDynamaxLevel() {
        return dynamaxLevel;
    }

    public void setDynamaxLevel(int dynamaxLevel) {
        this.dynamaxLevel = dynamaxLevel;
    }

    public int getOriginalHp() {
        return originalHp;
    }

    public void setOriginalHp(int originalHp) {
        this.originalHp = originalHp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
