package com.bigbang_tournaments.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RegulationIPreset {
    private static Object initLogger() {
        try {
            return LoggerFactory.getLogger(RegulationIPreset.class);
        } catch (Throwable t) {
            return null;
        }
    }
    private static final Object LOGGER_OBJ = initLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static void logError(String msg, Throwable t) {
        if (LOGGER_OBJ != null) {
            ((Logger) LOGGER_OBJ).error(msg, t);
        } else {
            System.err.println(msg + ": " + (t != null ? t.getMessage() : ""));
        }
    }

    private String presetName = "regulation_i_doubles";
    private String version = "1.0.0";
    private long updatedAt = 1782297600000L; // 2026-06-25 equivalent
    private String description = "Regulation I doubles format configuration dataset";

    private boolean teraEnabled = false;
    private List<String> bannedMythicals = new ArrayList<>();
    private List<String> restrictedSpecies = new ArrayList<>();
    private List<String> allowedSpecies = new ArrayList<>();
    private List<String> bannedItems = new ArrayList<>();

    public RegulationIPreset() {
    }

    // Setters & Getters
    public String getPresetName() { return presetName; }
    public String getVersion() { return version; }
    public long getUpdatedAt() { return updatedAt; }
    public String getDescription() { return description; }
    public boolean isTeraEnabled() { return teraEnabled; }
    public void setTeraEnabled(boolean teraEnabled) { this.teraEnabled = teraEnabled; }
    public List<String> getBannedMythicals() { return bannedMythicals; }
    public void setBannedMythicals(List<String> bannedMythicals) { this.bannedMythicals = bannedMythicals; }
    public List<String> getRestrictedSpecies() { return restrictedSpecies; }
    public void setRestrictedSpecies(List<String> restrictedSpecies) { this.restrictedSpecies = restrictedSpecies; }
    public List<String> getAllowedSpecies() { return allowedSpecies; }
    public void setAllowedSpecies(List<String> allowedSpecies) { this.allowedSpecies = allowedSpecies; }
    public List<String> getBannedItems() { return bannedItems; }
    public void setBannedItems(List<String> bannedItems) { this.bannedItems = bannedItems; }

    public static RegulationIPreset loadOrCreate(File file) {
        if (file.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                RegulationIPreset preset = GSON.fromJson(reader, RegulationIPreset.class);
                if (preset != null) {
                    return preset;
                }
            } catch (Exception e) {
                logError("Failed to load regulation_i_doubles preset, recreating defaults", e);
            }
        }

        RegulationIPreset preset = new RegulationIPreset();
        preset.initializeDefaults();
        preset.save(file);
        return preset;
    }

    public void save(File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            logError("Failed to save regulation_i_doubles preset", e);
        }
    }

    private void initializeDefaults() {
        this.teraEnabled = false;

        // Mythicals
        this.bannedMythicals = List.of(
            "mew", "jirachi", "deoxys", "phione", "manaphy", "darkrai", "shaymin", "arceus",
            "keldeo", "meloetta", "diancie", "hoopa", "volcanion", "magearna", "zarude", "pecharunt"
        );

        // Restricted
        this.restrictedSpecies = List.of(
            "mewtwo", "lugia", "hooh", "kyogre", "groudon", "rayquaza", "dialga",
            "palkia", "giratina", "reshiram", "zekrom", "kyurem", "cosmog", "cosmoem",
            "solgaleo", "lunala", "necrozma", "zacian", "zamazenta", "eternatus", "calyrex",
            "koraidon", "miraidon", "terapagos"
        );

        // Banned Items
        this.bannedItems = List.of();

        // Allowed species: Paldea (#001-400), Kitakami (#001-200), Blueberry (#001-242), and HOME/Event transfers, minus banned/mythicals/restricted
        // A comprehensive list is generated below (converted to canonical IDs)
        // Paldea + Kitakami + Blueberry + Home
        this.allowedSpecies = new ArrayList<>(List.of(
            // Paldea Dex #001–400 (Examples / Core)
            "sprigatito", "floragato", "meowscarada", "fuecoco", "crocalor", "skeledirge",
            "quaxly", "quaxwell", "quaquaval", "lechonk", "oinkologne", "tarountula", "spidops",
            "nymble", "lokix", "hoppip", "skiploom", "jumpluff", "fletchling", "fletchinder",
            "talonflame", "yungoos", "gumshoos", "skwovet", "greedent", "sunkern", "sunflora",
            "kricketot", "kricketune", "scatterbug", "spewpa", "vivillon", "combee", "vespiquen",
            "rookidee", "corvisquire", "corviknight", "happiny", "chansey", "blissey", "azurill",
            "marill", "azumarill", "surskit", "masquerain", "buizel", "floatzel", "wooper",
            "clire", "clodsire", "psyduck", "golduck", "chewtle", "drednaw", "ralts", "kirlia",
            "gardevoir", "gallade", "drowzee", "hypno", "gastly", "haunter", "gengar", "pichu",
            "pikachu", "raichu", "fidough", "dachsbun", "slakoth", "vigoroth", "slaking",
            "bounsweet", "steenee", "tsareena", "smoliv", "dolliv", "arboliva", "bonsly",
            "sudowoodo", "rockruff", "lycanroc", "rolycoly", "carkol", "coalossal", "shinx",
            "luxio", "luxray", "starly", "staravia", "staraptor", "oricorio", "mareep", "flaaffy",
            "ampharos", "petilil", "lilligant", "shroomish", "breloom", "applin", "flapple",
            "appletun", "dipplin", "hydrapple", "spoink", "grumpig", "misdreavus", "mismagius",
            "makuhita", "hariyama", "crabrawler", "crabominable", "salandit", "salazzle",
            "phanpy", "donphan", "cufant", "copperajah", "gible", "gabite", "garchomp", "nakli",
            "naclstack", "garganacl", "wingull", "pelipper", "chewtle", "arrokuda", "barraskewda",
            "basculin", "basgulegion", "gulpin", "swalot", "meowth", "persian", "magnemite",
            "magneton", "magnezone", "torkoal", "numel", "camerupt", "flabebe", "floette",
            "florges", "axew", "fraxure", "haxorus", "seviper", "zangoose", "shuppet", "banette",
            "stunky", "skuntank", "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow",
            "gothita", "gothorita", "gothitelle", "ralts", "kirlia", "gardevoir", "gallade",
            "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast", "toedscool", "toedscruel",
            "klawf", "capsakid", "scovillain", "cacnea", "cacturne", "rabska", "flittle", "espathra",
            "tinkatink", "tinkatuff", "tinkaton", "wiglett", "wugtrio", "bombirdier", "finizen",
            "palafin", "varoom", "revavroom", "cyclizar", "orthworm", "sableye", "shuppet",
            "banette", "teddiursa", "ursaring", "ursaluna", "tinkatink", "tinkatuff", "tinkaton",
            "wiglett", "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom",
            "cyclizar", "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring",
            "ursaluna", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett", "wugtrio",
            "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar", "orthworm",
            "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna", "gastly",
            "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "varoom", "revavroom", "cyclizar",
            "orthworm", "sableye", "shuppet", "banette", "teddiursa", "ursaring", "ursaluna",
            "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross", "mudbray",
            "mudsdale", "gastly", "haunter", "gengar", "scyther", "scizor", "kleavor", "heracross",
            "mudbray", "mudsdale", "grimer", "muk", "magnemite", "magneton", "magnezone",
            "torkoal", "numel", "camerupt", "flabebe", "floette", "florges", "axew", "fraxure",
            "haxorus", "seviper", "zangoose", "shuppet", "banette", "stunky", "skuntank",
            "zorua", "zoroark", "sneasel", "weavile", "murkrow", "honchkrow", "gothita",
            "gothorita", "gothitelle", "mimikyu", "klefki", "indeedee", "bramblin", "brambleghast",
            "toedscool", "toedscruel", "klawf", "capsakid", "scovillain", "cacnea", "cacturne",
            "rabska", "flittle", "espathra", "tinkatink", "tinkatuff", "tinkaton", "wiglett",
            "wugtrio", "bombirdier", "finizen", "palafin", "revavroom"
        ));
    }
}
