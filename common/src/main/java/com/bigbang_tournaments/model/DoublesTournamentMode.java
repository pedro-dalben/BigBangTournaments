package com.bigbang_tournaments.model;

import java.util.Collections;
import java.util.Set;

public class DoublesTournamentMode implements TournamentMode {
    @Override
    public String id() {
        return "doubles";
    }

    @Override
    public String displayName() {
        return "Campeonato em Dupla";
    }

    @Override
    public Set<String> aliases() {
        return Set.of("2v2", "duplas");
    }

    @Override
    public boolean requiresElementAssignment() {
        return false;
    }

    @Override
    public EffectiveTournamentRules resolveRules(
            TournamentConfig globalConfig,
            TournamentState tournamentState) {
        return new EffectiveTournamentRules(
                globalConfig.isBanLegendaries(),
                globalConfig.isBanMythicals(),
                globalConfig.getBannedSpecies(),
                globalConfig.getBannedItems(),
                globalConfig.isItemClauseEnabled(),
                globalConfig.isSpeciesClauseEnabled(),
                globalConfig.isAllowMega(),
                globalConfig.isAllowTera(),
                globalConfig.isAllowDynamax(),
                globalConfig.isAllowZMove(),
                globalConfig.isSingleSpecialMechanicPerTeam()
        );
    }

    @Override
    public TournamentTeamValidationResult analyzeTeam(TournamentValidationContext context) {
        return TournamentTeamValidationResult.empty();
    }
}
