package com.bigbang_tournaments.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeamPreviewConfigTest {

    @Test
    void defaultValues() {
        TeamPreviewConfig config = new TeamPreviewConfig();
        assertEquals(60, config.getDurationSeconds());
        assertEquals("FIRST_FOUR", config.getAutoSelectStrategy());
        assertTrue(config.isRevealSpecies());
        assertTrue(config.isRevealHeldItems());
        assertTrue(config.isRevealAbilities());
    }

    @Test
    void tournamentConfigIncludesTeamPreview() {
        TournamentConfig config = new TournamentConfig();
        TeamPreviewConfig preview = config.getTeamPreview();
        assertNotNull(preview);
        assertEquals(60, preview.getDurationSeconds());
    }

    @Test
    void configPropertiesAreMutable() {
        TeamPreviewConfig config = new TeamPreviewConfig();
        config.setDurationSeconds(120);
        config.setAutoSelectStrategy("RANDOM");
        config.setRevealSpecies(false);
        config.setRevealHeldItems(false);
        config.setRevealAbilities(false);

        assertEquals(120, config.getDurationSeconds());
        assertEquals("RANDOM", config.getAutoSelectStrategy());
        assertFalse(config.isRevealSpecies());
        assertFalse(config.isRevealHeldItems());
        assertFalse(config.isRevealAbilities());
    }

    @Test
    void tournamentConfigStoresTeamPreview() {
        TournamentConfig config = new TournamentConfig();
        config.getTeamPreview().setDurationSeconds(90);
        assertEquals(90, config.getTeamPreview().getDurationSeconds());
    }
}
