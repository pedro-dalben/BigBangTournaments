package com.bigbang_tournaments.command;

import com.bigbang_tournaments.service.PokemonTeamService;
import com.bigbang_tournaments.service.PokemonTeamService.PrepareResult;
import com.bigbang_tournaments.service.PokemonTeamService.RestoreResult;
import com.bigbang_tournaments.service.PokemonTeamService.ValidateResult;
import com.bigbang_tournaments.storage.SnapshotStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.UUID;

public final class TournamentCommandRegistrar {
    private TournamentCommandRegistrar() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tournament")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("prepare")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            int level = IntegerArgumentType.getInteger(context, "level");
                                            return executePrepare(context.getSource(), target, level, false);
                                        })
                                        .then(Commands.argument("force", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    int level = IntegerArgumentType.getInteger(context, "level");
                                                    boolean force = BoolArgumentType.getBool(context, "force");
                                                    return executePrepare(context.getSource(), target, level, force);
                                                })))))
                .then(Commands.literal("prepareall")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes(context -> {
                                    int level = IntegerArgumentType.getInteger(context, "level");
                                    return executePrepareAll(context.getSource(), level, false);
                                })
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                        .executes(context -> {
                                            int level = IntegerArgumentType.getInteger(context, "level");
                                            boolean force = BoolArgumentType.getBool(context, "force");
                                            return executePrepareAll(context.getSource(), level, force);
                                        }))))
                .then(Commands.literal("restore")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    return executeRestore(context.getSource(), target);
                                })))
                .then(Commands.literal("restoreall")
                        .executes(context -> {
                            return executeRestoreAll(context.getSource());
                        }))
                .then(Commands.literal("validate")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            int level = IntegerArgumentType.getInteger(context, "level");
                                            return executeValidate(context.getSource(), target, level);
                                        }))))
                .then(Commands.literal("healall")
                        .executes(context -> {
                            return executeHealAll(context.getSource());
                        })));
    }

    private static int executePrepare(CommandSourceStack source, ServerPlayer target, int level, boolean force) {
        if (level != 50 && level != 100) {
            source.sendFailure(Component.translatable("commands.tournament.prepare.invalid_level"));
            return 0;
        }

        PrepareResult result = PokemonTeamService.prepareTeam(target, level, force);
        switch (result.getStatus()) {
            case SUCCESS:
                source.sendSuccess(() -> Component.translatable("commands.tournament.prepare.success", target.getGameProfile().getName(), level), true);
                target.sendSystemMessage(Component.translatable("commands.tournament.prepare.success_target", level));
                return 1;
            case ALREADY_HAS_SNAPSHOT:
                source.sendFailure(Component.translatable("commands.tournament.prepare.already_has_snapshot", target.getGameProfile().getName()));
                return 0;
            case EMPTY_PARTY:
                source.sendFailure(Component.translatable("commands.tournament.prepare.empty_party", target.getGameProfile().getName()));
                return 0;
            default:
                source.sendFailure(Component.translatable("commands.tournament.prepare.error"));
                return 0;
        }
    }

    private static int executePrepareAll(CommandSourceStack source, int level, boolean force) {
        if (level != 50 && level != 100) {
            source.sendFailure(Component.translatable("commands.tournament.prepare.invalid_level"));
            return 0;
        }

        Collection<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        int successCount = 0;
        int skippedSnapshotCount = 0;
        int skippedEmptyCount = 0;
        int errorCount = 0;

        for (ServerPlayer player : players) {
            PrepareResult result = PokemonTeamService.prepareTeam(player, level, force);
            switch (result.getStatus()) {
                case SUCCESS:
                    successCount++;
                    break;
                case ALREADY_HAS_SNAPSHOT:
                    skippedSnapshotCount++;
                    break;
                case EMPTY_PARTY:
                    skippedEmptyCount++;
                    break;
                default:
                    errorCount++;
                    break;
            }
        }

        int finalSuccess = successCount;
        int finalSkippedSnapshot = skippedSnapshotCount;
        int finalSkippedEmpty = skippedEmptyCount;
        int finalError = errorCount;

        source.sendSuccess(() -> Component.translatable("commands.tournament.prepareall.summary",
                finalSuccess, finalSkippedSnapshot, finalSkippedEmpty, finalError), true);

        return successCount;
    }

    private static int executeRestore(CommandSourceStack source, ServerPlayer target) {
        RestoreResult result = PokemonTeamService.restoreTeam(target);
        switch (result.getStatus()) {
            case SUCCESS:
                source.sendSuccess(() -> Component.translatable("commands.tournament.restore.success", target.getGameProfile().getName()), true);
                target.sendSystemMessage(Component.translatable("commands.tournament.restore.success_target"));
                return 1;
            case PARTIAL:
                String restoredList = String.join(", ", result.getRestoredPokemon());
                String missingList = String.join(", ", result.getMissingPokemon());
                source.sendSuccess(() -> Component.translatable("commands.tournament.restore.partial", target.getGameProfile().getName(), restoredList, missingList), true);
                target.sendSystemMessage(Component.translatable("commands.tournament.restore.partial_target"));
                return 1;
            case NO_SNAPSHOT:
                source.sendFailure(Component.translatable("commands.tournament.restore.no_snapshot", target.getGameProfile().getName()));
                return 0;
            default:
                source.sendFailure(Component.translatable("commands.tournament.restore.error"));
                return 0;
        }
    }

    private static int executeRestoreAll(CommandSourceStack source) {
        Collection<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        int successCount = 0;
        int partialCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (ServerPlayer player : players) {
            UUID playerUuid = player.getUUID();
            if (SnapshotStorage.hasSnapshot(source.getServer(), playerUuid)) {
                RestoreResult result = PokemonTeamService.restoreTeam(player);
                switch (result.getStatus()) {
                    case SUCCESS:
                        successCount++;
                        break;
                    case PARTIAL:
                        partialCount++;
                        break;
                    default:
                        errorCount++;
                        break;
                }
            } else {
                skippedCount++;
            }
        }

        int finalSuccess = successCount;
        int finalPartial = partialCount;
        int finalSkipped = skippedCount;
        int finalError = errorCount;

        source.sendSuccess(() -> Component.translatable("commands.tournament.restoreall.summary",
                finalSuccess, finalPartial, finalSkipped, finalError), true);

        return successCount + partialCount;
    }

    private static int executeValidate(CommandSourceStack source, ServerPlayer target, int level) {
        ValidateResult result = PokemonTeamService.validateTeam(target, level);
        if (result.isValid()) {
            source.sendSuccess(() -> Component.translatable("commands.tournament.validate.valid", target.getGameProfile().getName(), level), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("commands.tournament.validate.invalid", target.getGameProfile().getName()));
            for (String reason : result.getInvalidReasons()) {
                source.sendFailure(Component.literal("- " + reason));
            }
            return 0;
        }
    }

    private static int executeHealAll(CommandSourceStack source) {
        Collection<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        int successCount = 0;

        for (ServerPlayer player : players) {
            boolean success = PokemonTeamService.healPlayerTeam(player);
            if (success) {
                successCount++;
            }
        }

        int finalSuccess = successCount;
        source.sendSuccess(() -> Component.translatable("commands.tournament.healall.success", finalSuccess), true);
        return successCount;
    }
}
