package me.deecaad.weaponmechanics.weapon.explode.exposures;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.compatibility.HitBox;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.explode.raytrace.Ray;
import me.deecaad.weaponmechanics.weapon.explode.raytrace.TraceCollision;
import me.deecaad.weaponmechanics.weapon.explode.raytrace.TraceResult;
import me.deecaad.weaponmechanics.weapon.explode.shapes.ExplosionShape;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DefaultExposure implements ExplosionExposure {

    /**
     * Default constructor for serializer.
     */
    public DefaultExposure() {
    }

    /**
     * Return the namespaced identifier for this object.
     *
     * @return this object's key
     */
    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "default");
    }

    @NotNull @Override
    public Object2DoubleMap<LivingEntity> mapExposures(@NotNull Location origin, @NotNull ExplosionShape shape) {
        List<LivingEntity> entities = shape.getEntities(origin);

        // Map to store all the calculated entities in
        Object2DoubleMap<LivingEntity> temp = new Object2DoubleOpenHashMap<>(entities.size());

        // How far away from the explosion to damage players
        double damageRadius = shape.getMaxDistance() * 2.0;

        // Gets data on the location of the explosion
        World world = origin.getWorld();
        double x = origin.getX();
        double y = origin.getY();
        double z = origin.getZ();

        MechanicsLogger debug = WeaponMechanics.getInstance().debugger;
        if (world == null) {
            debug.severe("Explosion in null world? Location: " + origin, "Please report error to devs");
            return temp;
        }

        Vector vector = new Vector(x, y, z);

        for (LivingEntity entity : entities) {
            Vector entityLocation = entity.getLocation().toVector();

            // Gets the "rate" or percentage of how far the entity
            // is from the explosion. For example, it the distance
            // is 8 and explosion radius is 10, the rate will be 1/5
            double impactRate = (damageRadius - entityLocation.distance(vector)) / damageRadius;

            if (impactRate > 1.0D) {
                debug.fine("Entity " + entity + " was just outside the blast radius");
                continue;
            }

            Vector betweenEntityAndExplosion = entityLocation.subtract(vector);
            double distance = betweenEntityAndExplosion.length();

            // If there is distance between the entity and the explosion
            if (distance != 0.0) {

                // Normalize
                betweenEntityAndExplosion.multiply(1 / distance);

                double exposure = getExposure(vector, entity);
                double impact = impactRate * exposure;

                temp.put(entity, impact);
            }
        }

        return temp;
    }

    /**
     * Gets a double [0, 1] representing how exposed the entity is to the explosion
     *
     * @param vec3d The origin of the explosion
     * @param entity The entity exposed to the explosion
     * @return The level of exposure of the entity to the epxlosion
     */
    private static double getExposure(Vector vec3d, Entity entity) {
        HitBox box = HitBox.getHitbox(entity);

        if (box == null) {
            return 0.0;
        }

        // Get the dimensions of the bounding box
        double width = box.getWidth();
        double height = box.getHeight();
        double depth = box.getDepth();

        // Gets the size of the grid in each axis
        double stepX = width * 2.0 + 1.0;
        double stepY = height * 2.0 + 1.0;
        double stepZ = depth * 2.0 + 1.0;
        double gridX = 1.0 / stepX;
        double gridY = 1.0 / stepY;
        double gridZ = 1.0 / stepZ;

        // Outside the grid
        if (gridX < 0.0 || gridY < 0.0 || gridZ < 0.0)
            return 0.0;

        double d3 = (1.0 - Math.floor(stepX) * gridX) / 2.0;
        double d4 = (1.0 - Math.floor(stepZ) * gridZ) / 2.0;

        // Setup variables for the loop
        World world = entity.getWorld();

        int successfulTraces = 0;
        int totalTraces = 0;

        // For each grid on the bounding box
        for (double x = 0; x <= 1; x += gridX) {
            for (double y = 0; y <= 1; y += gridY) {
                for (double z = 0; z <= 1; z += gridZ) {

                    double a = NumberUtil.lerp(box.getMinX(), box.getMaxX(), x);
                    double b = NumberUtil.lerp(box.getMinY(), box.getMaxY(), y);
                    double c = NumberUtil.lerp(box.getMinZ(), box.getMaxZ(), z);

                    // Calculates a path from the origin of the explosion
                    // (0, 0, 0) to the current grid on the entity's bounding
                    // box. The Vector is then ray traced to check for obstructions
                    Vector vector = new Vector(a + d3, b, c + d4).subtract(vec3d);

                    Ray ray = new Ray(vec3d.toLocation(world), vector);
                    TraceResult trace = ray.trace(TraceCollision.BLOCK, 0.3);
                    if (trace.getBlocks().isEmpty()) {
                        successfulTraces++;
                    }

                    totalTraces++;
                }
            }
        }

        // The percentage of successful traces
        return ((double) successfulTraces) / totalTraces;
    }

    @Override
    public @NotNull ExplosionExposure serialize(@NotNull SerializeData data) throws SerializerException {
        return new DefaultExposure();
    }
}
