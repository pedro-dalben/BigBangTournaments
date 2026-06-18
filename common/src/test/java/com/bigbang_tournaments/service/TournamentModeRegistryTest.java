package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TournamentModeRegistryTest {

    @Test
    void resolvesCanonicalModesAndAliases() {
        TournamentMode standard = TournamentModeRegistry.resolve("standard");
        assertEquals("standard", standard.id());

        TournamentMode singleType = TournamentModeRegistry.resolve("singletype");
        assertEquals("singletype", singleType.id());

        assertEquals("singletype", TournamentModeRegistry.resolve("monotype").id());
        assertEquals("singletype", TournamentModeRegistry.resolve("singleelement").id());
    }

    @Test
    void fallsBackToStandardForUnknownOrNullTypes() {
        assertEquals("standard", TournamentModeRegistry.resolve(null).id());
        assertEquals("standard", TournamentModeRegistry.resolve("unknown").id());
    }

    @Test
    void recognizesValidTypes() {
        assertTrue(TournamentModeRegistry.isValidType("standard"));
        assertTrue(TournamentModeRegistry.isValidType("singletype"));
        assertTrue(TournamentModeRegistry.isValidType("monotype"));
        assertTrue(TournamentModeRegistry.isValidType("singleelement"));
        assertFalse(TournamentModeRegistry.isValidType("unknown"));
    }
}
