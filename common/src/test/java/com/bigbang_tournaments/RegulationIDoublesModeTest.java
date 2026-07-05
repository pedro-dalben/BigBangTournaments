package com.bigbang_tournaments;

import com.bigbang_tournaments.model.*;
import com.bigbang_tournaments.service.TournamentModeRegistry;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RegulationIDoublesModeTest {

    @Test
    public void testRegistryResolvesRegulationIDoubles() {
        TournamentMode mode = TournamentModeRegistry.resolve("regulation_i_doubles");
        assertNotNull(mode);
        assertEquals("regulation_i_doubles", mode.id());
        assertEquals("VGC Doubles — Regulation I", mode.displayName());
        assertFalse(mode.requiresElementAssignment());

        // Test aliases
        assertEquals("regulation_i_doubles", TournamentModeRegistry.resolve("regulation_i").id());
        assertEquals("regulation_i_doubles", TournamentModeRegistry.resolve("vgc_doubles").id());
        assertEquals("regulation_i_doubles", TournamentModeRegistry.resolve("vgc_reg_i").id());
    }

    @Test
    public void testRulesResolution() {
        RegulationIPreset preset = new RegulationIPreset();
        preset.setTeraEnabled(true);
        preset.setBannedMythicals(List.of("pecharunt", "mew"));
        preset.setBannedItems(List.of("choice-specs"));

        RegulationIDoublesMode mode = new RegulationIDoublesMode(preset);
        EffectiveTournamentRules rules = mode.resolveRules(new TournamentConfig(), new TournamentState());

        assertFalse(rules.isBanLegendaries());
        assertFalse(rules.isBanMythicals());
        assertEquals(List.of(), rules.getBannedSpecies());
        assertEquals(List.of("choice-specs"), rules.getBannedItems());
        assertTrue(rules.isItemClauseEnabled());
        assertTrue(rules.isSpeciesClauseEnabled());
        assertFalse(rules.isAllowMega());
        assertTrue(rules.isAllowTera());
        assertTrue(rules.isAllowDynamax());
        assertFalse(rules.isAllowZMove());
        assertTrue(rules.isSingleSpecialMechanicPerTeam());
    }

    @Test
    public void testRulesResolutionIgnoresGlobalLegendaryBlacklist() {
        RegulationIPreset preset = new RegulationIPreset();
        RegulationIDoublesMode mode = new RegulationIDoublesMode(preset);

        TournamentConfig config = new TournamentConfig();
        config.setBanLegendaries(true);
        config.setBanMythicals(true);
        config.setBannedSpecies(List.of("miraidon", "calyrex"));

        EffectiveTournamentRules rules = mode.resolveRules(config, new TournamentState());

        assertFalse(rules.isBanLegendaries());
        assertFalse(rules.isBanMythicals());
        assertTrue(rules.getBannedSpecies().isEmpty());
    }
}
