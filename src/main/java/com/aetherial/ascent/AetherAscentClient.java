package com.aetherial.ascent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Entrée client uniquement : bouton « Config » dans l’écran des mods. Le serveur dédié ne charge pas cette classe.
 */
@Mod(value = AetherAscent.MOD_ID, dist = Dist.CLIENT)
public final class AetherAscentClient {
    public AetherAscentClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
