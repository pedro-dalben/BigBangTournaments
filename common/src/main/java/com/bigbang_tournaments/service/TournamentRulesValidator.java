package com.bigbang_tournaments.service;

import com.bigbang_tournaments.model.TournamentConfig;
import com.bigbang_tournaments.model.TournamentRuleViolation;
import com.bigbang_tournaments.model.TournamentRuleViolationType;
import com.bigbang_tournaments.model.TournamentSnapshot;
import com.bigbang_tournaments.model.TournamentSpecialMechanic;
import com.bigbang_tournaments.storage.SnapshotStorage;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TournamentRulesValidator {
    private TournamentRulesValidator() {
    }

    public static List<TournamentRuleViolation> validatePlayer(ServerPlayer player, int expectedLevel, boolean includeRosterDiff) {
        return validatePlayer(player, expectedLevel, includeRosterDiff, true);
    }

    public static List<TournamentRuleViolation> validatePlayer(ServerPlayer player, int expectedLevel, boolean includeRosterDiff, boolean checkLevel) {
        MinecraftServer server = player.getServer();
        TournamentConfig config = server != null ? TournamentStateService.getConfig(server) : new TournamentConfig();
        TournamentSnapshot snapshot = includeRosterDiff && server != null
                ? SnapshotStorage.loadSnapshot(server, player.getUUID())
                : null;
        return validatePlayer(player, expectedLevel, config, snapshot, checkLevel);
    }

    public static List<TournamentRuleViolation> validatePlayer(ServerPlayer player, int expectedLevel, TournamentConfig config, TournamentSnapshot lockedSnapshot) {
        return validatePlayer(player, expectedLevel, config, lockedSnapshot, true);
    }

    public static List<TournamentRuleViolation> validatePlayer(ServerPlayer player, int expectedLevel, TournamentConfig config, TournamentSnapshot lockedSnapshot, boolean checkLevel) {
        Collection<Pokemon> party = new ArrayList<>();
        Cobblemon.INSTANCE.getStorage().getParty(player).forEach(pokemon -> {
            if (pokemon != null) {
                party.add(pokemon);
            }
        });

        List<TournamentRuleViolation> violations = new ArrayList<>();
        Map<String, String> seenSpecies = new HashMap<>();
        Map<String, String> seenItems = new HashMap<>();
        int partySize = Cobblemon.INSTANCE.getStorage().getParty(player).size();
        boolean hasAnyPokemon = false;

        for (int slotIndex = 0; slotIndex < partySize; slotIndex++) {
            Pokemon pokemon = Cobblemon.INSTANCE.getStorage().getParty(player).get(slotIndex);
            if (pokemon == null) {
                violations.add(violation(TournamentRuleViolationType.EMPTY_SLOT, "Slot vazio detectado: " + (slotIndex + 1) + ".", "slot:" + slotIndex));
                continue;
            }

            hasAnyPokemon = true;
            String speciesName = pokemon.getSpecies() != null ? pokemon.getSpecies().getName() : "Unknown";
            String speciesKey = normalize(speciesName);
            if (config.isSpeciesClauseEnabled() && seenSpecies.containsKey(speciesKey)) {
                violations.add(violation(TournamentRuleViolationType.DUPLICATED_SPECIES, "Pokemon repetido detectado: " + speciesName + ".", speciesName));
            } else {
                seenSpecies.put(speciesKey, speciesName);
            }

            if (checkLevel && pokemon.getLevel() != expectedLevel) {
                violations.add(violation(TournamentRuleViolationType.INVALID_LEVEL,
                        "Pokemon fora do nivel esperado: " + speciesName + " esta no level " + pokemon.getLevel() + " (esperado " + expectedLevel + ").",
                        speciesName));
            }

            if (config.isBanLegendaries() && pokemon.isLegendary()) {
                violations.add(violation(TournamentRuleViolationType.BANNED_LEGENDARY, "Pokemon banido detectado: " + speciesName + ".", speciesName));
            }

            if (config.isBanMythicals() && pokemon.isMythical()) {
                violations.add(violation(TournamentRuleViolationType.BANNED_MYTHICAL, "Pokemon banido detectado: " + speciesName + ".", speciesName));
            }

            if (isBannedSpecies(pokemon, config.getBannedSpecies())) {
                violations.add(violation(TournamentRuleViolationType.BANNED_SPECIES, "Pokemon banido detectado: " + speciesName + ".", speciesName));
            }

            ItemStack heldItem = pokemon.heldItem();
            String heldItemId = PokemonTeamService.getHeldItemId(heldItem);
            if (!heldItemId.isBlank()) {
                if (config.isItemClauseEnabled() && seenItems.containsKey(normalize(heldItemId))) {
                    violations.add(violation(TournamentRuleViolationType.DUPLICATED_HELD_ITEM, "Item repetido detectado: " + heldItemId + ".", heldItemId));
                } else {
                    seenItems.put(normalize(heldItemId), heldItemId);
                }

                if (containsIgnoreCase(config.getBannedItems(), heldItemId)) {
                    violations.add(violation(TournamentRuleViolationType.BANNED_ITEM, "Item banido detectado: " + heldItemId + ".", heldItemId));
                }
            }
        }

        if (!hasAnyPokemon) {
            violations.add(violation(TournamentRuleViolationType.EMPTY_PARTY, "O time esta vazio.", "party"));
        }

        Set<TournamentSpecialMechanic> mechanics = TournamentMechanicInspector.detectMechanics(player, party);
        if (!config.isAllowMega() && mechanics.contains(TournamentSpecialMechanic.MEGA_EVOLUTION)) {
            violations.add(violation(TournamentRuleViolationType.DISALLOWED_SPECIAL_MECHANIC, "Mega Evolucao nao e permitida neste campeonato.", "mega"));
        }
        if (!config.isAllowTera() && mechanics.contains(TournamentSpecialMechanic.TERASTALLIZATION)) {
            violations.add(violation(TournamentRuleViolationType.DISALLOWED_SPECIAL_MECHANIC, "Teralizacao nao e permitida neste campeonato.", "tera"));
        }
        if (!config.isAllowDynamax() && mechanics.contains(TournamentSpecialMechanic.DYNAMAX)) {
            violations.add(violation(TournamentRuleViolationType.DISALLOWED_SPECIAL_MECHANIC, "Dynamax nao e permitido neste campeonato.", "dynamax"));
        }
        if (!config.isAllowZMove() && mechanics.contains(TournamentSpecialMechanic.Z_MOVE)) {
            violations.add(violation(TournamentRuleViolationType.DISALLOWED_SPECIAL_MECHANIC, "Z-Move nao e permitido neste campeonato.", "zmove"));
        }
        if (config.isSingleSpecialMechanicPerTeam() && mechanics.size() > 1) {
            violations.add(violation(
                    TournamentRuleViolationType.MULTIPLE_SPECIAL_MECHANICS,
                    "Mais de uma mecanica especial detectada: " + formatMechanics(mechanics) + ".",
                    mechanics.toString()
            ));
        }

        if (lockedSnapshot != null) {
            for (String detail : PokemonTeamService.comparePartyAgainstSnapshot(player, lockedSnapshot)) {
                violations.add(violation(TournamentRuleViolationType.ROSTER_CHANGED_AFTER_LOCK, detail, detail));
            }
        }

        return violations;
    }

    public static List<String> toReasonList(List<TournamentRuleViolation> violations) {
        List<String> reasons = new ArrayList<>();
        for (TournamentRuleViolation violation : violations) {
            reasons.add(violation.getMessage());
        }
        return reasons;
    }

    private static TournamentRuleViolation violation(TournamentRuleViolationType type, String message, String detail) {
        return new TournamentRuleViolation(type, message, detail);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean containsIgnoreCase(List<String> values, String target) {
        if (target == null) {
            return false;
        }
        String normalizedTarget = normalize(target);
        for (String value : values) {
            if (normalize(value).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBannedSpecies(Pokemon pokemon, List<String> bannedSpecies) {
        for (String bannedEntry : bannedSpecies) {
            if (TournamentPokemonBanHelper.matchesBanEntry(pokemon, bannedEntry)) {
                return true;
            }
        }
        return false;
    }

    private static String formatMechanics(Set<TournamentSpecialMechanic> mechanics) {
        List<String> names = new ArrayList<>();
        Set<TournamentSpecialMechanic> ordered = new LinkedHashSet<>(mechanics);
        for (TournamentSpecialMechanic mechanic : ordered) {
            switch (mechanic) {
                case MEGA_EVOLUTION -> names.add("Mega Evolucao");
                case TERASTALLIZATION -> names.add("Teralizacao");
                case DYNAMAX -> names.add("Dynamax");
                case Z_MOVE -> names.add("Z-Move");
            }
        }
        return String.join(" + ", names);
    }
}
