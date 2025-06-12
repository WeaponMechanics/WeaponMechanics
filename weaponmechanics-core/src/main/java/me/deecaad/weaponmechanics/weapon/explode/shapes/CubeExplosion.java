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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This explosion defines a rectangular prism shaped explosion, where a valid (non-negative) width
 * and height are specified.
 */
public class CubeExplosion implements ExplosionShape {

    // These are set to be half the actual
    // values, kind of like radius
    private double width;
    private double height;

    /**
     * Default constructor for serializer.
     */
    public CubeExplosion() {
    }

    /**
     * Constructs a <code>CuboidExplosion</code> object. The width and height are divided by 2 to get
     * the "radius" of the rectangle.
     * <p>
     * This is used when finding points about the origin, the width input is the total width of the
     * explosion. The width instance variable is the distance from one point to the origin.
     *
     * @param width Total width of the explosion
     * @param height Total height of the explosion
     */
    public CubeExplosion(double width, double height) {
        this.width = width / 2.0;
        this.height = height / 2.0;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("weaponmechanics", "cube");
    }

    /**
     * Gets all blocks between 2 points, where the 2 points are found using this width, and height as
     * well as the method's origin parameter.
     *
     * @param origin The center of the explosion Should not be null
     * @return The blocks within the explosion
     */
    @NotNull @Override
    public List<Block> getBlocks(@NotNull Location origin) {
        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        MechanicsLogger debug = WeaponMechanics.getInstance().debugger;
        List<Block> temp = new ArrayList<>((int) (2 * width + 2 * height) + 1);

        double noiseDistance = config.getDouble("Explosions.Spherical.Noise_Distance", 1.25);
        double noiseChance = config.getDouble("Explosions.Spherical.Noise_Chance", 0.25);

        World world = origin.getWorld();
        if (world == null) {
            debug.severe("Cuboid explosion's origin was null? Origin:", origin.toString());
            return temp;
        }
        int blockX = origin.getBlockX();
        int blockY = origin.getBlockY();
        int blockZ = origin.getBlockZ();

        for (int x = (int) -width; x < width; x++) {
            for (int y = (int) -height; y < height; y++) {
                for (int z = (int) -width; z < width; z++) {

                    // Noise checker
                    if (isNearEdge(x, y, z, noiseDistance) && RandomUtil.chance(noiseChance)) {
                        debug.fine("Skipping block(" + x + ", " + y + ", " + z + ") due to noise.");
                        continue; // outer noise checker
                    }

                    temp.add(world.getBlockAt(x + blockX, y + blockY, z + blockZ));
                }
            }
        }
        return temp;
    }

    /**
     * Gets any <code>LivingEntity</code>s inside this <code>CuboidExplosion</code> by getting the
     * entity's (x, y, z) coordinates, and checking if that point is in between the 2 points. This is
     * done via
     *
     * <blockquote>
     * 
     * <pre>{@code
     *     point1.x <= entity.x <= point2.x
     *     point1.y <= entity.y <= point2.y
     *     point1.z <= entity.z <= point2.z
     * }</pre>
     * 
     * </blockquote>
     *
     * @param origin The center of the explosion Should not be null
     * @return All entities within the explosion
     */
    @NotNull @Override
    public List<LivingEntity> getEntities(@NotNull Location origin) {
        double xMin = origin.getX() - width, xMax = origin.getX() + width;
        double yMin = origin.getY() - height, yMax = origin.getY() + height;
        double zMin = origin.getZ() - width, zMax = origin.getZ() + width;

        List<LivingEntity> all = origin.getWorld().getLivingEntities();
        List<LivingEntity> temp = new ArrayList<>(all.size());
        for (LivingEntity entity : all) {
            double x = entity.getLocation().getX();
            double y = entity.getLocation().getY();
            double z = entity.getLocation().getZ();

            boolean in = x >= xMin && x <= xMax &&
                y >= yMin && y <= yMax &&
                z >= zMin && z <= zMax;

            if (in)
                temp.add(entity);
        }
        return temp;
    }

    @Override
    public double getMaxDistance() {

        // Return a triangle to the furthest corner
        return Math.sqrt(width * width + height * height);
    }

    @Override
    public double getArea() {

        // Simple cube area, length * width * height
        return width * width * height;
    }

    @Override
    public boolean isContained(@NotNull Location origin, @NotNull Location point) {
        double x1 = origin.getX() - width;
        double y1 = origin.getY() - height;
        double z1 = origin.getZ() - width;
        double x2 = origin.getX() + width;
        double y2 = origin.getY() + height;
        double z2 = origin.getZ() + width;

        return point.getX() > x1 && point.getX() < x2 &&
            point.getY() > y1 && point.getY() < y2 &&
            point.getZ() > z1 && point.getZ() < z2;
    }

    public boolean isNearEdge(double x, double y, double z, double distance) {
        x = width - Math.abs(x);
        y = height - Math.abs(y);
        z = width - Math.abs(z);

        return x < distance ||
            y < distance ||
            z < distance;
    }

    @Override
    public @NotNull ExplosionShape serialize(@NotNull SerializeData data) throws SerializerException {
        double width = data.of("Width").assertExists().getDouble().getAsDouble();
        double height = data.of("Height").assertExists().getDouble().getAsDouble();

        return new CubeExplosion(width, height);
    }

    @Override
    public String toString() {
        return "CuboidExplosion{" +
            "width=" + width +
            ", height=" + height +
            '}';
    }
}
