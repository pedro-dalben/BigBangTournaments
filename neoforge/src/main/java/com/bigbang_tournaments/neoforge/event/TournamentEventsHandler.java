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
            if (player.getServer() != null && TournamentStateService.isPartyLockedForTournament(player.getServer(), player.getUUID())) {
                event.setCanceled(true);
                TournamentMessages.sendRosterLocked(player);
                return;
            }
        }

        // Check if player is holding pc_on_a_stick
        if (player.getServer() != null && TournamentStateService.isPartyLockedForTournament(player.getServer(), player.getUUID())) {
            net.minecraft.world.item.ItemStack stack = event.getItemStack();
            if (!stack.isEmpty()) {
                net.minecraft.resources.ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                if ("allthemons:pc_on_a_stick".equals(itemId.toString())) {
                    event.setCanceled(true);
                    TournamentMessages.sendRosterLocked(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

                                                if (player.getServer() != null && TournamentStateService.isPartyLockedForTournament(player.getServer(), player.getUUID())) {
            net.minecraft.world.item.ItemStack stack = event.getItemStack();
            if (!stack.isEmpty()) {
                net.minecraft.resources.ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                if ("allthemons:pc_on_a_stick".equals(itemId.toString())) {
                    event.setCanceled(true);
                    TournamentMessages.sendRosterLocked(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getServer() != null && TournamentStateService.isPartyLockedForTournament(player.getServer(), player.getUUID())) {
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

        // Handle VGC battle session recovery on login
        com.bigbang_tournaments.service.TournamentBattleService.handleLogin(player);

        // Inject packet listener using reflection to bypass protected field access
        try {
            net.minecraft.server.network.ServerGamePacketListenerImpl connection = player.connection;
            if (connection != null) {
                java.lang.reflect.Field connectionField = null;
                Class<?> curr = connection.getClass();
                while (curr != null) {
                    try {
                        connectionField = curr.getDeclaredField("connection");
                        break;
                    } catch (NoSuchFieldException e) {
                        curr = curr.getSuperclass();
                    }
                }
                if (connectionField != null) {
                    connectionField.setAccessible(true);
                    net.minecraft.network.Connection nettyConn = (net.minecraft.network.Connection) connectionField.get(connection);
                    if (nettyConn != null) {
                        io.netty.channel.Channel channel = nettyConn.channel();
                        if (channel != null && channel.pipeline().get("bigbang_tournaments_handler") == null) {
                            channel.pipeline().addBefore("packet_handler", "bigbang_tournaments_handler", new io.netty.channel.ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) throws Exception {
                                    if (msg instanceof net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket customPacket) {
                                        net.minecraft.network.protocol.common.custom.CustomPacketPayload payload = customPacket.payload();
                                        if (payload != null) {
                                            String className = payload.getClass().getName();
                                            if (className.equals("com.cobblemon.mod.common.net.messages.server.BenchMovePacket") ||
                                                className.equals("com.cobblemon.mod.common.net.messages.server.RequestMoveSwapPacket") ||
                                                className.equals("com.cobblemon.mod.common.net.messages.server.storage.SwapPCPartyPokemonPacket") ||
                                                className.equals("com.cobblemon.mod.common.net.messages.server.storage.party.MovePartyPokemonPacket") ||
                                                className.equals("com.cobblemon.mod.common.net.messages.server.storage.party.SwapPartyPokemonPacket") ||
                                                className.equals("com.cobblemon.mod.common.net.messages.server.storage.pc.MovePCPokemonPacket") ||
                                                className.equals("com.cobblemon.mod.common.net.messages.server.storage.pc.MovePCPokemonToPartyPacket") ||
                                                className.equals("com.cobblemon.mod.common.net.messages.server.storage.pc.MovePartyPokemonToPCPacket") ||
                                                className.equals("com.cobblemon.mod.common.net.messages.server.storage.pc.SwapPCPokemonPacket")) {
                                                
        if (player.getServer() != null && TournamentStateService.isPartyLockedForTournament(player.getServer(), player.getUUID())) {
                                                    TournamentMessages.sendRosterLocked(player);
                                                    io.netty.util.ReferenceCountUtil.release(msg);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                    super.channelRead(ctx, msg);
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            BigBangTournaments.LOGGER.error("Failed to inject packet handler for player " + player.getGameProfile().getName(), e);
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
