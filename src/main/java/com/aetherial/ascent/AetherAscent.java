package com.aetherial.ascent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AetherAscent.MOD_ID)
public final class AetherAscent {
    public static final String MOD_ID = "aether_ascent";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AetherAscent(@SuppressWarnings("unused") IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, AscentConfig.SERVER_SPEC, "aether-ascent-server.toml");

        NeoForge.EVENT_BUS.addListener(VerticalTransitionHandler::onPlayerTickPost);
        LOGGER.info("Aetherial Ascent chargé");
    }
}
