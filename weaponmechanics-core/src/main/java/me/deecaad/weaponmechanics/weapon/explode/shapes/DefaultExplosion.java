package me.deecaad.weaponmechanics.weapon.explode.shapes;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This explosion uses the minecraft explosion system, instead of using a shape.
 */
public class DefaultExplosion implements ExplosionShape {

    private static final double DECAY_RATE = 0.3;
    private static final double ABSORB_RATE = 0.3;

    private float yield;
    private int gridSize;
    private int bound;

    /**
     * Default constructor for serializer.
     */
    public DefaultExplosion() {
    }

    /**
     * Constructs a DefaultExplosion object.
     *
     * @param yield The yield of the explosion, typically 4.0.
     * @param gridSize How many grids on the cube, typically 16.
     */
    public DefaultExplosion(double yield, int gridSize) {
        this.yield = (float) yield;
        this.gridSize = gridSize;
        this.bound = gridSize - 1;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey("weaponmechanics", "default");
    }

    @NotNull @Override
    public List<Block> getBlocks(@NotNull Location origin) {
        if (origin.getWorld() == null)
            throw new IllegalArgumentException("origin is null");

        // If the explosion is too small, then no blocks are destroyed
        if (yield < 0.1F)
            return List.of();

        World world = origin.getWorld();
        Set<Block> set = new HashSet<>();

        // Separates the explosion into a 16 by 16 by 16
        // grid.
        for (int k = 0; k < gridSize; ++k) {
            for (int i = 0; i < gridSize; ++i) {
                for (int j = 0; j < gridSize; ++j) {

                    // Checking if the the point defined by (k, i, j) is on the grid
                    if (k == 0 || k == bound || i == 0 || i == bound || j == 0 || j == bound) {

                        Vector vector = new Vector(((double) k) / bound * 2 - 1, ((double) i) / bound * 2 - 1, ((double) j) / bound * 2 - 1);
                        vector.normalize();

                        double x = origin.getX();
                        double y = origin.getY();
                        double z = origin.getZ();

                        // Slightly randomized intensity, based on the yield of the explosion
                        float intensity = yield * (0.7F + ThreadLocalRandom.current().nextFloat() * 0.6F);

                        while (intensity > 0.0f) {
                            Block block = world.getBlockAt((int) x, (int) y, (int) z);

                            if (!block.isEmpty()) {
                                float resistance = block.getType().getBlastResistance();

                                intensity -= (float) ((resistance + 0.3F) * ABSORB_RATE);
                            }

                            if (intensity > 0.0F && y < 256 && y >= 0) {
                                set.add(block);
                            }

                            x += vector.getX() * DECAY_RATE;
                            y += vector.getY() * DECAY_RATE;
                            z += vector.getZ() * DECAY_RATE;

                            // Ray decays over longer distance
                            intensity -= (float) (DECAY_RATE * 0.75);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    @Override
    public List<LivingEntity> getEntities(@NotNull Location origin) {

        // How far away from the explosion to damage players
        double damageRadius = yield * 2.0F;
        double damageRadiusOuter = damageRadius + 1;

        World world = origin.getWorld();
        if (world == null) {
            WeaponMechanics.getInstance().debugger.severe("Explosion in null world? Location: " + origin, "Please report error to devs");
            return null;
        }

        ArrayList<LivingEntity> entities = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(origin, damageRadiusOuter, damageRadiusOuter, damageRadiusOuter)) {
            if (entity.getType().isAlive()) {
                entities.add((LivingEntity) entity);
            }
        }

        return entities;
    }

    @Override
    public double getMaxDistance() {
        return yield * 2f;
    }

    @Override
    public boolean isContained(@NotNull Location origin, @NotNull Location point) {

        // During explosions, yield is randomly multiplied by numbers [0.7, 1.3],
        // so using yield is a fairly accurate estimate on the radius.
        return origin.distanceSquared(point) < (yield * yield);
    }

    @Override
    public double getArea() {
        // Sphere volume estimate, 4/3 * PI * r^3
        return 4.0 / 3.0 * Math.PI * yield * yield * yield;
    }

    @Override
    public @NotNull ExplosionShape serialize(@NotNull SerializeData data) throws SerializerException {
        double yield = data.of("Yield").assertExists().assertRange(0.0, null).getDouble().getAsDouble();
        int gridSize = data.of("Rays").assertRange(1, null).getInt().orElse(16);

        return new DefaultExplosion(yield, gridSize);
    }

    @Override
    public String toString() {
        return "DefaultExplosion{" +
            "yield=" + yield +
            ", gridSize=" + gridSize +
            '}';
    }
}
