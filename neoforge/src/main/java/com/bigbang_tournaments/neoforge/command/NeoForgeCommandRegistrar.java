package com.bigbang_tournaments.neoforge.command;

import com.bigbang_tournaments.command.TournamentCommandRegistrar;
import com.bigbang_tournaments.neoforge.BigBangTournaments;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = BigBangTournaments.MODID)
public class NeoForgeCommandRegistrar {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        TournamentCommandRegistrar.register(dispatcher);
    }
}
