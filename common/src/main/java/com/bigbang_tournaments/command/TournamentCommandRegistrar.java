package com.bigbang_tournaments.command;

import com.bigbang_tournaments.model.TournamentParticipantRecord;
import com.bigbang_tournaments.model.TournamentRuleViolation;
import com.bigbang_tournaments.model.TournamentConfig;
import com.bigbang_tournaments.model.TournamentCheckInStatus;
import com.bigbang_tournaments.model.TournamentMode;
import com.bigbang_tournaments.service.PokemonTeamService;
import com.bigbang_tournaments.service.TournamentPokemonBanHelper;
import com.bigbang_tournaments.service.TournamentBattleService;
import com.bigbang_tournaments.service.TournamentRulesValidator;
import com.bigbang_tournaments.service.TournamentStateService;
import com.bigbang_tournaments.service.TournamentModeRegistry;
import com.bigbang_tournaments.storage.SnapshotStorage;
import com.bigbang_tournaments.util.TournamentMessages;
import com.bigbang_tournaments.util.PermissionHelper;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.cobblemon.mod.common.pokemon.Species;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TournamentCommandRegistrar {
    private TournamentCommandRegistrar() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tournament")
                .then(registerBanCommands()
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer()))))
                .then(registerParticipantCommands()
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer()))))
                .then(registerArenaCommands()
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer()))))
                .then(Commands.literal("validate")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> executeValidate(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "level"))))))
                .then(Commands.literal("validateall")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes(context -> executeValidateAll(context.getSource(), IntegerArgumentType.getInteger(context, "level")))))
                .then(Commands.literal("prepare")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> executePrepare(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "level"),
                                                false))
                                        .then(Commands.argument("force", BoolArgumentType.bool())
                                                .executes(context -> executePrepare(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "level"),
                                                        BoolArgumentType.getBool(context, "force")))))))
                .then(Commands.literal("prepareall")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes(context -> executePrepareAll(context.getSource(), IntegerArgumentType.getInteger(context, "level"), false))
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                        .executes(context -> executePrepareAll(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "level"),
                                                BoolArgumentType.getBool(context, "force"))))))
                .then(Commands.literal("restore")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> executeRestore(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("restoreall")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .executes(context -> executeRestoreAll(context.getSource())))
                .then(Commands.literal("unlock")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(context -> executeUnlock(context.getSource(), singleProfile(context, "player")))))
                .then(Commands.literal("battle")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("player1", EntityArgument.player())
                                .then(Commands.argument("player2", EntityArgument.player())
                                        .executes(context -> TournamentBattleService.startBattle(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player1"),
                                                EntityArgument.getPlayer(context, "player2"))))))
                .then(Commands.literal("win")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> TournamentBattleService.registerManualWin(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("start")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("standard");
                                    builder.suggest("singletype");
                                    builder.suggest("doubles");
                                    builder.suggest("regulation_i_doubles");
                                    return builder.buildFuture();
                                })
                                .executes(context -> executeStartTournament(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "type")))))
                .then(Commands.literal("entrar")
                        .executes(context -> executeEntrar(context.getSource())))
                .then(Commands.literal("spectate")
                        .executes(context -> executeSpectate(context.getSource())))
                .then(Commands.literal("healall")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .executes(context -> executeHealAll(context.getSource())))
                .then(Commands.literal("end")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .executes(context -> executeEndTournament(context.getSource(), null))
                        .then(Commands.argument("champion", GameProfileArgument.gameProfile())
                                .executes(context -> executeEndTournament(context.getSource(), singleProfile(context, "champion")))))
                .then(Commands.literal("finalizar")
                        .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                        .executes(context -> executeEndTournament(context.getSource(), null))
                        .then(Commands.argument("champion", GameProfileArgument.gameProfile())
                                .executes(context -> executeEndTournament(context.getSource(), singleProfile(context, "champion"))))));

        dispatcher.register(Commands.literal("finalizarcampeonato")
                .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                .executes(context -> executeEndTournament(context.getSource(), null))
                .then(Commands.argument("champion", GameProfileArgument.gameProfile())
                        .executes(context -> executeEndTournament(context.getSource(), singleProfile(context, "champion")))));

        dispatcher.register(Commands.literal("assistirbatalha")
                .executes(context -> executeSpectate(context.getSource())));

        dispatcher.register(Commands.literal("criarcampeonato")
                .requires(source -> source.hasPermission(TournamentStateService.getAdminPermissionLevel(source.getServer())))
                .then(Commands.argument("dia", ResourceLocationArgument.id())
                        .then(Commands.argument("horario", ResourceLocationArgument.id())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("singleelement");
                                            builder.suggest("singletype");
                                            builder.suggest("monotype");
                                            builder.suggest("doubles");
                                            builder.suggest("2v2");
                                            builder.suggest("duplas");
                                            builder.suggest("regulation_i_doubles");
                                            builder.suggest("standard");
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                                .executes(context -> executeCreateTournament(
                                                        context.getSource(),
                                                        getCleanResourceLocationString(context, "dia"),
                                                        getCleanResourceLocationString(context, "horario"),
                                                        StringArgumentType.getString(context, "nome"),
                                                        StringArgumentType.getString(context, "type"))))))));

        dispatcher.register(Commands.literal("campeonato")
                .then(Commands.literal("inscrever")
                        .executes(context -> executeRegisterSelf(context.getSource())))
                .then(Commands.literal("participar")
                        .executes(context -> executeRegisterSelf(context.getSource())))
                .then(Commands.literal("sortearnovamente")
                        .executes(context -> executeRerollSelf(context.getSource()))));

        dispatcher.register(Commands.literal("torneio")
                .then(Commands.literal("participar")
                        .executes(context -> executeRegisterSelf(context.getSource())))
                .then(Commands.literal("inscrever")
                        .executes(context -> executeRegisterSelf(context.getSource())))
                .then(Commands.literal("sortearnovamente")
                        .executes(context -> executeRerollSelf(context.getSource()))));

        dispatcher.register(Commands.literal("participarcampeonato")
                .executes(context -> executeRegisterSelf(context.getSource())));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerBanCommands() {
        return Commands.literal("ban")
                .then(Commands.literal("species")
                        .then(Commands.literal("list")
                                .executes(context -> executeBanSpeciesList(context.getSource())))
                        .then(Commands.literal("add")
                                .then(Commands.argument("species", new SpeciesArgumentType())
                                        .executes(context -> executeBanSpeciesAdd(
                                                context.getSource(),
                                                context.getArgument("species", Species.class),
                                                null))
                                        .then(Commands.argument("form", StringArgumentType.word())
                                                .suggests(TournamentCommandRegistrar::suggestFormForSpecies)
                                                .executes(context -> executeBanSpeciesAdd(
                                                        context.getSource(),
                                                        context.getArgument("species", Species.class),
                                                        StringArgumentType.getString(context, "form"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("species", new SpeciesArgumentType())
                                        .executes(context -> executeBanSpeciesRemove(
                                                context.getSource(),
                                                context.getArgument("species", Species.class),
                                                null))
                                        .then(Commands.argument("form", StringArgumentType.word())
                                                .suggests(TournamentCommandRegistrar::suggestFormForSpecies)
                                                .executes(context -> executeBanSpeciesRemove(
                                                        context.getSource(),
                                                        context.getArgument("species", Species.class),
                                                        StringArgumentType.getString(context, "form")))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerParticipantCommands() {
        return Commands.literal("participant")
                .then(Commands.literal("add")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(context -> executeParticipantAdd(context.getSource(), singleProfile(context, "player")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(context -> executeParticipantRemove(context.getSource(), singleProfile(context, "player"))))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    try {
                                        MinecraftServer server = context.getSource().getServer();
                                        for (TournamentParticipantRecord part : TournamentStateService.listParticipants(server)) {
                                            builder.suggest(part.getPlayerName());
                                        }
                                    } catch (Exception ignored) {}
                                    return builder.buildFuture();
                                })
                                .executes(context -> executeParticipantRemoveByName(context.getSource(), StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("list")
                        .executes(context -> executeParticipantList(context.getSource())));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> registerArenaCommands() {
        return Commands.literal("arena")
                .then(Commands.literal("setpos1").executes(context -> executeArenaSetPos1(context.getSource())))
                .then(Commands.literal("setpos2").executes(context -> executeArenaSetPos2(context.getSource())))
                .then(Commands.literal("setspectator").executes(context -> executeArenaSetSpectator(context.getSource())))
                .then(Commands.literal("info").executes(context -> executeArenaInfo(context.getSource())));
    }

    private static int executeValidate(CommandSourceStack source, ServerPlayer target, int level) {
        if (!isAllowedLevel(source.getServer(), level)) {
            TournamentMessages.sendFailure(source, "Level invalido. Apenas os levels configurados sao permitidos.");
            return 0;
        }

        TournamentStateService.upsertParticipant(target);
        List<TournamentRuleViolation> violations = TournamentRulesValidator.validatePlayer(target, level, false, false);
        if (violations.isEmpty()) {
            TournamentMessages.sendSuccess(source, target.getGameProfile().getName() + " esta com o time valido para level " + level + ".", true);
            return 1;
        }

        TournamentMessages.sendFailure(source, target.getGameProfile().getName() + " esta com o time invalido.");
        for (String reason : TournamentRulesValidator.toReasonList(violations)) {
            source.sendFailure(Component.literal("- " + reason));
        }
        return 0;
    }

    private static int executeValidateAll(CommandSourceStack source, int level) {
        if (!isAllowedLevel(source.getServer(), level)) {
            TournamentMessages.sendFailure(source, "Level invalido. Apenas os levels configurados sao permitidos.");
            return 0;
        }

        TournamentMessages.sendSuccess(source, "Validando todos os participantes...", true);
        int validCount = 0;
        int invalidCount = 0;
        int offlineCount = 0;

        for (TournamentParticipantRecord participant : TournamentStateService.listParticipants(source.getServer())) {
            ServerPlayer onlinePlayer = source.getServer().getPlayerList().getPlayer(participant.getPlayerUuid());
            if (onlinePlayer == null) {
                offlineCount++;
                continue;
            }

            List<TournamentRuleViolation> violations = TournamentRulesValidator.validatePlayer(onlinePlayer, level, false, false);
            if (violations.isEmpty()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }

        TournamentMessages.sendSuccess(source, "Validacao concluida. " + validCount + " valido(s), " + invalidCount + " invalido(s), " + offlineCount + " offline.", true);
        return validCount;
    }

    private static int executePrepare(CommandSourceStack source, ServerPlayer target, int level, boolean force) {
        if (!isAllowedLevel(source.getServer(), level)) {
            TournamentMessages.sendFailure(source, "Level invalido. Apenas os levels configurados sao permitidos.");
            return 0;
        }

        TournamentStateService.upsertParticipant(target);
        List<TournamentRuleViolation> violations = TournamentRulesValidator.validatePlayer(target, level, false, false);
        if (!violations.isEmpty()) {
            TournamentStateService.markPending(source.getServer(), target, level, violations);
            TournamentMessages.broadcastInvalidTeam(source.getServer(), target.getGameProfile().getName());
            TournamentMessages.sendInvalidTeam(target, TournamentRulesValidator.toReasonList(violations));
            TournamentMessages.sendFailure(source, "O time de " + target.getGameProfile().getName() + " esta invalido e foi marcado como pendente.");
            for (String reason : TournamentRulesValidator.toReasonList(violations)) {
                source.sendFailure(Component.literal("- " + reason));
            }
            return 0;
        }

        PokemonTeamService.PrepareResult result = PokemonTeamService.prepareTeam(target, level, force);
        switch (result.getStatus()) {
            case SUCCESS -> {
                TournamentStateService.markPrepared(source.getServer(), target, level);
                TournamentMessages.broadcastPrepared(source.getServer(), target.getGameProfile().getName());
                if (TournamentStateService.areAllParticipantsPrepared(source.getServer())) {
                    TournamentMessages.broadcastAllPrepared(source.getServer());
                }
                TournamentMessages.sendSuccess(source, "Time de " + target.getGameProfile().getName() + " preparado com sucesso.", true);
                return 1;
            }
            case ALREADY_HAS_SNAPSHOT -> {
                TournamentMessages.sendFailure(source, "O jogador ja possui um snapshot ativo. Use force=true para sobrescrever.");
                return 0;
            }
            case EMPTY_PARTY -> {
                TournamentMessages.sendFailure(source, "O jogador esta com a party vazia.");
                return 0;
            }
            default -> {
                TournamentMessages.sendFailure(source, "Ocorreu um erro ao preparar o time.");
                return 0;
            }
        }
    }

    private static int executePrepareAll(CommandSourceStack source, int level, boolean force) {
        if (!isAllowedLevel(source.getServer(), level)) {
            TournamentMessages.sendFailure(source, "Level invalido. Apenas os levels configurados sao permitidos.");
            return 0;
        }

        TournamentMessages.sendSuccess(source, "Preparando todos os participantes para level " + level + "...", true);
        int preparedCount = 0;
        int pendingCount = 0;
        int offlineCount = 0;

        for (TournamentParticipantRecord participant : TournamentStateService.listParticipants(source.getServer())) {
            ServerPlayer onlinePlayer = source.getServer().getPlayerList().getPlayer(participant.getPlayerUuid());
            if (onlinePlayer == null) {
                offlineCount++;
                continue;
            }

            int result = executePrepare(source, onlinePlayer, level, force);
            if (result > 0) {
                preparedCount++;
            } else {
                pendingCount++;
            }
        }

        TournamentMessages.sendSuccess(source,
                preparedCount + " participante(s) preparado(s). " + pendingCount + " pendente(s). " + offlineCount + " offline.",
                true);
        return preparedCount;
    }

    private static int executeRestore(CommandSourceStack source, ServerPlayer target) {
        PokemonTeamService.RestoreResult result = PokemonTeamService.restoreTeam(target);
        switch (result.getStatus()) {
            case SUCCESS -> {
                TournamentStateService.markRestored(source.getServer(), target.getUUID());
                TournamentMessages.sendSuccess(source, "Time de " + target.getGameProfile().getName() + " restaurado com sucesso.", true);
                return 1;
            }
            case PARTIAL -> {
                TournamentStateService.markRestored(source.getServer(), target.getUUID());
                TournamentMessages.sendSuccess(source, "Time de " + target.getGameProfile().getName() + " restaurado parcialmente.", true);
                return 1;
            }
            case NO_SNAPSHOT -> {
                TournamentMessages.sendFailure(source, "Nao existe snapshot de torneio para " + target.getGameProfile().getName() + ".");
                return 0;
            }
            default -> {
                TournamentMessages.sendFailure(source, "Ocorreu um erro ao restaurar o time.");
                return 0;
            }
        }
    }

    private static int executeRestoreAll(CommandSourceStack source) {
        int restored = 0;
        for (TournamentParticipantRecord participant : TournamentStateService.listParticipants(source.getServer())) {
            ServerPlayer onlinePlayer = source.getServer().getPlayerList().getPlayer(participant.getPlayerUuid());
            if (onlinePlayer == null || !SnapshotStorage.hasSnapshot(source.getServer(), participant.getPlayerUuid())) {
                continue;
            }
            if (executeRestore(source, onlinePlayer) > 0) {
                restored++;
            }
        }
        TournamentMessages.sendSuccess(source, restored + " jogador(es) restaurado(s).", true);
        return restored;
    }

    private static int executeUnlock(CommandSourceStack source, GameProfile profile) {
        TournamentStateService.unlock(source.getServer(), profile.getId());
        TournamentMessages.sendSuccess(source, "Roster destravado para " + profile.getName() + ".", true);
        return 1;
    }

    private static int executeParticipantAdd(CommandSourceStack source, GameProfile profile) {
        MinecraftServer server = source.getServer();
        com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);
        TournamentStateService.upsertParticipant(server, profile);

        if ("CHECK_IN".equals(state.getTournamentPhase())) {
            TournamentStateService.markCheckInAwaiting(server, profile.getId());
            server.getPlayerList().broadcastSystemMessage(TournamentMessages.translatable("commands.tournament.participant.add.during_checkin", profile.getName()), false);
        } else if ("READY".equals(state.getTournamentPhase())) {
            TournamentStateService.markCheckInConfirmed(server, profile.getId());
            server.getPlayerList().broadcastSystemMessage(TournamentMessages.translatable("commands.tournament.participant.add.after_checkin", profile.getName()), false);
        } else {
            TournamentStateService.resetCheckInState(server, profile.getId());
            TournamentMessages.sendSuccess(source, profile.getName() + " adicionado a lista de participantes.", true);
        }
        return 1;
    }

    private static int executeParticipantRemove(CommandSourceStack source, GameProfile profile) {
        boolean removed = TournamentStateService.removeParticipant(source.getServer(), profile.getId());
        if (!removed) {
            TournamentMessages.sendFailure(source, "Participante nao encontrado.");
            return 0;
        }
        TournamentMessages.sendSuccess(source, profile.getName() + " removido da lista de participantes.", true);
        return 1;
    }

    private static int executeParticipantList(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);
        List<TournamentParticipantRecord> participants = TournamentStateService.listParticipants(server);
        if (participants.isEmpty()) {
            TournamentMessages.sendFailure(source, "Nao ha participantes registrados.");
            return 0;
        }

        boolean isSingleType = "singletype".equals(TournamentModeRegistry.resolve(state.getTournamentType()).id());
        TournamentMessages.sendSuccess(source, "Participantes registrados: " + participants.size(), false);

        for (TournamentParticipantRecord participant : participants) {
            String presenceKey;
            switch (participant.getCheckInStatus()) {
                case CHECKED_IN:
                    presenceKey = "commands.tournament.presence.present";
                    break;
                case ABSENT:
                    presenceKey = "commands.tournament.presence.absent";
                    break;
                case AWAITING:
                case NOT_STARTED:
                default:
                    presenceKey = "commands.tournament.presence.awaiting";
                    break;
            }
            Component presenceComp = TournamentMessages.plain(presenceKey);

            String prepKey;
            if (participant.isPrepared()) {
                prepKey = "commands.tournament.prep.prepared";
            } else if (participant.isPendingValidation()) {
                prepKey = "commands.tournament.prep.pending";
            } else {
                prepKey = "commands.tournament.prep.not_prepared";
            }
            Component prepComp = TournamentMessages.plain(prepKey);

            Component itemComponent;
            if (isSingleType) {
                String element = participant.getAssignedElement() != null ? participant.getAssignedElement() : "N/A";
                itemComponent = TournamentMessages.plain("commands.tournament.participant.list.item.singletype",
                        participant.getPlayerName(), element, presenceComp, prepComp);
            } else {
                itemComponent = TournamentMessages.plain("commands.tournament.participant.list.item.standard",
                        participant.getPlayerName(), presenceComp, prepComp);
            }
            source.sendSuccess(() -> itemComponent, false);
        }
        return participants.size();
    }

    private static int executeBanSpeciesList(CommandSourceStack source) {
        List<String> bannedSpecies = TournamentStateService.getConfig(source.getServer()).getBannedSpecies();
        if (bannedSpecies.isEmpty()) {
            TournamentMessages.sendFailure(source, "Nao ha pokemon banidos configurados.");
            return 0;
        }

        TournamentMessages.sendSuccess(source, "Pokemon banidos: " + bannedSpecies.size(), false);
        for (String bannedEntry : bannedSpecies) {
            source.sendSuccess(() -> Component.literal("- " + TournamentPokemonBanHelper.describeEntry(bannedEntry) + " [" + bannedEntry + "]"), false);
        }
        return bannedSpecies.size();
    }

    private static int executeBanSpeciesAdd(CommandSourceStack source, Species species, String formToken) {
        String entry = formToken == null
                ? TournamentPokemonBanHelper.speciesEntry(species)
                : TournamentPokemonBanHelper.speciesFormEntry(species, formToken);

        if (entry.isBlank()) {
            TournamentMessages.sendFailure(source, "Nao foi possivel identificar o Pokemon informado.");
            return 0;
        }

        List<String> bannedSpecies = TournamentStateService.getConfig(source.getServer()).getBannedSpecies();
        if (containsNormalizedEntry(bannedSpecies, entry)) {
            TournamentMessages.sendFailure(source, "Pokemon ja esta banido: " + TournamentPokemonBanHelper.describeEntry(entry) + ".");
            return 0;
        }

        bannedSpecies.add(entry);
        TournamentStateService.saveConfig(source.getServer());
        TournamentMessages.sendSuccess(source, "Pokemon banido adicionado: " + TournamentPokemonBanHelper.describeEntry(entry) + ".", true);
        return 1;
    }

    private static int executeBanSpeciesRemove(CommandSourceStack source, Species species, String formToken) {
        String entry = formToken == null
                ? TournamentPokemonBanHelper.speciesEntry(species)
                : TournamentPokemonBanHelper.speciesFormEntry(species, formToken);

        if (entry.isBlank()) {
            TournamentMessages.sendFailure(source, "Nao foi possivel identificar o Pokemon informado.");
            return 0;
        }

        List<String> bannedSpecies = TournamentStateService.getConfig(source.getServer()).getBannedSpecies();
        boolean removed = bannedSpecies.removeIf(value -> TournamentPokemonBanHelper.normalize(value).equals(TournamentPokemonBanHelper.normalize(entry)));
        if (!removed) {
            TournamentMessages.sendFailure(source, "Pokemon nao encontrado na banlist: " + TournamentPokemonBanHelper.describeEntry(entry) + ".");
            return 0;
        }

        TournamentStateService.saveConfig(source.getServer());
        TournamentMessages.sendSuccess(source, "Pokemon removido da banlist: " + TournamentPokemonBanHelper.describeEntry(entry) + ".", true);
        return 1;
    }

    private static int executeArenaSetPos1(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            TournamentMessages.sendFailure(source, "Apenas jogadores podem definir a arena.");
            return 0;
        }
        TournamentBattleService.setArenaPos1(player);
        return 1;
    }

    private static int executeArenaSetPos2(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            TournamentMessages.sendFailure(source, "Apenas jogadores podem definir a arena.");
            return 0;
        }
        TournamentBattleService.setArenaPos2(player);
        return 1;
    }

    private static int executeArenaSetSpectator(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            TournamentMessages.sendFailure(source, "Apenas jogadores podem definir a arena.");
            return 0;
        }
        TournamentBattleService.setArenaSpectator(player);
        return 1;
    }

    private static int executeArenaInfo(CommandSourceStack source) {
        TournamentMessages.sendSuccess(source, TournamentBattleService.arenaInfo(source.getServer()), false);
        return 1;
    }

    private static int executeSpectate(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            TournamentMessages.sendFailure(source, "Apenas jogadores podem usar este comando.");
            return 0;
        }
        TournamentBattleService.spectate(player);
        return 1;
    }

    private static int executeHealAll(CommandSourceStack source) {
        Collection<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        int successCount = 0;
        for (ServerPlayer player : players) {
            if (PokemonTeamService.healPlayerTeam(player)) {
                successCount++;
            }
        }
        TournamentMessages.sendSuccess(source, "HealAll: " + successCount + " time(s) curado(s).", true);
        return successCount;
    }

    private static boolean isAllowedLevel(MinecraftServer server, int level) {
        return TournamentStateService.getConfig(server).getAllowedLevels().contains(level);
    }

    private static CompletableFuture<Suggestions> suggestFormForSpecies(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Species species = context.getArgument("species", Species.class);
        for (String suggestion : TournamentPokemonBanHelper.buildSpeciesFormSuggestions(species)) {
            builder.suggest(suggestion);
        }
        return builder.buildFuture();
    }

    private static boolean containsNormalizedEntry(List<String> values, String entry) {
        String normalizedEntry = TournamentPokemonBanHelper.normalize(entry);
        for (String value : values) {
            if (TournamentPokemonBanHelper.normalize(value).equals(normalizedEntry)) {
                return true;
            }
        }
        return false;
    }

    private static GameProfile singleProfile(CommandContext<CommandSourceStack> context, String argumentName) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, argumentName);
        if (profiles.isEmpty()) {
            throw GameProfileArgument.ERROR_UNKNOWN_PLAYER.create();
        }
        return new ArrayList<>(profiles).get(0);
    }

    private static String getCleanResourceLocationString(CommandContext<CommandSourceStack> context, String argumentName) {
        ResourceLocation loc = ResourceLocationArgument.getId(context, argumentName);
        if (loc.getNamespace().equals("minecraft")) {
            return loc.getPath();
        } else {
            return loc.getNamespace() + ":" + loc.getPath();
        }
    }

    private static int executeCreateTournament(CommandSourceStack source, String date, String time, String name, String type) {
        try {
            MinecraftServer server = source.getServer();
            com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);
            
            state.getParticipants().clear();
            state.setActiveBattle(null);
            state.getBattleHistory().clear();
            
            state.setScheduledDate(date);
            state.setScheduledTime(time);
            state.setTournamentName(name);
            state.setTournamentType(TournamentStateService.normalizeTournamentType(type));
            
            TournamentStateService.saveState(server);
            
            TournamentMessages.sendSuccess(source, "Campeonato '" + name + "' (" + type + ") criado com sucesso para o dia " + date + " as " + time + "!", true);
            return 1;
        } catch (Exception e) {
            TournamentMessages.sendFailure(source, "Erro ao criar campeonato: " + e.getMessage());
            return 0;
        }
    }

    private static int executeRegisterSelf(CommandSourceStack source) {
        try {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                TournamentMessages.sendFailure(source, "Apenas jogadores podem se inscrever.");
                return 0;
            }

            MinecraftServer server = source.getServer();
            TournamentConfig config = TournamentStateService.getConfig(server);
            String assignedElement = TournamentStateService.registerPlayer(server, player);
            com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);
            boolean needsElement = TournamentStateService.requiresElementTournamentType(state.getTournamentType());

            String broadcastMsg;
            try {
                broadcastMsg = String.format(config.getBroadcastRegistrationMessage(), player.getGameProfile().getName());
            } catch (Exception e) {
                broadcastMsg = "O " + player.getGameProfile().getName() + " se inscreveu no campeonato!";
            }
            TournamentMessages.broadcast(server, broadcastMsg);

            if (needsElement && assignedElement != null) {
                TournamentMessages.send(player, "Voce se inscreveu. Seu tipo e: " + assignedElement + ".");
            } else {
                TournamentMessages.send(player, "Voce se inscreveu no campeonato " + state.getTournamentName() + "!");
            }

            return 1;
        } catch (IllegalStateException e) {
            TournamentMessages.sendFailure(source, e.getMessage());
            return 0;
        } catch (IllegalArgumentException e) {
            TournamentMessages.sendFailure(source, e.getMessage());
            return 0;
        } catch (Exception e) {
            TournamentMessages.sendFailure(source, "Erro ao se inscrever: " + e.getMessage());
            return 0;
        }
    }

    private static int executeRerollSelf(CommandSourceStack source) {
        try {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                TournamentMessages.sendFailure(source, "Apenas jogadores podem sortear novamente.");
                return 0;
            }

            MinecraftServer server = source.getServer();
            TournamentConfig config = TournamentStateService.getConfig(server);
            String newElement = TournamentStateService.rerollElement(server, player);

            java.util.UUID uuid = player.getUUID();
            int rollsUsed = TournamentStateService.getParticipant(server, uuid)
                    .map(TournamentParticipantRecord::getRollsUsed).orElse(0);
            int maxRolls = PermissionHelper.getMaxRolls(player, config.getDefaultRerolls());

            TournamentMessages.send(player, "Voce sorteou novamente! Seu novo elemento e: " + newElement + ". Tentativas: (" + rollsUsed + "/" + maxRolls + ")");
            return 1;
        } catch (IllegalStateException e) {
            TournamentMessages.sendFailure(source, e.getMessage());
            return 0;
        } catch (IllegalArgumentException e) {
            TournamentMessages.sendFailure(source, e.getMessage());
            return 0;
        } catch (Exception e) {
            TournamentMessages.sendFailure(source, "Erro ao sortear novamente: " + e.getMessage());
            return 0;
        }
    }

    private static int executeStartTournament(CommandSourceStack source, String typeInput) {
        try {
            MinecraftServer server = source.getServer();
            com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);

            if (state.getTournamentName() == null || state.getTournamentName().trim().isEmpty()) {
                TournamentMessages.sendFailure(source, "Não foi possível iniciar o campeonato.\nNão existe campeonato agendado no momento.");
                return 0;
            }

            if (!TournamentModeRegistry.isValidType(typeInput)) {
                TournamentMessages.sendFailure(source, "Tipo de campeonato desconhecido: " + typeInput + ".");
                return 0;
            }

            com.bigbang_tournaments.model.TournamentMode mode = TournamentModeRegistry.resolve(typeInput);
            String canonicalType = mode.id();

            if ("CHECK_IN".equals(state.getTournamentPhase())) {
                TournamentMessages.sendFailure(source, "A chamada para o campeonato ja esta ativa.");
                return 0;
            }

            if (state.getParticipants().isEmpty()) {
                TournamentMessages.sendFailure(source, "Não foi possível iniciar o campeonato.\nNão existem participantes inscritos.");
                return 0;
            }

            state.setTournamentType(canonicalType);
            state.setTournamentPhase("CHECK_IN");

            long now = System.currentTimeMillis();
            long durationMs = 5 * 60 * 1000L;
            long deadline = now + durationMs;
            state.setCheckInStartedAt(now);
            state.setCheckInDeadline(deadline);

            for (TournamentParticipantRecord part : state.getParticipants()) {
                part.setCheckInStatus(TournamentCheckInStatus.AWAITING);
                part.setCheckedInAt(0L);
            }

            TournamentStateService.saveState(server);

            Component announcement = TournamentMessages.translatable("commands.tournament.start.announcement", mode.displayName(), state.getParticipants().size());
            server.getPlayerList().broadcastSystemMessage(announcement, false);

            for (TournamentParticipantRecord part : state.getParticipants()) {
                boolean isOnline = server.getPlayerList().getPlayer(part.getPlayerUuid()) != null;
                Component statusComponent = TournamentMessages.plain(isOnline ? "commands.tournament.status.online" : "commands.tournament.status.offline");

                Component pLine;
                if ("singletype".equals(canonicalType)) {
                    String element = part.getAssignedElement() != null ? part.getAssignedElement() : "N/A";
                    pLine = TournamentMessages.plain("commands.tournament.start.participant.singletype", part.getPlayerName(), element, statusComponent);
                } else {
                    pLine = TournamentMessages.plain("commands.tournament.start.participant.standard", part.getPlayerName(), statusComponent);
                }
                server.getPlayerList().broadcastSystemMessage(pLine, false);
            }

            List<String> offlineNames = new ArrayList<>();
            for (TournamentParticipantRecord part : state.getParticipants()) {
                if (server.getPlayerList().getPlayer(part.getPlayerUuid()) == null) {
                    offlineNames.add(part.getPlayerName());
                }
            }
            if (!offlineNames.isEmpty()) {
                String offlineListStr = offlineNames.stream().map(name -> "- " + name).collect(java.util.stream.Collectors.joining("\n"));
                Component offlineWarning = TournamentMessages.translatable("commands.tournament.start.offline_warning", offlineListStr);
                server.getPlayerList().broadcastSystemMessage(offlineWarning, false);
            }

            TournamentStateService.scheduleCheckInTasks(server, deadline);

            return 1;
        } catch (Exception e) {
            TournamentMessages.sendFailure(source, "Erro ao iniciar campeonato: " + e.getMessage());
            return 0;
        }
    }

    private static int executeEntrar(CommandSourceStack source) {
        try {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                TournamentMessages.sendFailure(source, "Apenas jogadores podem utilizar este comando.");
                return 0;
            }

            MinecraftServer server = source.getServer();
            com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);

            if (!"CHECK_IN".equals(state.getTournamentPhase())) {
                player.sendSystemMessage(TournamentMessages.plain("commands.tournament.entrar.no_checkin"));
                return 0;
            }

            if (System.currentTimeMillis() > state.getCheckInDeadline()) {
                player.sendSystemMessage(TournamentMessages.plain("commands.tournament.entrar.closed"));
                TournamentStateService.endCheckIn(server);
                return 0;
            }

            java.util.Optional<TournamentParticipantRecord> recordOpt = TournamentStateService.getParticipant(server, player.getUUID());
            if (recordOpt.isEmpty()) {
                player.sendSystemMessage(TournamentMessages.plain("commands.tournament.entrar.not_registered"));
                return 0;
            }

            TournamentParticipantRecord record = recordOpt.get();

            if (record.getCheckInStatus() == TournamentCheckInStatus.CHECKED_IN) {
                player.sendSystemMessage(TournamentMessages.plain("commands.tournament.entrar.already_confirmed"));
                return 0;
            }

            record.setCheckInStatus(TournamentCheckInStatus.CHECKED_IN);
            record.setCheckedInAt(System.currentTimeMillis());
            TournamentStateService.saveState(server);

            server.getPlayerList().broadcastSystemMessage(
                    TournamentMessages.translatable("commands.tournament.entrar.confirmed.broadcast", player.getGameProfile().getName()),
                    false
            );

            player.sendSystemMessage(TournamentMessages.plain("commands.tournament.entrar.confirmed.target"));

            TournamentStateService.checkIfAllCheckedIn(server);

            return 1;
        } catch (Exception e) {
            TournamentMessages.sendFailure(source, "Erro ao confirmar presenca: " + e.getMessage());
            return 0;
        }
    }

    private static int executeParticipantRemoveByName(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        java.util.Optional<TournamentParticipantRecord> recordOpt = TournamentStateService.getParticipantByName(server, name);
        if (recordOpt.isEmpty()) {
            TournamentMessages.sendFailure(source, "Participante '" + name + "' nao encontrado.");
            return 0;
        }
        TournamentParticipantRecord record = recordOpt.get();
        boolean removed = TournamentStateService.removeParticipant(server, record.getPlayerUuid());
        if (!removed) {
            TournamentMessages.sendFailure(source, "Erro ao remover participante.");
            return 0;
        }
        TournamentMessages.sendSuccess(source, record.getPlayerName() + " removido da lista de participantes.", true);
        return 1;
    }

    private static int executeEndTournament(CommandSourceStack source, GameProfile champion) {
        try {
            MinecraftServer server = source.getServer();
            com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);
            
            if (state.getTournamentName() == null || state.getTournamentName().trim().isEmpty()) {
                TournamentMessages.sendFailure(source, "Não há campeonato ativo para finalizar.");
                return 0;
            }
            
            String tournamentName = state.getTournamentName();
            
            // Set phase
            state.setTournamentPhase("FINISHED");
            state.setActiveBattle(null);
            TournamentStateService.saveState(server);
            
            // Broadcast ending
            Component announcement;
            if (champion != null) {
                announcement = TournamentMessages.translatable("commands.tournament.end.announcement.champion", tournamentName, champion.getName());
            } else {
                announcement = TournamentMessages.translatable("commands.tournament.end.announcement", tournamentName);
            }
            server.getPlayerList().broadcastSystemMessage(announcement, false);
            
            // Restore all participants
            int restored = 0;
            for (TournamentParticipantRecord participant : TournamentStateService.listParticipants(server)) {
                ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(participant.getPlayerUuid());
                if (onlinePlayer != null && SnapshotStorage.hasSnapshot(server, participant.getPlayerUuid())) {
                    PokemonTeamService.RestoreResult restoreResult = PokemonTeamService.restoreTeam(onlinePlayer);
                    if (restoreResult.getStatus() == PokemonTeamService.RestoreResult.Status.SUCCESS || 
                        restoreResult.getStatus() == PokemonTeamService.RestoreResult.Status.PARTIAL) {
                        TournamentStateService.markRestored(server, participant.getPlayerUuid());
                        restored++;
                    }
                }
            }
            
            TournamentMessages.sendSuccess(source, "Campeonato finalizado. " + restored + " time(s) de jogador(es) restaurado(s).", true);
            return 1;
        } catch (Exception e) {
            TournamentMessages.sendFailure(source, "Erro ao finalizar campeonato: " + e.getMessage());
            return 0;
        }
    }
}
