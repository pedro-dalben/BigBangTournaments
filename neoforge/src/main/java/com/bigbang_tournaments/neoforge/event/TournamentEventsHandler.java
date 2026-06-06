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
}
