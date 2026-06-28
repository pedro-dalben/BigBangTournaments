package com.bigbang_tournaments.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TournamentBattleStatusTest {

    @Test
    void terminalStatesAreCorrect() {
        assertTrue(TournamentBattleStatus.RESTORED.isTerminal());
        assertTrue(TournamentBattleStatus.CANCELLED.isTerminal());
        assertTrue(TournamentBattleStatus.FINISHED.isTerminal());
        assertFalse(TournamentBattleStatus.ACTIVE.isTerminal());
        assertFalse(TournamentBattleStatus.FAILED.isTerminal());
        assertFalse(TournamentBattleStatus.RESTORE_PENDING.isTerminal());
    }

    @Test
    void battleActiveStatesAreCorrect() {
        assertTrue(TournamentBattleStatus.BATTLE_STARTING.isBattleActive());
        assertTrue(TournamentBattleStatus.COUNTDOWN.isBattleActive());
        assertTrue(TournamentBattleStatus.ACTIVE.isBattleActive());
        assertFalse(TournamentBattleStatus.TEAM_PREVIEW.isBattleActive());
        assertFalse(TournamentBattleStatus.RESTORED.isBattleActive());
    }

    @Test
    void partyLockedStatesAreCorrect() {
        assertTrue(TournamentBattleStatus.TEAM_PREVIEW.isPartyLocked());
        assertTrue(TournamentBattleStatus.ACTIVE.isPartyLocked());
        assertTrue(TournamentBattleStatus.RESTORE_PENDING.isPartyLocked());
        assertTrue(TournamentBattleStatus.RESTORING.isPartyLocked());
        assertFalse(TournamentBattleStatus.RESTORED.isPartyLocked());
        assertFalse(TournamentBattleStatus.CANCELLED.isPartyLocked());
        assertFalse(TournamentBattleStatus.CREATED.isPartyLocked());
    }
}
