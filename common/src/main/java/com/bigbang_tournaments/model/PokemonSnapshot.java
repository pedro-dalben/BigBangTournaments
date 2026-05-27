package com.bigbang_tournaments.model;

import java.util.UUID;

public class PokemonSnapshot {
    private UUID pokemonUuid;
    private int slot;
    private int originalLevel;
    private String species;
    private String form;
    private boolean shiny;
    private String heldItem;
    private int originalHp;
    private String notes;

    public PokemonSnapshot() {
    }

    public PokemonSnapshot(UUID pokemonUuid, int slot, int originalLevel, String species, String form, boolean shiny, String heldItem, int originalHp, String notes) {
        this.pokemonUuid = pokemonUuid;
        this.slot = slot;
        this.originalLevel = originalLevel;
        this.species = species;
        this.form = form;
        this.shiny = shiny;
        this.heldItem = heldItem;
        this.originalHp = originalHp;
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

    public int getOriginalLevel() {
        return originalLevel;
    }

    public void setOriginalLevel(int originalLevel) {
        this.originalLevel = originalLevel;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public boolean isShiny() {
        return shiny;
    }

    public void setShiny(boolean shiny) {
        this.shiny = shiny;
    }

    public String getHeldItem() {
        return heldItem;
    }

    public void setHeldItem(String heldItem) {
        this.heldItem = heldItem;
    }

    public int getOriginalHp() {
        return originalHp;
    }

    public void setOriginalHp(int originalHp) {
        this.originalHp = originalHp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
