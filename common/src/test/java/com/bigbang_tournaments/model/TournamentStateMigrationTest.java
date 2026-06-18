package com.bigbang_tournaments.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TournamentStateMigrationTest {

    @Test
    void defaultsMissingTournamentTypeToStandard() {
        TournamentState state = new TournamentState();
        state.setTournamentType(null);

        assertTrue(state.normalizeAndMigrate());
        assertEquals("standard", state.getTournamentType());
        assertEquals("SCHEDULED", state.getTournamentPhase());
    }

    @Test
    void migratesLegacyAliasesToSingletype() {
        TournamentState state = new TournamentState();
        state.setTournamentType("monotype");

        assertTrue(state.normalizeAndMigrate());
        assertEquals("singletype", state.getTournamentType());
    }

    @Test
    void preservesPreparationStatusWhileMigratingCheckInStatusSeparately() {
        TournamentParticipantRecord preparedParticipant = new TournamentParticipantRecord();
        preparedParticipant.setStatus(TournamentParticipantStatus.PREPARED);

        TournamentParticipantRecord checkedInParticipant = new TournamentParticipantRecord();
        checkedInParticipant.setStatus(TournamentParticipantStatus.CHECKED_IN);

        TournamentState state = new TournamentState();
        state.setParticipants(List.of(preparedParticipant, checkedInParticipant));

        assertTrue(state.normalizeAndMigrate());

        assertEquals(TournamentParticipantStatus.PREPARED, preparedParticipant.getStatus());
        assertEquals(TournamentCheckInStatus.NOT_STARTED, preparedParticipant.getCheckInStatus());

        assertEquals(TournamentParticipantStatus.CHECKED_IN, checkedInParticipant.getStatus());
        assertEquals(TournamentCheckInStatus.CHECKED_IN, checkedInParticipant.getCheckInStatus());
    }
}
