package com.bigbang_tournaments.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TournamentBattleSessionStateMachineTest {

    private TournamentBattleSession createSession() {
        return new TournamentBattleSession(
                UUID.randomUUID(), "player1",
                UUID.randomUUID(), "player2",
                "regulation_i_doubles"
        );
    }

    @Test
    void initialStateIsCreated() {
        TournamentBattleSession session = createSession();
        assertEquals(TournamentBattleStatus.CREATED, session.getState());
        assertFalse(session.getState().isTerminal());
        assertTrue(session.getState().isPreviewActive());
        assertFalse(session.getState().isPartyModified());
        assertFalse(session.getState().isPartyLocked());
    }

    @Test
    void validTransitionFromCreatedToTeamPreview() {
        TournamentBattleSession session = createSession();
        assertTrue(session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW));
        assertEquals(TournamentBattleStatus.TEAM_PREVIEW, session.getState());
        assertTrue(session.getState().isPreviewActive());
        assertTrue(session.getState().isPartyLocked());
    }

    @Test
    void cannotTransitionFromTeamPreviewToCreated() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        assertFalse(session.transitionTo(TournamentBattleStatus.CREATED));
    }

    @Test
    void validTransitionsForPlayerSelection() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);

        assertTrue(session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED));
        assertEquals(TournamentBattleStatus.PLAYER_ONE_SELECTED, session.getState());

        assertTrue(session.transitionTo(TournamentBattleStatus.PLAYER_TWO_SELECTED));
        assertEquals(TournamentBattleStatus.PLAYER_TWO_SELECTED, session.getState());
    }

    @Test
    void canGoDirectlyToPreparingPartiesFromPlayerTwoSelected() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED);
        session.transitionTo(TournamentBattleStatus.PLAYER_TWO_SELECTED);
        assertTrue(session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES));
        assertEquals(TournamentBattleStatus.PREPARING_PARTIES, session.getState());
        assertTrue(session.getState().isPartyModified());
        assertTrue(session.getState().isPartyLocked());
    }

    @Test
    void partiesSwappedAfterPreparingParties() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED);
        session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES);
        assertTrue(session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED));
        assertEquals(TournamentBattleStatus.PARTIES_SWAPPED, session.getState());
    }

    @Test
    void fullHappyPathToActive() {
        TournamentBattleSession session = createSession();
        assertTrue(session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW));
        session.setPlayerOneSelection(java.util.List.of(1, 2, 3, 4));
        session.setPlayerTwoSelection(java.util.List.of(1, 2, 3, 4));
        assertTrue(session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED));
        assertTrue(session.transitionTo(TournamentBattleStatus.PLAYER_TWO_SELECTED));
        assertTrue(session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES));
        assertTrue(session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED));
        assertTrue(session.transitionTo(TournamentBattleStatus.BATTLE_STARTING));
        assertTrue(session.transitionTo(TournamentBattleStatus.COUNTDOWN));
        assertTrue(session.transitionTo(TournamentBattleStatus.ACTIVE));
        assertEquals(TournamentBattleStatus.ACTIVE, session.getState());
    }

    @Test
    void terminalStateRestored() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED);
        session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES);
        session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED);
        session.transitionTo(TournamentBattleStatus.RESTORE_PENDING);
        session.transitionTo(TournamentBattleStatus.RESTORING);
        assertTrue(session.transitionTo(TournamentBattleStatus.RESTORED));
        assertTrue(session.getState().isTerminal());
        assertFalse(session.getState().isPartyLocked());
    }

    @Test
    void cannotTransitionFromTerminalState() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.CANCELLED);
        assertTrue(session.getState().isTerminal());
        assertFalse(session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW));
        assertFalse(session.transitionTo(TournamentBattleStatus.ACTIVE));
    }

    @Test
    void invalidTransitionsReturnFalseAndPreserveState() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);

        assertFalse(session.transitionTo(TournamentBattleStatus.COUNTDOWN));
        assertEquals(TournamentBattleStatus.TEAM_PREVIEW, session.getState());

        assertFalse(session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED));
        assertEquals(TournamentBattleStatus.TEAM_PREVIEW, session.getState());

        assertFalse(session.transitionTo(TournamentBattleStatus.ACTIVE));
        assertEquals(TournamentBattleStatus.TEAM_PREVIEW, session.getState());
    }

    @Test
    void cannotSwapPartiesTwice() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED);
        session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES);
        assertTrue(session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED));
        assertFalse(session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED));
    }

    @Test
    void cannotRestoreTwice() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED);
        session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES);
        session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED);
        session.transitionTo(TournamentBattleStatus.RESTORE_PENDING);
        session.transitionTo(TournamentBattleStatus.RESTORING);
        assertTrue(session.transitionTo(TournamentBattleStatus.RESTORED));
        assertFalse(session.transitionTo(TournamentBattleStatus.RESTORE_PENDING));
        assertFalse(session.transitionTo(TournamentBattleStatus.RESTORING));
        assertFalse(session.transitionTo(TournamentBattleStatus.RESTORED));
    }

    @Test
    void tryFinalizeFromActiveReturnsTrueAndTransitionsToRestorePending() {
        TournamentBattleSession session = createSession();
        runToActive(session);
        assertTrue(session.tryFinalize("test_reason"));
        assertEquals(TournamentBattleStatus.RESTORE_PENDING, session.getState());
        assertEquals("test_reason", session.getFinalizationReason());
    }

    @Test
    void tryFinalizeFromRestoredReturnsFalse() {
        TournamentBattleSession session = createSession();
        runToActive(session);
        session.tryFinalize("reason1");
        session.transitionTo(TournamentBattleStatus.RESTORING);
        session.transitionTo(TournamentBattleStatus.RESTORED);
        assertFalse(session.tryFinalize("reason2"));
        assertTrue(session.getState().isTerminal());
    }

    @Test
    void tryCancelFromPreview() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        assertTrue(session.tryCancel());
        assertEquals(TournamentBattleStatus.CANCELLED, session.getState());
    }

    @Test
    void tryCancelAfterPartyModificationGoesToRestorePending() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED);
        session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES);
        session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED);
        assertTrue(session.tryCancel());
        assertEquals(TournamentBattleStatus.RESTORE_PENDING, session.getState());
    }

    @Test
    void bothSelectedReturnsTrueWhenBothSelectionsArePresent() {
        TournamentBattleSession session = createSession();
        assertFalse(session.bothSelected());
        session.setPlayerOneSelection(java.util.List.of(1, 2, 3, 4));
        assertFalse(session.bothSelected());
        session.setPlayerTwoSelection(java.util.List.of(1, 2, 3, 4));
        assertTrue(session.bothSelected());
    }

    @Test
    void invalidTransitionFromFailedToRestoredIsNotAllowed() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.FAILED);
        assertTrue(session.transitionTo(TournamentBattleStatus.RESTORE_PENDING));
    }

    @Test
    void terminalStatesCoverAllEndings() {
        assertTrue(TournamentBattleStatus.RESTORED.isTerminal());
        assertTrue(TournamentBattleStatus.CANCELLED.isTerminal());
        assertTrue(TournamentBattleStatus.FINISHED.isTerminal());
        assertFalse(TournamentBattleStatus.FAILED.isTerminal());
        assertFalse(TournamentBattleStatus.ACTIVE.isTerminal());
    }

    private void runToActive(TournamentBattleSession session) {
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED);
        session.transitionTo(TournamentBattleStatus.PLAYER_TWO_SELECTED);
        session.transitionTo(TournamentBattleStatus.PREPARING_PARTIES);
        session.transitionTo(TournamentBattleStatus.PARTIES_SWAPPED);
        session.transitionTo(TournamentBattleStatus.BATTLE_STARTING);
        session.transitionTo(TournamentBattleStatus.COUNTDOWN);
        session.transitionTo(TournamentBattleStatus.ACTIVE);
    }

    @Test
    void activeStatusIsBattleActive() {
        assertTrue(TournamentBattleStatus.BATTLE_STARTING.isBattleActive());
        assertTrue(TournamentBattleStatus.COUNTDOWN.isBattleActive());
        assertTrue(TournamentBattleStatus.ACTIVE.isBattleActive());
        assertFalse(TournamentBattleStatus.TEAM_PREVIEW.isBattleActive());
    }

    @Test
    void previewStatusIsPreviewActive() {
        assertTrue(TournamentBattleStatus.CREATED.isPreviewActive());
        assertTrue(TournamentBattleStatus.TEAM_PREVIEW.isPreviewActive());
        assertTrue(TournamentBattleStatus.PLAYER_ONE_SELECTED.isPreviewActive());
        assertTrue(TournamentBattleStatus.PLAYER_TWO_SELECTED.isPreviewActive());
        assertFalse(TournamentBattleStatus.ACTIVE.isPreviewActive());
    }

    @Test
    void partyModifiedStates() {
        assertTrue(TournamentBattleStatus.PREPARING_PARTIES.isPartyModified());
        assertTrue(TournamentBattleStatus.PARTIES_SWAPPED.isPartyModified());
        assertTrue(TournamentBattleStatus.BATTLE_STARTING.isPartyModified());
        assertTrue(TournamentBattleStatus.ACTIVE.isPartyModified());
        assertTrue(TournamentBattleStatus.RESTORE_PENDING.isPartyModified());
        assertTrue(TournamentBattleStatus.RESTORING.isPartyModified());
        assertTrue(TournamentBattleStatus.FAILED.isPartyModified());
        assertFalse(TournamentBattleStatus.TEAM_PREVIEW.isPartyModified());
    }

    @Test
    void transitionFromTeamPreviewToBothSelectedStates() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        assertTrue(session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED));
    }

    @Test
    void transitionFromPlayerTwoSelectedBackToPlayerOneSelected() {
        TournamentBattleSession session = createSession();
        session.transitionTo(TournamentBattleStatus.TEAM_PREVIEW);
        session.transitionTo(TournamentBattleStatus.PLAYER_TWO_SELECTED);
        assertTrue(session.transitionTo(TournamentBattleStatus.PLAYER_ONE_SELECTED));
    }

    @Test
    void transitionFromActiveToManualResultRequired() {
        TournamentBattleSession session = createSession();
        runToActive(session);
        assertTrue(session.transitionTo(TournamentBattleStatus.MANUAL_RESULT_REQUIRED));
    }

    @Test
    void transitionFromManualResultRequiredToFinished() {
        TournamentBattleSession session = createSession();
        runToActive(session);
        session.transitionTo(TournamentBattleStatus.MANUAL_RESULT_REQUIRED);
        assertTrue(session.transitionTo(TournamentBattleStatus.FINISHED));
    }

    @Test
    void transitionToSameStateReturnsFalse() {
        TournamentBattleSession session = createSession();
        assertFalse(session.transitionTo(TournamentBattleStatus.CREATED));
    }
}
