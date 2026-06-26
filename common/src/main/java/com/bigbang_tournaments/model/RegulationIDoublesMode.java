package com.bigbang_tournaments.model;

import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.*;

public class RegulationIDoublesMode implements TournamentMode {
    private final RegulationIPreset preset;

    public RegulationIDoublesMode(RegulationIPreset preset) {
        this.preset = preset;
    }

    @Override
    public String id() {
        return "regulation_i_doubles";
    }

    @Override
    public String displayName() {
        return "VGC Doubles — Regulation I";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("regulation_i", "vgc_doubles", "vgc_reg_i");
    }

    @Override
    public boolean requiresElementAssignment() {
        return false;
    }

    @Override
    public EffectiveTournamentRules resolveRules(
            TournamentConfig globalConfig,
            TournamentState tournamentState) {
        // Under Regulation I: Mega, Dynamax, Gigantamax, Z-Moves are disabled.
        // Terastallization feature flag comes from the preset/config.
        return new EffectiveTournamentRules(
                true, // banLegendaries (always forbidden under Regulation I rules, except restricted but they are Mythicals/Restricted and banned species check takes care of them)
                true, // banMythicals (always forbidden)
                preset.getBannedMythicals(), // We will validate restricted and mythicals, let's pass banned species list
                preset.getBannedItems(),
                true, // Item clause: enabled
                true, // Species clause: enabled
                false, // Mega: disabled
                preset.isTeraEnabled(), // Tera: feature flag
                false, // Dynamax: disabled
                false, // Z-Move: disabled
                true // Single special mechanic per team: enabled
        );
    }

    @Override
    public TournamentTeamValidationResult analyzeTeam(TournamentValidationContext context) {
        List<TournamentRuleViolation> violations = new ArrayList<>();
        Collection<Pokemon> party = context.getParty();

        // Standard competitive team check
        if (party.size() != 6) {
            violations.add(new TournamentRuleViolation(
                    TournamentRuleViolationType.INVALID_TEAM_COMPOSITION,
                    "[Campeonato] Time registrado deve conter exatamente 6 Pokemon.",
                    "party_size"
            ));
            return new TournamentTeamValidationResult(violations, null, null, null);
        }

        // Validate species limit / Mythical / Restricted counts
        int restrictedCount = 0;
        List<String> foundRestricted = new ArrayList<>();
        List<String> seenSpecies = new ArrayList<>();
        List<String> seenItems = new ArrayList<>();

        for (Pokemon pokemon : party) {
            if (pokemon == null) {
                continue;
            }

            String speciesName = pokemon.getSpecies() != null ? pokemon.getSpecies().getName() : "Unknown";
            String normalizedSpeciesName = speciesName.toLowerCase(Locale.ROOT);

            // Canonical species identification for species clause
            if (seenSpecies.contains(normalizedSpeciesName)) {
                violations.add(new TournamentRuleViolation(
                        TournamentRuleViolationType.DUPLICATED_SPECIES,
                        "[Campeonato] Pokemon repetido detectado no time: " + speciesName + ".",
                        speciesName
                ));
            } else {
                seenSpecies.add(normalizedSpeciesName);
            }

            // Item clause
            net.minecraft.world.item.ItemStack heldItem = pokemon.heldItem();
            String heldItemId = com.bigbang_tournaments.service.PokemonTeamService.getHeldItemId(heldItem);
            if (!heldItemId.isBlank()) {
                String normalizedItemId = heldItemId.toLowerCase(Locale.ROOT);
                if (seenItems.contains(normalizedItemId)) {
                    violations.add(new TournamentRuleViolation(
                            TournamentRuleViolationType.DUPLICATED_HELD_ITEM,
                            "[Campeonato] Item repetido detectado no time: " + heldItemId + ".",
                            heldItemId
                    ));
                } else {
                    seenItems.add(normalizedItemId);
                }
            }

            // Mythicals ban check
            boolean isMythical = false;
            for (String mythical : preset.getBannedMythicals()) {
                if (normalizedSpeciesName.startsWith(mythical.toLowerCase(Locale.ROOT))) {
                    isMythical = true;
                    break;
                }
            }

            if (isMythical) {
                violations.add(new TournamentRuleViolation(
                        TournamentRuleViolationType.BANNED_MYTHICAL,
                        "[Campeonato] " + speciesName + " é Mythical e não é permitido no Regulation I.",
                        speciesName
                ));
            }

            // Restricted check
            boolean isRestricted = false;
            for (String restricted : preset.getRestrictedSpecies()) {
                if (normalizedSpeciesName.startsWith(restricted.toLowerCase(Locale.ROOT))) {
                    isRestricted = true;
                    break;
                }
            }

            if (isRestricted) {
                restrictedCount++;
                foundRestricted.add(speciesName);
            }

            // Eligible check: allowed species
            boolean isAllowed = false;
            for (String allowed : preset.getAllowedSpecies()) {
                if (normalizedSpeciesName.startsWith(allowed.toLowerCase(Locale.ROOT))) {
                    isAllowed = true;
                    break;
                }
            }

            if (!isAllowed && !isMythical && !isRestricted) {
                // If it is not explicitly allowed (HOME/Events, etc.), report it
                violations.add(new TournamentRuleViolation(
                        TournamentRuleViolationType.BANNED_SPECIES,
                        "[Campeonato] " + speciesName + " não é permitido no Regulation I.",
                        speciesName
                ));
            }
        }

        // Restricted count check
        if (restrictedCount > 2) {
            violations.add(new TournamentRuleViolation(
                    TournamentRuleViolationType.INVALID_TEAM_COMPOSITION,
                    "[Campeonato] Seu time possui " + restrictedCount + " Pokémon Restricted: " + String.join(", ", foundRestricted) + ". O limite é 2.",
                    "restricted_limit"
            ));
        }

        return new TournamentTeamValidationResult(violations, "REGULATION_I_DOUBLES", null, null);
    }
}
