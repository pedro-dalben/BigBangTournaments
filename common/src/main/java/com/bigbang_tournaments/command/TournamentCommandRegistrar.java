package com.bigbang_tournaments.command;

import com.bigbang_tournaments.service.PokemonTeamService;
import com.bigbang_tournaments.service.PokemonTeamService.PrepareResult;
import com.bigbang_tournaments.service.PokemonTeamService.RestoreResult;
import com.bigbang_tournaments.service.PokemonTeamService.ValidateResult;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
                .then(Commands.literal("restore")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    return executeRestore(context.getSource(), target);
                                })))
                .then(Commands.literal("validate")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            int level = IntegerArgumentType.getInteger(context, "level");
                                            return executeValidate(context.getSource(), target, level);
                                        })))));
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
}
