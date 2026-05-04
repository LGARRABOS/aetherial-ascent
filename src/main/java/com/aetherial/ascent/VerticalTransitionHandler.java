package com.aetherial.ascent;

import java.util.Collections;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class VerticalTransitionHandler {
    public static final ResourceKey<Level> AETHER = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("aether", "the_aether"));

    private static final String COOLDOWN_TAG = "aether_ascent:cooldown";
    private static final int COOLDOWN_TICKS = 5 * 20;
    private static final int TRANSITION_EFFECT_TICKS = 2 * 20;
    /** Blocs au-dessus de la heightmap (pas de plancher artificiel : suit le terrain / le vide local). */
    private static final int AETHER_ARRIVAL_CLEARANCE = 4;
    /** Seuil minimal uniquement pour éviter y≤1 (comportement natif autour de la couche 0), pas un « plancher » de jeu. */
    private static final double AETHER_ARRIVAL_EPSILON_MIN_Y = 2.0;

    private VerticalTransitionHandler() {
    }

    public static void onPlayerTickPost(final PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }

        final Level level = player.level();
        final double y = player.getY();

        final TransitionKind kind;
        if (level.dimension() == Level.OVERWORLD && y > 500.0) {
            kind = TransitionKind.TO_AETHER;
        } else if (level.dimension().equals(AETHER) && y < -2.0) {
            kind = TransitionKind.TO_OVERWORLD;
        } else {
            return;
        }

        final Entity root = resolveTransportRoot(player);
        if (isOnCooldown(root)) {
            return;
        }

        final ServerLevel destination = switch (kind) {
            case TO_AETHER -> player.serverLevel().getServer().getLevel(AETHER);
            case TO_OVERWORLD -> player.serverLevel().getServer().getLevel(Level.OVERWORLD);
        };

        if (destination == null) {
            AetherAscent.LOGGER.warn("Dimension cible absente pour {}", kind);
            return;
        }

        final double tx = root.getX();
        final double tz = root.getZ();
        final double ty = switch (kind) {
            case TO_AETHER -> resolveAetherArrivalY(destination, tx, tz);
            case TO_OVERWORLD -> 450.0;
        };
        final float yRot = root.getYRot();
        final float xRot = root.getXRot();

        setCooldown(root);
        final var motion = root.getDeltaMovement();

        teleportRoot(root, destination, tx, ty, tz, yRot, xRot);
        root.setDeltaMovement(motion);

        applyTransitionPresentation(player, destination);
    }

    /**
     * Hauteur d’arrivée : sol local (heightmap) + léger dégagement. Pas de plancher type y=72 : un engin Create
     * Aeronautics reste cohérent avec le relief réel sous (x,z). On ne force qu’un minimum très bas pour ne pas
     * retomber exactement sur la zone y≈0 déclenchant le renvoi Overworld natif.
     */
    private static double resolveAetherArrivalY(ServerLevel aether, double x, double z) {
        final int ix = BlockPos.containing(x, 0, z).getX();
        final int iz = BlockPos.containing(x, 0, z).getZ();
        final int surface = aether.getHeight(Heightmap.Types.MOTION_BLOCKING, ix, iz);
        return Math.max(surface + AETHER_ARRIVAL_CLEARANCE, AETHER_ARRIVAL_EPSILON_MIN_Y);
    }

    private static Entity resolveTransportRoot(ServerPlayer player) {
        final Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return player;
        }
        final Entity root = player.getRootVehicle();
        return root != null ? root : vehicle;
    }

    private static boolean isOnCooldown(Entity root) {
        final long now = root.level().getGameTime();
        return root.getPersistentData().contains(COOLDOWN_TAG)
                && now < root.getPersistentData().getLong(COOLDOWN_TAG);
    }

    private static void setCooldown(Entity root) {
        final long deadline = root.level().getGameTime() + COOLDOWN_TICKS;
        root.getPersistentData().putLong(COOLDOWN_TAG, deadline);
    }

    private static void teleportRoot(
            Entity root,
            ServerLevel dest,
            double x,
            double y,
            double z,
            float yRot,
            float xRot) {
        if (root instanceof ServerPlayer sp) {
            sp.teleportTo(dest, x, y, z, yRot, xRot);
        } else {
            root.teleportTo(dest, x, y, z, Collections.<RelativeMovement>emptySet(), yRot, xRot);
        }
    }

    private static void applyTransitionPresentation(ServerPlayer player, ServerLevel afterLevel) {
        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                TRANSITION_EFFECT_TICKS,
                0,
                false,
                false,
                true));
        player.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                TRANSITION_EFFECT_TICKS,
                0,
                false,
                false,
                true));
        afterLevel.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ELYTRA_FLYING,
                SoundSource.PLAYERS,
                1.0F,
                1.55F);
    }

    private enum TransitionKind {
        TO_AETHER,
        TO_OVERWORLD
    }
}
