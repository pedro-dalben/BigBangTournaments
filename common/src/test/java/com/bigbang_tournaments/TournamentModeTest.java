package com.bigbang_tournaments;

import com.bigbang_tournaments.model.TournamentConfig;
import com.bigbang_tournaments.model.TournamentMode;
import com.bigbang_tournaments.model.TournamentState;
import com.bigbang_tournaments.model.EffectiveTournamentRules;
import com.bigbang_tournaments.service.TournamentModeRegistry;
import com.bigbang_tournaments.service.TournamentStateService;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TournamentModeTest {

    @Test
    public void testRegistryResolvesCorrectly() {
        // Test standard resolution
        TournamentMode standard = TournamentModeRegistry.resolve("standard");
        assertNotNull(standard);
        assertEquals("standard", standard.id());
        assertEquals("Campeonato Padrão", standard.displayName());
        assertFalse(standard.requiresElementAssignment());

        // Test singletype resolution
        TournamentMode singletype = TournamentModeRegistry.resolve("singletype");
        assertNotNull(singletype);
        assertEquals("singletype", singletype.id());
        assertEquals("Guerra dos Ginásios", singletype.displayName());
        assertTrue(singletype.requiresElementAssignment());

        // Test aliases
        TournamentMode monotype = TournamentModeRegistry.resolve("monotype");
        assertEquals("singletype", monotype.id());

        TournamentMode singleelement = TournamentModeRegistry.resolve("singleelement");
        assertEquals("singletype", singleelement.id());

        // Test unknown fallback
        TournamentMode unknown = TournamentModeRegistry.resolve("unknown_mode");
        assertEquals("standard", unknown.id());

        // Test null/empty fallback
        TournamentMode nullMode = TournamentModeRegistry.resolve(null);
        assertEquals("standard", nullMode.id());

        TournamentMode emptyMode = TournamentModeRegistry.resolve("");
        assertEquals("standard", emptyMode.id());
    }

    @Test
    public void testRegistryIsValidType() {
        assertTrue(TournamentModeRegistry.isValidType("standard"));
        assertTrue(TournamentModeRegistry.isValidType("singletype"));
        assertTrue(TournamentModeRegistry.isValidType("monotype"));
        assertTrue(TournamentModeRegistry.isValidType("singleelement"));
        assertFalse(TournamentModeRegistry.isValidType("unknown_mode"));
        assertFalse(TournamentModeRegistry.isValidType(null));
    }

    @Test
    public void testStandardModeRulesResolution() {
        TournamentMode standardMode = TournamentModeRegistry.resolve("standard");
        TournamentConfig config = new TournamentConfig();
        config.setBanLegendaries(false);
        config.setBanMythicals(true);
        config.setAllowMega(false);
        config.setAllowTera(true);

        TournamentState state = new TournamentState();
        EffectiveTournamentRules rules = standardMode.resolveRules(config, state);

        assertFalse(rules.isBanLegendaries());
        assertTrue(rules.isBanMythicals());
        assertFalse(rules.isAllowMega());
        assertTrue(rules.isAllowTera());
    }

    @Test
    public void testSingleTypeModeRulesResolution() {
        TournamentMode singleTypeMode = TournamentModeRegistry.resolve("singletype");
        TournamentConfig config = new TournamentConfig();
        // Set all to true globally, to check if they are overridden correctly
        config.setBanLegendaries(false);
        config.setBanMythicals(false);
        config.setAllowMega(true);
        config.setAllowTera(false); // will be overridden to true
        config.setAllowDynamax(true);
        config.setAllowZMove(true);

        TournamentState state = new TournamentState();
        EffectiveTournamentRules rules = singleTypeMode.resolveRules(config, state);

        // Overridden values for singletype
        assertTrue(rules.isBanLegendaries());
        assertTrue(rules.isBanMythicals());
        assertFalse(rules.isAllowMega());
        assertTrue(rules.isAllowTera());
        assertFalse(rules.isAllowDynamax());
        assertFalse(rules.isAllowZMove());
    }

}
