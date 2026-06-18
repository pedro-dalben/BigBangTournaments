package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentMode;
import com.bigbang_tournaments.model.StandardTournamentMode;
import com.bigbang_tournaments.model.SingleTypeTournamentMode;

import java.util.HashMap;
import java.util.Map;

public final class TournamentModeRegistry {
    private static final Map<String, TournamentMode> MODES = new HashMap<>();
    private static final TournamentMode DEFAULT_MODE = new StandardTournamentMode();

    static {
        register(DEFAULT_MODE);
        register(new SingleTypeTournamentMode());
    }

    private TournamentModeRegistry() {
    }

    public static void register(TournamentMode mode) {
        MODES.put(mode.id().toLowerCase(), mode);
        for (String alias : mode.aliases()) {
            MODES.put(alias.toLowerCase(), mode);
        }
    }

    public static TournamentMode resolve(String type) {
        if (type == null) {
            return DEFAULT_MODE;
        }
        TournamentMode mode = MODES.get(type.toLowerCase());
        return mode != null ? mode : DEFAULT_MODE;
    }

    public static boolean isValidType(String type) {
        if (type == null) {
            return false;
        }
        return MODES.containsKey(type.toLowerCase());
    }
}
