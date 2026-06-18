package com.bigbang_tournaments.model;

import java.util.List;
import java.util.Set;

public interface TournamentMode {
    String id();

    String displayName();

    Set<String> aliases();

    boolean requiresElementAssignment();

    EffectiveTournamentRules resolveRules(
            TournamentConfig globalConfig,
            TournamentState tournamentState
    );

    default TournamentTeamValidationResult analyzeTeam(
            TournamentValidationContext context
    ) {
        return TournamentTeamValidationResult.empty();
    }

    default List<TournamentRuleViolation> validateTeam(
            TournamentValidationContext context
    ) {
        return analyzeTeam(context).getViolations();
    }
}
