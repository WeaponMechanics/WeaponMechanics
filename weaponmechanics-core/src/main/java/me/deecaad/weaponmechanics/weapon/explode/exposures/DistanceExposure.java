package me.deecaad.weaponmechanics.weapon.explode.exposures;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.weaponmechanics.weapon.explode.shapes.ExplosionShape;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DistanceExposure implements ExplosionExposure {

    /**
     * Default constructor for serializer.
     */
    public DistanceExposure() {
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("weaponmechanics", "distance");
    }

    @NotNull @Override
    public Object2DoubleMap<LivingEntity> mapExposures(@NotNull Location origin, @NotNull ExplosionShape shape) {

        List<LivingEntity> entities = shape.getEntities(origin);
        Object2DoubleMap<LivingEntity> temp = new Object2DoubleOpenHashMap<>(entities.size());

        // The outer "shell" of the explosion
        double maxDistance = shape.getMaxDistance();

        for (LivingEntity entity : entities) {

            // Determine how far away the entity is from the explosion
            Vector vector = origin.toVector().subtract(entity.getLocation().toVector());
            double length = vector.length();

            double distanceRate = (maxDistance - length) / maxDistance;
            temp.put(entity, distanceRate);
        }

        return temp;
    }

    @Override
    public @NotNull ExplosionExposure serialize(@NotNull SerializeData data) throws SerializerException {
        return new DistanceExposure();
    }
}
