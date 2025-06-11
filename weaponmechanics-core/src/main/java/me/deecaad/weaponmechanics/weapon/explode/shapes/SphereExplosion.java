package me.deecaad.weaponmechanics.weapon.explode.shapes;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SphereExplosion implements ExplosionShape {

    private double radius;
    private double radiusSquared;

    /**
     * Default constructor for serializer.
     */
    public SphereExplosion() {
    }

    public SphereExplosion(double radius) {
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    /**
     * Return the namespaced identifier for this object.
     *
     * @return this object's key
     */
    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(WeaponMechanics.getInstance(), "sphere");
    }

    @Override
    public @NotNull List<Block> getBlocks(@NotNull Location origin) {
        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        MechanicsLogger debug = WeaponMechanics.getInstance().debugger;
        List<Block> temp = new ArrayList<>();

        Location pos1 = origin.clone().add(-radius, -radius, -radius);
        Location pos2 = origin.clone().add(+radius, +radius, +radius);

        double noiseDistance = NumberConversions.square(config.getDouble("Explosions.Spherical.Noise_Distance", 1.0));
        double noiseChance = config.getDouble("Explosions.Spherical.Noise_Chance", 0.10);

        // Loops through a cuboid region between pos1 and pos2
        // effectively looping through every single block inside
        // of a square
        for (int x = pos1.getBlockX(); x < pos2.getBlockX(); x++) {
            for (int y = pos1.getBlockY(); y < pos2.getBlockY(); y++) {
                for (int z = pos1.getBlockZ(); z < pos2.getBlockZ(); z++) {
                    Location loc = new Location(origin.getWorld(), x, y, z);

                    // If the distance between the current iteration
                    // and the origin is less than the radius of the
                    // sphere. This "reshapes" the cube into a sphere
                    double distance = loc.distanceSquared(origin);
                    if (distance <= radiusSquared) {

                        boolean isNearEdge = radiusSquared - distance < noiseDistance;
                        if (isNearEdge && RandomUtil.chance(noiseChance)) {
                            if (debug.canLog(Level.FINEST))
                                debug.finest("Skipping block (" + x + ", " + y + ", " + z + ") due to noise.");
                            continue; // outer noise checker
                        }

                        temp.add(loc.getBlock());
                    }
                }
            }
        }
        return temp;
    }

    @NotNull @Override
    public List<LivingEntity> getEntities(@NotNull Location origin) {
        return origin.getWorld().getLivingEntities()
            .stream()
            .filter(entity -> entity.getLocation().distanceSquared(origin) < radiusSquared)
            .collect(Collectors.toList());
    }

    @Override
    public double getMaxDistance() {
        return radius;
    }

    @Override
    public boolean isContained(@NotNull Location origin, @NotNull Location point) {
        return origin.distanceSquared(point) < radiusSquared;
    }

    @Override
    public double getArea() {
        return 4.0 / 3.0 * Math.PI * radius * radius * radius;
    }

    @Override
    public @NotNull ExplosionShape serialize(@NotNull SerializeData data) throws SerializerException {
        double radius = data.of("Radius").assertExists().assertRange(0.0, null).getDouble().getAsDouble();
        return new SphereExplosion(radius);
    }

    @Override
    public String toString() {
        return "SphericalExplosion{" +
            "radius=" + radius +
            '}';
    }
}
