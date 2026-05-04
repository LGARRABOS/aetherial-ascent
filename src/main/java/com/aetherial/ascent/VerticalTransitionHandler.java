package com.aetherial.ascent;

import java.util.Collections;

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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class VerticalTransitionHandler {
    public static final ResourceKey<Level> AETHER = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("aether", "the_aether"));

    private static final String COOLDOWN_TAG = "aether_ascent:cooldown";
    private static final int COOLDOWN_TICKS = 5 * 20;
    private static final int TRANSITION_EFFECT_TICKS = 2 * 20;

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
        final double ty = kind == TransitionKind.TO_AETHER ? 0.0 : 450.0;
        final double tz = root.getZ();
        final float yRot = root.getYRot();
        final float xRot = root.getXRot();

        setCooldown(root);
        final var motion = root.getDeltaMovement();

        teleportRoot(root, destination, tx, ty, tz, yRot, xRot);
        root.setDeltaMovement(motion);

        applyTransitionPresentation(player, destination);
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
