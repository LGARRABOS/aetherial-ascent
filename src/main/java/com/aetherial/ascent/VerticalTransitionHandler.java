package com.aetherial.ascent;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class VerticalTransitionHandler {
    public static final ResourceKey<Level> AETHER = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("aether", "the_aether"));

    private static final String COOLDOWN_TAG = "aether_ascent:cooldown";

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

        final int overworldGate = AscentConfig.SERVER.overworldGateMinY.get();
        final int aetherGate = AscentConfig.SERVER.aetherGateMaxY.get();

        final TransitionKind kind;
        if (level.dimension() == Level.OVERWORLD && y > overworldGate) {
            kind = TransitionKind.TO_AETHER;
        } else if (level.dimension().equals(AETHER) && y < aetherGate) {
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
        final Optional<Double> tyOpt = resolveArrivalY(destination, tx, tz, root, kind);
        if (tyOpt.isEmpty()) {
            AetherAscent.LOGGER.warn(
                    "Aucun emplacement libre pour l’engin (union hitboxes) à ({}, ?, {}) en {} — passage annulé.",
                    tx,
                    tz,
                    destination.dimension().location());
            player.sendSystemMessage(
                    Component.literal(
                            "Pas assez de place pour faire apparaître l’engin à l’arrivée. Dégagez le sol ou le ciel sous (x,z) miroir."));
            return;
        }
        final double ty = tyOpt.get();
        final float yRot = root.getYRot();
        final float xRot = root.getXRot();

        setCooldown(root);
        final Vec3 motion = root.getDeltaMovement();
        final Vec3 destPos = new Vec3(tx, ty, tz);

        // Flux vanilla (respawn + chargement) : ServerPlayer et passagers via Entity#changeDimension.
        final DimensionTransition.PostDimensionTransition after = buildPostTransition(motion, destination);
        final DimensionTransition transition = new DimensionTransition(
                destination,
                destPos,
                motion,
                yRot,
                xRot,
                false,
                DimensionTransition.PLAY_PORTAL_SOUND
                        .then(DimensionTransition.PLACE_PORTAL_TICKET)
                        .then(after));

        if (root.changeDimension(transition) == null) {
            root.getPersistentData().remove(COOLDOWN_TAG);
            AetherAscent.LOGGER.warn("Changement de dimension refusé pour {} ({})", root, kind);
        }
    }

    /**
     * Calcule une hauteur d’arrivée en tenant compte de la taille réelle de l’ensemble (véhicule + passagers
     * embarqués) : union des hitboxes, puis position minimale pour que le bas du volume reste au‑dessus du sol
     * (heightmap), enfin vérification {@link Level#noCollision(Entity, AABB)} dans le monde cible avec
     * {@code entity = null} pour ne pas dépendre du dimension actuel du root.
     */
    private static Optional<Double> resolveArrivalY(
            ServerLevel dest,
            double tx,
            double tz,
            Entity root,
            TransitionKind kind) {
        final AABB union = unionMountedBounds(root);
        final double bottomOffset = root.getY() - union.minY;

        final double baseTy = switch (kind) {
            case TO_AETHER -> {
                final int ix = BlockPos.containing(tx, 0, tz).getX();
                final int iz = BlockPos.containing(tx, 0, tz).getZ();
                final int clearance = AscentConfig.SERVER.aetherGroundClearanceBlocks.get();
                final double minBottom = AscentConfig.SERVER.minArrivalBottomY.get();
                final int surface = dest.getHeight(Heightmap.Types.MOTION_BLOCKING, ix, iz);
                final double tyTerrain = surface + clearance + bottomOffset;
                yield Math.max(tyTerrain, bottomOffset + minBottom);
            }
            case TO_OVERWORLD -> {
                final int landing = AscentConfig.SERVER.overworldLandingY.get();
                final double minBottom = AscentConfig.SERVER.minArrivalBottomY.get();
                yield Math.max(landing, bottomOffset + minBottom);
            }
        };

        return findClearArrivalY(dest, root, union, tx, tz, baseTy);
    }

    /**
     * Enveloppe monde du convoi : racine + passagers récursifs (ballon Create + joueur, etc.).
     */
    private static AABB unionMountedBounds(Entity entity) {
        AABB box = entity.getBoundingBox();
        for (Entity passenger : entity.getPassengers()) {
            box = box.minmax(unionMountedBounds(passenger));
        }
        return box;
    }

    /**
     * Cherche une hauteur où {@code union} translate au‑dessus de (tx, tz) ne chevauche ni blocs solides ni autres
     * entités dans le monde cible.
     */
    private static Optional<Double> findClearArrivalY(
            ServerLevel dest,
            Entity root,
            AABB union,
            double tx,
            double tz,
            double startTy) {
        final double dx = tx - root.getX();
        final double dz = tz - root.getZ();
        final double pad = AscentConfig.SERVER.arrivalCollisionPadding.get();
        final AABB unionPadded = union.inflate(pad);
        final int minY = dest.getMinBuildHeight();
        final int maxY = dest.getMaxBuildHeight();
        final int maxSearch = AscentConfig.SERVER.maxVerticalArrivalSearch.get();

        for (int step = 0; step < maxSearch; step++) {
            final double ty = startTy + step;
            final AABB atDest = unionPadded.move(dx, ty - root.getY(), dz);
            if (atDest.minY < minY || atDest.maxY > maxY) {
                continue;
            }
            if (dest.noCollision(null, atDest)) {
                return Optional.of(ty);
            }
        }
        return Optional.empty();
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
        final int cooldownTicks = cooldownTicks();
        if (cooldownTicks <= 0) {
            return false;
        }
        final long now = root.level().getGameTime();
        return root.getPersistentData().contains(COOLDOWN_TAG)
                && now < root.getPersistentData().getLong(COOLDOWN_TAG);
    }

    private static void setCooldown(Entity root) {
        final int cooldownTicks = cooldownTicks();
        if (cooldownTicks <= 0) {
            return;
        }
        final long deadline = root.level().getGameTime() + cooldownTicks;
        root.getPersistentData().putLong(COOLDOWN_TAG, deadline);
    }

    private static int cooldownTicks() {
        return Math.max(0, AscentConfig.SERVER.transitionCooldownSeconds.get()) * 20;
    }

    /**
     * {@link ServerPlayer#changeDimension} n’applique pas {@link DimensionTransition#speed()} — on réinjecte la
     * vélocité ici. Les effets ne s’appliquent qu’une fois (uniquement pour l’entité joueur : le callback véhicule est
     * ignoré).
     */
    private static DimensionTransition.PostDimensionTransition buildPostTransition(Vec3 motion, ServerLevel afterLevel) {
        return entity -> {
            entity.setDeltaMovement(motion);
            if (entity instanceof ServerPlayer sp) {
                applyTransitionPresentation(sp, afterLevel);
            }
        };
    }

    private static void applyTransitionPresentation(ServerPlayer player, ServerLevel afterLevel) {
        final double sec = AscentConfig.SERVER.transitionPresentationSeconds.get();
        final int ticks = Mth.floor(sec * 20.0);
        if (ticks <= 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                ticks,
                0,
                false,
                false,
                true));
        player.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                ticks,
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
