package com.bigbang_tournaments.neoforge.event;

import com.bigbang_tournaments.neoforge.BigBangTournaments;
import com.bigbang_tournaments.service.TournamentStateService;
import com.bigbang_tournaments.util.TournamentMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BigBangTournaments.MODID)
public final class TournamentEventsHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        Block block = state.getBlock();

        // Check if the block is the Cobblemon PC block
        if (block instanceof com.cobblemon.mod.common.block.PCBlock) {
            if (player.getServer() != null && TournamentStateService.isRosterLocked(player.getServer(), player.getUUID())) {
                event.setCanceled(true);
                TournamentMessages.sendRosterLocked(player);
            }
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getServer() != null && TournamentStateService.isRosterLocked(player.getServer(), player.getUUID())) {
            String command = event.getParseResults().getReader().getString().trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            String commandLower = command.toLowerCase();

            if (commandLower.startsWith("pc") ||
                commandLower.startsWith("pokebox") ||
                commandLower.startsWith("storage") ||
                commandLower.startsWith("box") ||
                commandLower.startsWith("pk")) {

                event.setCanceled(true);
                TournamentMessages.sendRosterLocked(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        net.minecraft.server.MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        com.bigbang_tournaments.model.TournamentState state = TournamentStateService.getState(server);
        if ("CHECK_IN".equals(state.getTournamentPhase())) {
            java.util.Optional<com.bigbang_tournaments.model.TournamentParticipantRecord> recordOpt =
                    TournamentStateService.getParticipant(server, player.getUUID());
            if (recordOpt.isPresent()) {
                com.bigbang_tournaments.model.TournamentParticipantRecord record = recordOpt.get();
                if (record.getCheckInStatus() == com.bigbang_tournaments.model.TournamentCheckInStatus.AWAITING ||
                    record.getCheckInStatus() == com.bigbang_tournaments.model.TournamentCheckInStatus.NOT_STARTED) {
                    
                    long timeLeft = state.getCheckInDeadline() - System.currentTimeMillis();
                    if (timeLeft > 0) {
                        long min = timeLeft / 60000L;
                        long sec = (timeLeft % 60000L) / 1000L;
                        
                        net.minecraft.network.chat.Component timeComponent;
                        if (min > 0) {
                            if (min == 1) {
                                timeComponent = TournamentMessages.plain("time.minute_and_seconds", min, sec);
                            } else {
                                timeComponent = TournamentMessages.plain("time.minutes_and_seconds", min, sec);
                            }
                        } else {
                            timeComponent = TournamentMessages.plain("time.seconds", sec);
                        }

                        net.minecraft.network.chat.Component alertComponent =
                            TournamentMessages.translatable("commands.tournament.checkin.login_alert", timeComponent);
                        
                        player.sendSystemMessage(alertComponent);
                    }
                }
            }
        }
    }
}
