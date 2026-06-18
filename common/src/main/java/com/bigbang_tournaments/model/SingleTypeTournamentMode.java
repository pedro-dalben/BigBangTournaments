package com.bigbang_tournaments.model;

import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SingleTypeTournamentMode implements TournamentMode {
    @Override
    public String id() {
        return "singletype";
    }

    @Override
    public String displayName() {
        return "Guerra dos Ginásios";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("singleelement", "monotype");
    }

    @Override
    public boolean requiresElementAssignment() {
        return true;
    }

    @Override
    public EffectiveTournamentRules resolveRules(
            TournamentConfig globalConfig,
            TournamentState tournamentState) {
        return new EffectiveTournamentRules(
                true, // banLegendaries (always forbidden)
                true, // banMythicals (always forbidden)
                globalConfig.getBannedSpecies(),
                globalConfig.getBannedItems(),
                globalConfig.isItemClauseEnabled(),
                globalConfig.isSpeciesClauseEnabled(),
                false, // allowMega (always forbidden)
                true,  // allowTera (always allowed)
                false, // allowDynamax (always forbidden)
                false, // allowZMove (always forbidden)
                globalConfig.isSingleSpecialMechanicPerTeam()
        );
    }

    @Override
    public TournamentTeamValidationResult analyzeTeam(TournamentValidationContext context) {
        List<TournamentRuleViolation> violations = new ArrayList<>();
        TournamentParticipantRecord record = context.getParticipantRecord();
        if (record == null) {
            violations.add(new TournamentRuleViolation(
                    TournamentRuleViolationType.INVALID_TEAM_COMPOSITION,
                    "Elemento do participante nao encontrado ou nao definido.",
                    "gym_type"
            ));
            return new TournamentTeamValidationResult(violations, null, null, null);
        }

        String gymType = getGymEnglishType(record);
        if (gymType == null) {
            violations.add(new TournamentRuleViolation(
                    TournamentRuleViolationType.INVALID_TEAM_COMPOSITION,
                    "Elemento do participante nao encontrado ou nao definido.",
                    "gym_type"
            ));
            return new TournamentTeamValidationResult(violations, null, null, null);
        }

        Collection<Pokemon> party = context.getParty();
        if (party.size() != 6) {
            violations.add(new TournamentRuleViolation(
                    TournamentRuleViolationType.INVALID_TEAM_COMPOSITION,
                    "O time deve possuir exatamente 6 Pokemon.",
                    "party_size"
            ));
            return new TournamentTeamValidationResult(violations, null, null, null);
        }

        List<Pokemon> nonMatching = new ArrayList<>();
        for (Pokemon pokemon : party) {
            boolean hasGymType = false;
            if (pokemon.getPrimaryType() != null && pokemon.getPrimaryType().getName().equalsIgnoreCase(gymType)) {
                hasGymType = true;
            }
            if (pokemon.getSecondaryType() != null && pokemon.getSecondaryType().getName().equalsIgnoreCase(gymType)) {
                hasGymType = true;
            }
            if (!hasGymType) {
                nonMatching.add(pokemon);
            }
        }

        String compositionMode = null;
        java.util.UUID jokerPokemonUuid = null;
        String jokerSpeciesName = null;

        if (nonMatching.isEmpty()) {
            compositionMode = "MONOTYPE";
        } else if (nonMatching.size() == 1) {
            Pokemon joker = nonMatching.get(0);
            String teraType = joker.getTeraType() != null ? joker.getTeraType().getName() : "";
            String jokerName = joker.getSpecies() != null ? joker.getSpecies().getName() : "Unknown";
            if (!teraType.equalsIgnoreCase(gymType)) {
                violations.add(new TournamentRuleViolation(
                        TournamentRuleViolationType.INVALID_TEAM_COMPOSITION,
                        "O Coringa (" + jokerName + ") deve possuir o Tera Type igual ao tipo do ginasio (" + gymType + "). Encontrado: " + teraType + ".",
                        jokerName
                ));
            } else {
                compositionMode = "JOKER";
                jokerPokemonUuid = joker.getUuid();
                jokerSpeciesName = jokerName;
            }
        } else {
            violations.add(new TournamentRuleViolation(
                    TournamentRuleViolationType.INVALID_TEAM_COMPOSITION,
                    "O time possui mais de um Pokemon fora do tipo do ginasio: " + formatPokemonNames(nonMatching) + ".",
                    "too_many_non_matching"
            ));
        }

        return new TournamentTeamValidationResult(violations, compositionMode, jokerPokemonUuid, jokerSpeciesName);
    }

    private String getGymEnglishType(TournamentParticipantRecord record) {
        if (record == null || record.getAssignedElement() == null) {
            return null;
        }
        String cleaned = cleanElement(record.getAssignedElement());
        return mapToEnglishType(cleaned);
    }

    private String cleanElement(String element) {
        if (element == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(element, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private String mapToEnglishType(String cleanElement) {
        if (cleanElement == null) return "";
        switch (cleanElement.toLowerCase()) {
            case "agua": return "water";
            case "fogo": return "fire";
            case "planta": return "grass";
            case "eletrico": return "electric";
            case "aco": return "steel";
            case "fantasma": return "ghost";
            case "fada": return "fairy";
            case "dragao": return "dragon";
            case "venenoso": return "poison";
            case "psiquico": return "psychic";
            case "lutador": return "fighting";
            case "sombrio": return "dark";
            case "terra": return "ground";
            default: return cleanElement.toLowerCase();
        }
    }

    private String formatPokemonNames(List<Pokemon> pokemons) {
        List<String> names = new ArrayList<>();
        for (Pokemon p : pokemons) {
            names.add(p.getSpecies() != null ? p.getSpecies().getName() : "Unknown");
        }
        return String.join(", ", names);
    }
}
