package me.deecaad.weaponmechanics.weapon.explode.shapes;

import me.deecaad.core.file.Serializer;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.List;

/**
 * This interface outlines the shape an explosion may take.
 * <p>
 * All subclasses of this interface should be registered in {@link ExplosionShapes}.
 */
public interface ExplosionShape extends Keyed, Serializer<ExplosionShape> {

    /**
     * Returns a list of all effected blocks effected by this shape if an explosion were to trigger at
     * the given <code>origin</code>. This list will not contain any air blocks, but may still contain
     * liquids/bedrock/ otherwise.
     * <p>
     * Implementations should not filter blocks out by material (Like IRON_BLOCK, DIRT, STONE). Instead,
     * let {@link me.deecaad.weaponmechanics.weapon.explode.BlockDamage} handle that. You may still
     * filter out "tough" blocks, like liquids, bedrock, or obsidian.
     *
     * @param origin The non-null origin of the explosion (Usually the center).
     * @return The non-null list of blocks contained in the explosion.
     */
    @NotNull List<Block> getBlocks(@NotNull Location origin);

    /**
     * Returns a list of all effect entities effected by this shape. An entity is effected if they are
     * contained in the area of the explosion.
     *
     * <p>
     * Data returned by this method is usually consumed by an
     * {@link me.deecaad.weaponmechanics.weapon.explode.exposures.ExplosionExposure}.
     *
     * @param origin The non-null origin of the explosion (Usually the center).
     * @return The non-null list of entities contained in the explosion.
     */
    List<LivingEntity> getEntities(@NotNull Location origin);

    /**
     * Returns the maximum distance from the origin of the explosion that an entity is considered to be
     * contained in the explosion. This method should <b>NOT</b> be used to determine whether a point is
     * in an explosion (use {@link #isContained(Location, Location)}). This method is useful for helping
     * to determine an entity's exposure. See
     * {@link me.deecaad.weaponmechanics.weapon.explode.exposures.DistanceExposure}.
     *
     * @return The maximum distance away an entity takes damage.
     */
    @Nonnegative
    double getMaxDistance();

    /**
     * Returns <code>true</code> if the given point is contained in this shape, assuming the shape's
     * center is located at <code>origin</code>.
     *
     * @param origin The non-null origin of the explosion.
     * @param point The non-null point to test.
     * @return True if the point is in this shape.
     */
    boolean isContained(@NotNull Location origin, @NotNull Location point);

    /**
     * Returns the area, measured in meters cubed (blocks cubed), of this shape. Note that the returned
     * value will be inaccurate to an unknown degree, since some shapes (like {@link DefaultExplosion})
     * are based on randomness and ray-tracing.
     *
     * @return The maximum area effected by this shape.
     */
    @Nonnegative
    double getArea();
}
