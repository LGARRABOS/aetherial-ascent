package com.aetherial.ascent;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration serveur (fichier {@code serverconfig/aether-ascent-server.toml} par monde) ; modifiable en jeu via
 * Mods → Aetherial Ascent → Config.
 */
public final class AscentConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        final Pair<Server, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = pair.getLeft();
        SERVER_SPEC = pair.getRight();
    }

    public static final class Server {
        public final ModConfigSpec.IntValue overworldGateMinY;
        public final ModConfigSpec.IntValue aetherGateMaxY;
        public final ModConfigSpec.IntValue overworldLandingY;
        public final ModConfigSpec.IntValue transitionCooldownSeconds;
        public final ModConfigSpec.DoubleValue transitionPresentationSeconds;
        public final ModConfigSpec.IntValue aetherGroundClearanceBlocks;
        public final ModConfigSpec.DoubleValue minArrivalBottomY;
        public final ModConfigSpec.DoubleValue arrivalCollisionPadding;
        public final ModConfigSpec.IntValue maxVerticalArrivalSearch;

        Server(final ModConfigSpec.Builder builder) {
            builder.translation("aether_ascent.configuration.section.vertical").push("vertical");

            overworldGateMinY = builder
                    .comment("Dans l'Overworld, déclenche le passage vers l'Éther si la position Y du convoi est STRICTEMENT supérieure à cette valeur.")
                    .translation("aether_ascent.configuration.vertical.overworldGateMinY")
                    .defineInRange("overworldGateMinY", 500, -2048, 4096);

            aetherGateMaxY = builder
                    .comment("Dans l'Éther, déclenche le retour Overworld si la position Y du convoi est STRICTEMENT inférieure à cette valeur.")
                    .translation("aether_ascent.configuration.vertical.aetherGateMaxY")
                    .defineInRange("aetherGateMaxY", -2, -1024, 512);

            overworldLandingY = builder
                    .comment("Hauteur Y d'arrivée dans l'Overworld (avant ajustement collision / engin).")
                    .translation("aether_ascent.configuration.vertical.overworldLandingY")
                    .defineInRange("overworldLandingY", 450, -64, 2000);

            transitionCooldownSeconds = builder
                    .comment("Délai minimum entre deux passages, en secondes (0 = désactivé).")
                    .translation("aether_ascent.configuration.vertical.transitionCooldownSeconds")
                    .defineInRange("transitionCooldownSeconds", 5, 0, 300);

            transitionPresentationSeconds = builder
                    .comment("Durée des effets visuels (cécité, chute lente) après le voyage, en secondes.")
                    .translation("aether_ascent.configuration.vertical.transitionPresentationSeconds")
                    .defineInRange("transitionPresentationSeconds", 2.0, 0.0, 30.0);

            aetherGroundClearanceBlocks = builder
                    .comment("Blocs libres au-dessus de la heightmap (MOTION_BLOCKING) à l'arrivée dans l'Éther.")
                    .translation("aether_ascent.configuration.vertical.aetherGroundClearanceBlocks")
                    .defineInRange("aetherGroundClearanceBlocks", 4, 0, 64);

            minArrivalBottomY = builder
                    .comment("Le bas du volume (engin + passagers) ne sera pas placé sous cette coordonnée Y absolue.")
                    .translation("aether_ascent.configuration.vertical.minArrivalBottomY")
                    .defineInRange("minArrivalBottomY", 2.0, -2048.0, 4096.0);

            arrivalCollisionPadding = builder
                    .comment("Marge ajoutée au test de collision à l'arrivée (évite les contacts tangentiels).")
                    .translation("aether_ascent.configuration.vertical.arrivalCollisionPadding")
                    .defineInRange("arrivalCollisionPadding", 0.05, 0.0, 1.0);

            maxVerticalArrivalSearch = builder
                    .comment("Nombre maximum de blocs recherchés vers le haut si l'arrivée directe est obstruée.")
                    .translation("aether_ascent.configuration.vertical.maxVerticalArrivalSearch")
                    .defineInRange("maxVerticalArrivalSearch", 384, 8, 4096);

            builder.pop();
        }
    }

    private AscentConfig() {
    }
}
