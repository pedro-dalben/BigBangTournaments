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
    }
}
