package com.bigbang_tournaments.model;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TournamentTeamValidationResult {
    private final List<TournamentRuleViolation> violations;
    private final String compositionMode;
    private final UUID jokerPokemonUuid;
    private final String jokerSpeciesName;

    public TournamentTeamValidationResult(
            List<TournamentRuleViolation> violations,
            String compositionMode,
            UUID jokerPokemonUuid,
            String jokerSpeciesName) {
        this.violations = violations == null ? List.of() : List.copyOf(violations);
        this.compositionMode = compositionMode;
        this.jokerPokemonUuid = jokerPokemonUuid;
        this.jokerSpeciesName = jokerSpeciesName;
    }

    public static TournamentTeamValidationResult empty() {
        return new TournamentTeamValidationResult(Collections.emptyList(), null, null, null);
    }

    public List<TournamentRuleViolation> getViolations() {
        return violations;
    }

    public String getCompositionMode() {
        return compositionMode;
    }

    public UUID getJokerPokemonUuid() {
        return jokerPokemonUuid;
    }

    public String getJokerSpeciesName() {
        return jokerSpeciesName;
    }

    public boolean isValid() {
        return violations.isEmpty();
    }
}
