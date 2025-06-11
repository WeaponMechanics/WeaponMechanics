package me.deecaad.weaponmechanics.weapon.explode.exposures;

import me.deecaad.core.utils.MutableRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * All {@link ExplosionExposure explosion exposures} that can be used in the
 * {@link me.deecaad.weaponmechanics.weapon.explode.Explosion}'s serializer.
 */
public final class ExplosionExposures {

    public static final @NotNull MutableRegistry<ExplosionExposure> REGISTRY = new MutableRegistry.SimpleMutableRegistry<>(Map.of());

    public static final @NotNull ExplosionExposure DEFAULT = register(new DefaultExposure());
    public static final @NotNull ExplosionExposure DISTANCE = register(new DistanceExposure());
    public static final @NotNull ExplosionExposure OPTIMIZED = register(new OptimizedExposure());
    public static final @NotNull ExplosionExposure VOID = register(new VoidExposure());


    // Don't let anyone instantiate this class
    private ExplosionExposures() {
    }

    private static @NotNull ExplosionExposure register(@NotNull ExplosionExposure explosionExposure) {
        REGISTRY.add(explosionExposure);
        return explosionExposure;
    }
}
