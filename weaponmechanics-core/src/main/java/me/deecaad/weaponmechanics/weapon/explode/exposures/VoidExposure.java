package me.deecaad.weaponmechanics.weapon.explode.exposures;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.weaponmechanics.weapon.explode.shapes.ExplosionShape;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VoidExposure implements ExplosionExposure {

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("weaponmechanics", "void");
    }

    @NotNull @Override
    public Object2DoubleMap<LivingEntity> mapExposures(@NotNull Location origin, @NotNull ExplosionShape shape) {

        List<LivingEntity> entities = shape.getEntities(origin);
        Object2DoubleMap<LivingEntity> exposures = new Object2DoubleOpenHashMap<>(entities.size());

        for (LivingEntity entity : entities) {
            exposures.put(entity, 1.0);
        }

        return exposures;
    }

    @Override
    public @NotNull ExplosionExposure serialize(@NotNull SerializeData data) throws SerializerException {
        return new VoidExposure();
    }
}
