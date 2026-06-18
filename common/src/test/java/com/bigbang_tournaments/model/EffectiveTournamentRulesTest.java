package com.bigbang_tournaments.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EffectiveTournamentRulesTest {

    @Test
    void copiesGlobalListsInsteadOfSharingReferences() {
        TournamentConfig config = new TournamentConfig();
        List<String> bannedSpecies = new ArrayList<>();
        bannedSpecies.add("mewtwo");
        List<String> bannedItems = new ArrayList<>();
        bannedItems.add("choice-scarf");
        config.setBannedSpecies(bannedSpecies);
        config.setBannedItems(bannedItems);

        EffectiveTournamentRules rules = new StandardTournamentMode().resolveRules(config, new TournamentState());

        assertEquals(List.of("mewtwo"), rules.getBannedSpecies());
        assertEquals(List.of("choice-scarf"), rules.getBannedItems());

        bannedSpecies.add("rayquaza");
        bannedItems.add("leftovers");

        assertEquals(List.of("mewtwo"), rules.getBannedSpecies());
        assertEquals(List.of("choice-scarf"), rules.getBannedItems());
    }
}
