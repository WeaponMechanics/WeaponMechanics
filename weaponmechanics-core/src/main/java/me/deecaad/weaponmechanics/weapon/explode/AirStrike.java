package me.deecaad.weaponmechanics.weapon.explode;

import com.cjcrafter.foliascheduler.TaskImplementation;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.Projectile;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AirStrike implements Serializer<AirStrike> {

    private Projectile projectile;

    private int min;
    private int max;
    private double height;
    private double yVariation;
    private double distanceBetweenSquared;
    private double radius;
    private int loops;
    private int delay;
    private Detonation detonation;
    private MechanicManager mechanics;

    /**
     * Default constructor for serializer
     */
    public AirStrike() {
    }

    /**
     * See arguments.
     *
     * @param projectile The non-null projectile to spawn for each bomb.
     * @param min The minimum number of bombs to spawn (per layer).
     * @param max The maximum number of bombs to spawn (per layer).
     * @param height The vertical distance above the initial projectile to spawn the layers.
     * @param yVariation The random variations in the <code>height</code> parameter.
     * @param distance The minimum distance between bombs.
     * @param radius The maximum horizontal distance away from the initial projectile that a bomb is
     *        allowed to spawn. Larger numbers means higher spread.
     * @param loops The number of layers of bombs to spawn.
     * @param delay The amount of time (in ticks) between each layer of bombs.
     */
    public AirStrike(Projectile projectile, int min, int max, double height, double yVariation,
        double distance, double radius, int loops, int delay, Detonation detonation, MechanicManager mechanics) {

        this.projectile = projectile;
        this.min = min;
        this.max = max;
        this.height = height;
        this.yVariation = yVariation;
        this.distanceBetweenSquared = distance * distance;
        this.radius = radius;
        this.loops = loops;
        this.delay = delay;
        this.detonation = detonation;
        this.mechanics = mechanics;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public void setProjectile(Projectile projectile) {
        this.projectile = projectile;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getyVariation() {
        return yVariation;
    }

    public void setyVariation(double yVariation) {
        this.yVariation = yVariation;
    }

    public double getDistanceBetweenSquared() {
        return distanceBetweenSquared;
    }

    public void setDistanceBetweenSquared(double distanceBetweenSquared) {
        this.distanceBetweenSquared = distanceBetweenSquared;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public int getLoops() {
        return loops;
    }

    public void setLoops(int loops) {
        this.loops = loops;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public Detonation getDetonation() {
        return detonation;
    }

    public void setDetonation(Detonation detonation) {
        this.detonation = detonation;
    }

    public void trigger(Location flareLocation, LivingEntity shooter, WeaponProjectile projectile) {
        if (mechanics != null) {
            CastData cast = new CastData(shooter, projectile.getWeaponTitle(), projectile.getWeaponStack());
            cast.setTargetLocation(flareLocation);
            mechanics.use(cast);
        }

        WeaponMechanics.getInstance().getFoliaScheduler().region(flareLocation).runAtFixedRate(new Consumer<>() {
            int count = 0;

            @Override
            public void accept(@NotNull TaskImplementation task) {
                int bombs = RandomUtil.range(min, max);
                int checks = bombs * bombs;

                // Used to make sure we don't spawn bombs too close to
                // each other. Uses distanceBetweenSquared
                List<Vector2d> spawnLocations = new ArrayList<>(bombs);

                locationFinder : for (int i = 0; i < checks && spawnLocations.size() < bombs; i++) {

                    double x = flareLocation.getX() + RandomUtil.range(-radius, radius);
                    double z = flareLocation.getZ() + RandomUtil.range(-radius, radius);

                    Vector2d vector = new Vector2d(x, z);

                    for (Vector2d spawnLocation : spawnLocations) {
                        if (vector.distanceSquared(spawnLocation) < distanceBetweenSquared) {
                            continue locationFinder;
                        }
                    }

                    spawnLocations.add(vector);

                    double y = flareLocation.getY() + height + RandomUtil.range(-yVariation, yVariation);
                    Location location = new Location(flareLocation.getWorld(), x, y, z);

                    // Either use the projectile settings from the "parent" projectile,
                    // or use the projectile settings for this airstrike
                    Projectile projectileHandler = getProjectile() != null ? getProjectile() : WeaponMechanics.getInstance().getWeaponConfigurations().getObject(projectile.getWeaponTitle() + ".Projectile", Projectile.class);
                    if (projectileHandler != null) {
                        WeaponProjectile newProjectile = getProjectile() != null
                            ? projectileHandler.create(shooter, location, new Vector(0, 0, 0), projectile.getWeaponStack(), projectile.getWeaponTitle(), projectile.getHand())
                            : projectile.clone(location, new Vector(0, 0, 0));
                        newProjectile.setIntTag("airstrike-bomb", 1);
                        projectileHandler.shoot(newProjectile, location);
                    }
                }

                if (++count >= loops) {
                    task.cancel();
                }
            }
        }, 1L, delay);
    }

    @Override
    @NotNull public AirStrike serialize(@NotNull SerializeData data) throws SerializerException {

        int min = data.of("Minimum_Bombs").assertExists().assertRange(1, null).getInt().getAsInt();
        int max = data.of("Maximum_Bombs").assertExists().assertRange(min, null).getInt().orElse(min);

        Projectile projectile = data.of("Dropped_Projectile").assertExists().serialize(Projectile.class).get();

        double yOffset = data.of("Height").assertRange(0.0, null).getDouble().orElse(60.0);
        double yNoise = data.of("Vertical_Randomness").assertRange(0.0, null).getDouble().orElse(5.0);

        double separation = data.of("Distance_Between_Bombs").assertRange(0.0, null).getDouble().orElse(3.0);
        double range = data.of("Maximum_Distance_From_Center").assertRange(0.0, null).getDouble().orElse(25.0);

        int layers = data.of("Layers").assertRange(1, null).getInt().orElse(1);
        int interval = data.of("Delay_Between_Layers").assertRange(1, null).getInt().orElse(40);

        Detonation detonation = data.of("Detonation").serialize(Detonation.class).orElse(null);
        MechanicManager mechanics = data.of("Mechanics").serialize(MechanicManager.class).orElse(null);

        return new AirStrike(projectile, min, max, yOffset, yNoise, separation, range, layers, interval, detonation, mechanics);
    }

    record Vector2d(double x, double z) {
        double distanceSquared(@NotNull Vector2d vector) {
            return NumberConversions.square(this.x - vector.x) + NumberConversions.square(this.z - vector.z);
        }
    }
}
