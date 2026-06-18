package com.bigbang_tournaments.model;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;

public class TournamentValidationContext {
    private final ServerPlayer player;
    private final Collection<Pokemon> party;
    private final int expectedLevel;
    private final TournamentParticipantRecord participantRecord;

    public TournamentValidationContext(ServerPlayer player, Collection<Pokemon> party, int expectedLevel, TournamentParticipantRecord participantRecord) {
        this.player = player;
        this.party = party;
        this.expectedLevel = expectedLevel;
        this.participantRecord = participantRecord;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public Collection<Pokemon> getParty() {
        return party;
    }

    public int getExpectedLevel() {
        return expectedLevel;
    }

    public TournamentParticipantRecord getParticipantRecord() {
        return participantRecord;
    }
}
