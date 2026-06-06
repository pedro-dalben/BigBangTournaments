package com.bigbang_tournaments.service;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TournamentPokemonBanHelper {
    private TournamentPokemonBanHelper() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    public static String speciesKey(Species species) {
        if (species == null) {
            return "";
        }
        String showdownId = safe(species.showdownId());
        if (!showdownId.isBlank()) {
            return normalize(showdownId);
        }

        String speciesName = safe(species.getName());
        if (!speciesName.isBlank()) {
            return normalize(speciesName);
        }

        ResourceLocation identifier = species.getResourceIdentifier();
        return identifier != null ? normalize(identifier.toString()) : "";
    }

    public static String formKey(FormData form) {
        if (form == null) {
            return "";
        }

        String token = safe(form.formOnlyShowdownId());
        if (token.isBlank()) {
            token = safe(form.showdownId());
        }
        if (token.isBlank()) {
            token = safe(form.getName());
        }
        return normalize(token);
    }

    public static String speciesEntry(Species species) {
        return speciesKey(species);
    }

    public static String speciesFormEntry(Species species, String formToken) {
        String speciesKey = speciesKey(species);
        String normalizedForm = normalize(formToken);
        if (speciesKey.isBlank()) {
            return normalizedForm;
        }
        if (normalizedForm.isBlank()) {
            return speciesKey;
        }
        return speciesKey + "-" + normalizedForm;
    }

    public static boolean matchesBanEntry(Pokemon pokemon, String bannedEntry) {
        String normalizedEntry = normalize(bannedEntry);
        if (normalizedEntry.isBlank() || pokemon == null) {
            return false;
        }

        for (String alias : buildPokemonAliases(pokemon)) {
            if (alias.startsWith(normalizedEntry)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> buildSpeciesAndFormSuggestions() {
        List<String> suggestions = new ArrayList<>();
        for (Species species : PokemonSpecies.getImplemented()) {
            String speciesEntry = speciesEntry(species);
            if (!speciesEntry.isBlank()) {
                suggestions.add(speciesEntry);
            }

            for (FormData form : species.getForms()) {
                String formEntry = speciesFormEntry(species, formKey(form));
                if (!formEntry.isBlank() && !formEntry.equals(speciesEntry)) {
                    suggestions.add(formEntry);
                }
            }
        }
        return suggestions;
    }

    public static List<String> buildSpeciesFormSuggestions(Species species) {
        List<String> suggestions = new ArrayList<>();
        if (species == null) {
            return suggestions;
        }

        for (FormData form : species.getForms()) {
            String formEntry = formKey(form);
            if (!formEntry.isBlank()) {
                suggestions.add(formEntry);
            }
        }
        return suggestions;
    }

    public static String describeEntry(String entry) {
        String normalizedEntry = normalize(entry);
        if (normalizedEntry.isBlank()) {
            return entry;
        }

        for (Species species : PokemonSpecies.getImplemented()) {
            String speciesEntry = speciesEntry(species);
            if (normalizedEntry.equals(speciesEntry)) {
                return species.getName();
            }

            for (FormData form : species.getForms()) {
                String formEntry = speciesFormEntry(species, formKey(form));
                if (normalizedEntry.equals(formEntry) || formEntry.startsWith(normalizedEntry)) {
                    return species.getName() + " (" + form.getName() + ")";
                }
            }
        }

        return entry;
    }

    private static Set<String> buildPokemonAliases(Pokemon pokemon) {
        Set<String> aliases = new LinkedHashSet<>();
        Species species = pokemon.getSpecies();
        FormData form = pokemon.getForm();

        String speciesName = species != null ? safe(species.getName()) : "";
        String speciesShowdownId = species != null ? safe(species.showdownId()) : "";
        String speciesIdentifier = species != null && species.getResourceIdentifier() != null
                ? safe(species.getResourceIdentifier().toString())
                : "";
        String formName = form != null ? safe(form.getName()) : "";
        String formShowdownId = form != null ? safe(form.showdownId()) : "";
        String formOnlyShowdownId = form != null ? safe(form.formOnlyShowdownId()) : "";
        String combinedSpeciesName = speciesName + "-" + formName;
        String combinedShowdownId = speciesShowdownId + "-" + (formOnlyShowdownId.isBlank() ? formShowdownId : formOnlyShowdownId);

        addAlias(aliases, speciesName);
        addAlias(aliases, speciesShowdownId);
        addAlias(aliases, speciesIdentifier);
        addAlias(aliases, formName);
        addAlias(aliases, formShowdownId);
        addAlias(aliases, formOnlyShowdownId);
        addAlias(aliases, combinedSpeciesName);
        addAlias(aliases, combinedShowdownId);
        addAlias(aliases, pokemon.showdownId());

        return aliases;
    }

    private static void addAlias(Set<String> aliases, String alias) {
        String normalized = normalize(alias);
        if (!normalized.isBlank()) {
            aliases.add(normalized);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
