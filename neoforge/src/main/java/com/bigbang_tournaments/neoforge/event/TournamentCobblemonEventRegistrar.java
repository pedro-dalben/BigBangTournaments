package com.bigbang_tournaments.neoforge.event;

import com.bigbang_tournaments.neoforge.BigBangTournaments;
import com.bigbang_tournaments.service.TournamentBattleService;
import com.bigbang_tournaments.service.TournamentStateService;
import com.bigbang_tournaments.util.TournamentMessages;
import java.util.UUID;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.platform.events.PlatformEvents;
import com.github.yajatkaul.mega_showdown.api.event.DynamaxStartCallback;
import com.github.yajatkaul.mega_showdown.api.event.UltraBurstCallback;
import net.minecraft.server.level.ServerPlayer;

public final class TournamentCobblemonEventRegistrar {
    private static boolean registered;

    private TournamentCobblemonEventRegistrar() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CobblemonEvents.TRADE_EVENT_PRE.subscribe(Priority.HIGHEST, tradeEvent -> {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            var uuid1 = tradeEvent.getTradeParticipant1().getUuid();
            var uuid2 = tradeEvent.getTradeParticipant2().getUuid();
            if (TournamentStateService.isPartyLockedForTournament(server, uuid1) || TournamentStateService.isPartyLockedForTournament(server, uuid2)) {
                tradeEvent.cancel();
                ServerPlayer player1 = server.getPlayerList().getPlayer(uuid1);
                ServerPlayer player2 = server.getPlayerList().getPlayer(uuid2);
                if (player1 != null) {
                    TournamentMessages.sendRosterLocked(player1);
                }
                if (player2 != null) {
                    TournamentMessages.sendRosterLocked(player2);
                }
            }
        });

        CobblemonEvents.POKEMON_RELEASED_EVENT_PRE.subscribe(Priority.HIGHEST, releaseEvent -> {
            ServerPlayer player = releaseEvent.getPlayer();
            if (player.getServer() != null && TournamentStateService.isPartyLockedForTournament(player.getServer(), player.getUUID())) {
                releaseEvent.cancel();
                TournamentMessages.sendRosterLocked(player);
            }
        });

        CobblemonEvents.HELD_ITEM_PRE.subscribe(Priority.HIGHEST, event -> {
            ServerPlayer owner = event.getPokemon().getOwnerPlayer();
            if (owner != null && owner.getServer() != null && TournamentStateService.isPartyLockedForTournament(owner.getServer(), owner.getUUID())) {
                event.cancel();
                TournamentMessages.sendRosterLocked(owner);
            }
        });

        CobblemonEvents.COSMETIC_ITEM_PRE.subscribe(Priority.HIGHEST, event -> {
            ServerPlayer owner = event.getPokemon().getOwnerPlayer();
            if (owner != null && owner.getServer() != null && TournamentStateService.isPartyLockedForTournament(owner.getServer(), owner.getUUID())) {
                event.cancel();
                TournamentMessages.sendRosterLocked(owner);
            }
        });

        CobblemonEvents.POKEMON_ASPECTS_CHANGED.subscribe(event -> {
            if (event.getOwnerId() == null) {
                return;
            }
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null || !TournamentStateService.isPartyLockedForTournament(server, event.getOwnerId())) {
                return;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(event.getOwnerId());
            if (owner != null) {
                TournamentMessages.sendRosterLocked(owner);
            }
            BigBangTournaments.LOGGER.warn("[CAMPEONATO] {} tentou alterar aspects/forma com roster travado.", event.getOwnerId());
        });

        CobblemonEvents.BATTLE_STARTED_POST.subscribe(event -> TournamentBattleService.handleBattleStarted(event.getBattle()));
        CobblemonEvents.BATTLE_VICTORY.subscribe(TournamentBattleService::handleBattleVictory);
        CobblemonEvents.BATTLE_FLED.subscribe(TournamentBattleService::handleBattleFled);

        PlatformEvents.SERVER_STARTED.subscribe(event -> {
            TournamentStateService.startupRecovery(event.getServer());
            TournamentBattleService.handleRestartRecovery(event.getServer());
        });
        PlatformEvents.SERVER_STOPPING.subscribe(event -> TournamentStateService.shutdown(event.getServer()));
        PlatformEvents.SERVER_PLAYER_TICK_POST.subscribe(event -> TournamentBattleService.handlePlayerTick(event.getPlayer()));
        PlatformEvents.SERVER_PLAYER_LOGOUT.subscribe(event -> TournamentBattleService.handleDisconnect(event.getPlayer()));

        PlatformEvents.SERVER_PLAYER_LOGIN.subscribe(event -> {
            ServerPlayer player = event.getPlayer();
            net.minecraft.server.MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            if (TournamentStateService.getActiveBattle(server) != null
                    && TournamentStateService.getActiveBattle(server).isInterruptedByRestart()) {
                TournamentMessages.send(player, "Partida ativa foi interrompida por restart. Resultado deve ser definido manualmente.");
            }

            try {
                com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);
                if (state.getTournamentName() != null && !state.getTournamentName().isBlank()) {
                    com.bigbang_tournaments.model.TournamentConfig config = TournamentStateService.getConfig(server);
                    boolean isRegistered = TournamentStateService.getParticipant(server, player.getUUID()).isPresent();
                    
                    if (isRegistered) {
                        var recordOpt = TournamentStateService.getParticipant(server, player.getUUID());
                        String el = null;
                        if (recordOpt.isPresent()) {
                            var record = recordOpt.get();
                            el = TournamentStateService.assignElementToExistingParticipant(server, record);
                        }

                        long days = -1;
                        try {
                            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            java.time.LocalDate tournamentDate = java.time.LocalDate.parse(state.getScheduledDate(), formatter);
                            java.time.LocalDate today = java.time.LocalDate.now();
                            days = java.time.temporal.ChronoUnit.DAYS.between(today, tournamentDate);
                        } catch (Exception ignored) {
                            try {
                                java.time.format.DateTimeFormatter formatter2 = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
                                java.time.LocalDate tournamentDate = java.time.LocalDate.parse(state.getScheduledDate(), formatter2);
                                java.time.LocalDate today = java.time.LocalDate.now();
                                days = java.time.temporal.ChronoUnit.DAYS.between(today, tournamentDate);
                            } catch (Exception ignored2) {
                            }
                        }
                        
                        String formatted;
                        if (days >= 0) {
                            try {
                                formatted = String.format(config.getRegisteredLoginMessage(), days);
                            } catch (Exception e) {
                                formatted = String.format("Faltam %d dias para o campeonato!", days);
                            }
                        } else {
                            formatted = "Faltam 0 dias para o campeonato!";
                        }
                        TournamentMessages.send(player, formatted);
                        if (el != null) {
                            TournamentMessages.send(player, "Seu elemento no campeonato e: " + el);
                        }
                    } else {
                        String formatted;
                        try {
                            formatted = String.format(config.getUnregisteredLoginMessage(), state.getScheduledDate(), state.getScheduledTime());
                        } catch (Exception e) {
                            formatted = "ira acontecer um campeonato no dia " + state.getScheduledDate() + " as " + state.getScheduledTime() + ", para se inscrever digite /torneio participar";
                        }
                        TournamentMessages.send(player, formatted);
                    }
                }
            } catch (Exception e) {
                BigBangTournaments.LOGGER.error("Erro no handler de login do campeonato", e);
            }
        });

        DynamaxStartCallback.EVENT.register((battle, battlePokemon, gmax) -> {
            BigBangTournaments.LOGGER.info("Tournament audit: dynamax started in battle {}", battle.getBattleId());
        });
        UltraBurstCallback.EVENT.register((battle, battlePokemon) -> {
            BigBangTournaments.LOGGER.info("Tournament audit: ultra burst in battle {}", battle.getBattleId());
        });
    }
}
