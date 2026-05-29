package com.bigbang_tournaments.neoforge;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(BigBangTournaments.MODID)
public class BigBangTournaments {
    public static final String MODID = "bigbang_tournaments";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BigBangTournaments(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("BigBang Tournaments starting setup...");

        // Prevent trading when players are prepared for a tournament
        com.cobblemon.mod.common.api.events.CobblemonEvents.TRADE_EVENT_PRE.subscribe(
                com.cobblemon.mod.common.api.Priority.HIGHEST,
                new java.util.function.Consumer<com.cobblemon.mod.common.api.events.pokemon.TradeEvent.Pre>() {
                    @Override
                    public void accept(com.cobblemon.mod.common.api.events.pokemon.TradeEvent.Pre tradeEvent) {
                        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                        if (server == null) return;
                        java.util.UUID uuid1 = tradeEvent.getTradeParticipant1().getUuid();
                        java.util.UUID uuid2 = tradeEvent.getTradeParticipant2().getUuid();
                        if (com.bigbang_tournaments.storage.SnapshotStorage.hasSnapshot(server, uuid1) ||
                            com.bigbang_tournaments.storage.SnapshotStorage.hasSnapshot(server, uuid2)) {

                            tradeEvent.cancel();
                            net.minecraft.server.level.ServerPlayer p1 = server.getPlayerList().getPlayer(uuid1);
                            net.minecraft.server.level.ServerPlayer p2 = server.getPlayerList().getPlayer(uuid2);
                            if (p1 != null) {
                                p1.sendSystemMessage(net.minecraft.network.chat.Component.literal("Você não pode realizar trocas enquanto estiver no torneio!"));
                            }
                            if (p2 != null) {
                                p2.sendSystemMessage(net.minecraft.network.chat.Component.literal("Você não pode realizar trocas enquanto estiver no torneio!"));
                            }
                        }
                    }
                }
        );

        // Prevent releasing Pokémons when players are prepared for a tournament
        com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_RELEASED_EVENT_PRE.subscribe(
                com.cobblemon.mod.common.api.Priority.HIGHEST,
                new java.util.function.Consumer<com.cobblemon.mod.common.api.events.storage.ReleasePokemonEvent.Pre>() {
                    @Override
                    public void accept(com.cobblemon.mod.common.api.events.storage.ReleasePokemonEvent.Pre releaseEvent) {
                        net.minecraft.server.level.ServerPlayer player = releaseEvent.getPlayer();
                        net.minecraft.server.MinecraftServer server = player.getServer();
                        if (server != null && com.bigbang_tournaments.storage.SnapshotStorage.hasSnapshot(server, player.getUUID())) {
                            releaseEvent.cancel();
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Você não pode liberar Pokémons durante o torneio!"));
                        }
                    }
                }
        );
    }
}
