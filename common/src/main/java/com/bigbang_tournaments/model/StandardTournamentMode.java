package com.bigbang_tournaments.model;

import java.util.Collections;
import java.util.Set;

public class StandardTournamentMode implements TournamentMode {
    @Override
    public String id() {
        return "standard";
    }

    @Override
    public String displayName() {
        return "Campeonato Padrão";
    }

    @Override
    public Set<String> aliases() {
        return Collections.emptySet();
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
