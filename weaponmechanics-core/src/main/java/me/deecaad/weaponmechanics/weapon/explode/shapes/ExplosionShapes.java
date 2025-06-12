package me.deecaad.weaponmechanics.weapon.explode.shapes;

import me.deecaad.core.utils.MutableRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * All {@link ExplosionShape explosion shapes} that can be used in the
 * {@link me.deecaad.weaponmechanics.weapon.explode.Explosion}'s serializer.
 */
public final class ExplosionShapes {

    public static final @NotNull MutableRegistry<ExplosionShape> REGISTRY = new MutableRegistry.SimpleMutableRegistry<>(Map.of());

    public static final @NotNull ExplosionShape CUBE = register(new CubeExplosion());
    public static final @NotNull ExplosionShape DEFAULT = register(new DefaultExplosion());
    public static final @NotNull ExplosionShape PARABOLA = register(new ParabolaExplosion());
    public static final @NotNull ExplosionShape SPHERE = register(new SphereExplosion());

    // Don't let anyone instantiate this class
    private ExplosionShapes() {
    }

    private static @NotNull ExplosionShape register(@NotNull ExplosionShape explosionShape) {
        REGISTRY.add(explosionShape);
        return explosionShape;
    }
}
