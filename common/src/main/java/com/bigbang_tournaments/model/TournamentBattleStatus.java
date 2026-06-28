package com.bigbang_tournaments.model;

public enum TournamentBattleStatus {
    CREATED,
    TEAM_PREVIEW,
    PLAYER_ONE_SELECTED,
    PLAYER_TWO_SELECTED,
    PREPARING_PARTIES,
    PARTIES_SWAPPED,
    BATTLE_STARTING,
    COUNTDOWN,
    ACTIVE,
    RESTORE_PENDING,
    RESTORING,
    RESTORED,
    CANCELLED,
    FAILED,
    FINISHED,
    MANUAL_RESULT_REQUIRED,
    INTERRUPTED;

    public boolean isTerminal() {
        return this == RESTORED || this == CANCELLED || this == FINISHED;
    }

    public boolean isBattleActive() {
        return this == BATTLE_STARTING || this == COUNTDOWN || this == ACTIVE;
    }

    public boolean isPreviewActive() {
        return this == CREATED || this == TEAM_PREVIEW
                || this == PLAYER_ONE_SELECTED || this == PLAYER_TWO_SELECTED;
    }

    public boolean isPartyModified() {
        return this == PREPARING_PARTIES || this == PARTIES_SWAPPED
                || this == BATTLE_STARTING || this == COUNTDOWN
                || this == ACTIVE || this == RESTORE_PENDING
                || this == RESTORING || this == FAILED;
    }

    public boolean isPartyLocked() {
        return this == TEAM_PREVIEW || this == PLAYER_ONE_SELECTED
                || this == PLAYER_TWO_SELECTED || this == PREPARING_PARTIES
                || this == PARTIES_SWAPPED || this == BATTLE_STARTING
                || this == COUNTDOWN || this == ACTIVE
                || this == RESTORE_PENDING || this == RESTORING;
    }
}
