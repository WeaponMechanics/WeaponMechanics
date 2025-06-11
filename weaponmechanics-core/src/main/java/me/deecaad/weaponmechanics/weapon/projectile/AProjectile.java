package me.deecaad.weaponmechanics.weapon.projectile;

import me.deecaad.core.compatibility.entity.FakeEntity;
import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.core.utils.ray.RayTraceResult;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AProjectile {

    // Used with disguises and cached on first run, defaults to 50 ticks
    private static int CHECK_FOR_NEW_PLAYER_RATE = 0;

    private final LivingEntity shooter;
    private final World world;

    private FakeEntity disguise;
    private int lastDisguiseUpdateTick;

    private Vector lastLocation;
    private Vector location;
    private Vector motion;
    private double motionLength;

    private int aliveTicks;
    private double distanceTravelled;
    private boolean dead;
    private Map<String, String> stringTags;
    private Map<String, Integer> integerTags;

    /**
     * @see ProjectileScriptManager
     * @see ProjectileScript
     */
    private final List<ProjectileScript<?>> scripts;

    protected AProjectile(Location location, Vector motion) {
        this(null, location, motion);
    }

    protected AProjectile(LivingEntity shooter, Location location, Vector motion) {
        this.shooter = shooter;
        this.world = location.getWorld();
        this.location = location.toVector();
        this.lastLocation = this.location.clone();
        this.motion = motion;
        this.motionLength = motion.length();
        this.scripts = new LinkedList<>(); // dynamic, O(1) resize
        onStart();
    }

    /**
     * Returns true if the calling thread is the ticking thread for this projectile.
     *
     * @return true if the calling thread is the ticking thread for this projectile
     */
    public boolean isOwnedByCurrentRegion() {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return WeaponMechanics.getInstance().getFoliaScheduler().isOwnedByCurrentRegion(world, chunkX, chunkZ);
    }

    /**
     * @return gravity of projectile
     */
    public double getGravity() {
        return 0.05;
    }

    /**
     * -1.0 means not used
     *
     * @return minimum speed of projectile
     */
    public double getMinimumSpeed() {
        return -1.0;
    }

    /**
     * @return whether to remove projectile when minimum speed is reached
     */
    public boolean isRemoveAtMinimumSpeed() {
        return false;
    }

    /**
     * -1.0 means not used
     *
     * @return maximum speed of projectile
     */
    public double getMaximumSpeed() {
        return -1.0;
    }

    /**
     * @return whether to remove projectile when maximum speed is reached
     */
    public boolean isRemoveAtMaximumSpeed() {
        return false;
    }

    /**
     * @return base speed decreasing
     */
    public double getDrag() {
        if (getCurrentBlock().isLiquid())
            return 0.96;
        else if (world.isThundering() || world.hasStorm())
            return 0.98;
        else
            return 0.99;
    }

    /**
     * @return the maximum amount of ticks projectile can be alive
     */
    public int getMaximumAliveTicks() {
        return 600;
    }

    /**
     * @return the disguise of projectile, or null if not used
     */
    @Nullable public FakeEntity getDisguise() {
        return disguise;
    }

    /**
     * @return the shooter of projectile
     */
    @Nullable public LivingEntity getShooter() {
        return shooter;
    }

    /**
     * @return the world where this projectile is in
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the clone of last location
     */
    public Vector getLastLocation() {
        return lastLocation.clone();
    }

    /**
     * @return the clone of current location
     */
    public Vector getLocation() {
        return location.clone();
    }

    /**
     * @return the current location as a bukkit location
     */
    public Location getBukkitLocation() {
        return new Location(world, location.getX(), location.getY(), location.getZ());
    }

    public Block getCurrentBlock() {
        return world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public double getX() {
        return location.getX();
    }

    public double getY() {
        return location.getY();
    }

    public double getZ() {
        return location.getZ();
    }

    /**
     * @param location the new location for projectile
     */
    public void setLocation(Vector location) {
        if (location == null)
            throw new IllegalArgumentException("Location can't be null");
        this.lastLocation = this.location.clone();
        this.location = location;
    }

    /**
     * Main difference compared to {@link #setLocation(Vector)} is that this doesn't update projectile
     * last location.
     *
     * @param location the new location for projectile
     */
    public void setRawLocation(Vector location) {
        if (location == null)
            throw new IllegalArgumentException("Location can't be null");
        this.location = location;
    }

    /**
     * @return the clone of current motion
     */
    public Vector getMotion() {
        return motion.clone();
    }

    /**
     * @return the current motion's length
     */
    public double getMotionLength() {
        return motionLength;
    }

    /**
     * @return the normalized current motion
     */
    public Vector getNormalizedMotion() {
        if (motionLength == 0)
            return getMotion();
        return getMotion().divide(new Vector(motionLength, motionLength, motionLength));
    }

    /**
     * Updates motion length at same time
     *
     * @param motion the new motion for projectile
     */
    public void setMotion(Vector motion) {
        if (motion == null)
            throw new IllegalArgumentException("Motion can't be null");
        this.motion = motion;
        this.motionLength = motion.length();
    }

    /**
     * @return the amount of ticks this projectile has been alive
     */
    public int getAliveTicks() {
        return aliveTicks;
    }

    /**
     * @return the distance projectile has travelled where 1.0 equals 1 block
     */
    public double getDistanceTravelled() {
        return distanceTravelled;
    }

    /**
     * @param amount adds this amount to distance travelled
     */
    public void addDistanceTravelled(double amount) {
        distanceTravelled += amount;
    }

    /**
     * Used to fetch any temporary data from projectiles
     *
     * @param key the key to fetch
     * @return the value of key or null if not found
     */
    public String getTag(String key) {
        return stringTags == null || stringTags.isEmpty() ? null : this.stringTags.get(key);
    }

    /**
     * This can store temporary data for projectiles
     *
     * @param key the key to use
     * @param value the value for key
     */
    public void setTag(String key, String value) {
        if (key == null)
            throw new IllegalArgumentException("Key can't be null");
        if (value == null)
            throw new IllegalArgumentException("Value can't be null");
        if (this.stringTags == null) {
            this.stringTags = new HashMap<>();
        }
        this.stringTags.put(key, value);
    }

    /**
     * Used to fetch any temporary data from projectiles
     *
     * @param key the key to fetch
     * @return the value of key or 0 if not found
     */
    public int getIntTag(String key) {
        return integerTags == null || integerTags.isEmpty() ? 0 : this.integerTags.getOrDefault(key, 0);
    }

    /**
     * This can store temporary data for projectiles
     *
     * @param key the key to use
     * @param value the value for key
     */
    public void setIntTag(String key, int value) {
        if (key == null)
            throw new IllegalArgumentException("Key can't be null");
        if (this.integerTags == null) {
            this.integerTags = new HashMap<>();
        }
        this.integerTags.put(key, value);
    }

    /**
     * @return true if projectile is marked for removal or is already dead
     */
    public boolean isDead() {
        return dead;
    }

    public void addProjectileScript(ProjectileScript<?> script) {
        scripts.add(script);
    }

    /**
     * Marks projectile for removal and will be removed on next tick or in this tick if this method is
     * called within the tick method.
     */
    public void remove() {
        // Don't allow anything to remove this twice
        if (this.dead)
            return;

        this.dead = true;

        updateDisguise(true);

        onEnd();
        if (disguise != null)
            disguise.remove();

        scriptEvent(ProjectileScript::onTickEnd);
    }

    /**
     * Projectile base tick
     *
     * @return true if projectile should be removed from projectile runnable
     */
    public boolean tick() {
        if (this.dead) {
            // If projectile is marked for removal, but hasn't yet been removed
            return true;
        }

        scriptEvent(ProjectileScript::onTickStart);

        // Update last location here since handle collisions will change the location
        lastLocation = location.clone();

        // Update motion BEFORE updating position, see #339
        double gravity = getGravity();
        if (gravity != 0) {
            motion.setY(motion.getY() - gravity);
        }
        motion.multiply(getDrag());

        // Handle collisions will update location and distance travelled
        if (updatePosition()) {
            return true;
        }

        double locationY = location.getY();
        int minWorldHeight = MinecraftVersions.NETHER_UPDATE.isAtLeast() ? world.getMinHeight() : -32;
        if (aliveTicks >= getMaximumAliveTicks() || locationY < minWorldHeight || locationY > world.getMaxHeight()) {
            return true;
        }

        if (gravity == 0 && motionLength < Vector.getEpsilon()) {

            // No need to continue as motion is empty and there isn't gravity currently

            if (motionLength != 0)
                motionLength = 0;

            updateDisguise(true);
            scriptEvent(ProjectileScript::onTickEnd);
            ++aliveTicks;
            return false;
        }

        motionLength = motion.length();

        double minimumSpeed = getMinimumSpeed();
        double maximumSpeed = getMaximumSpeed();
        if (minimumSpeed != -1.0 && motionLength < minimumSpeed) {
            if (isRemoveAtMinimumSpeed()) {
                return true;
            }
            setMotion(getNormalizedMotion().multiply(getMinimumSpeed()));
        } else if (maximumSpeed != -1.0 && motionLength > maximumSpeed) {
            if (isRemoveAtMaximumSpeed()) {
                return true;
            }
            setMotion(getNormalizedMotion().multiply(getMaximumSpeed()));
        }

        updateDisguise(false);
        scriptEvent(ProjectileScript::onTickEnd);
        ++aliveTicks;

        return false;
    }

    /**
     * If this projectile already has disguise spawned, this call is ignored
     *
     * @param disguise the disguise to spawn
     */
    public void spawnDisguise(FakeEntity disguise) {
        if (disguise == null || this.disguise != null)
            return;

        // Cache this rate
        if (CHECK_FOR_NEW_PLAYER_RATE == 0)
            CHECK_FOR_NEW_PLAYER_RATE = WeaponMechanics.getInstance().getConfiguration().getInt("Check_For_New_Player_Rate", 50);

        this.disguise = disguise;

        if (getGravity() == 0.0)
            this.disguise.setGravity(false);

        this.disguise.show();
        this.disguise.setMotion(motion);
    }

    /**
     * This method can't be used multiple times on same tick
     *
     * @param forceTeleport true to force teleport packet
     */
    protected void updateDisguise(boolean forceTeleport) {
        if (disguise == null || lastDisguiseUpdateTick == aliveTicks)
            return;

        // Show for new players in range
        if (aliveTicks % CHECK_FOR_NEW_PLAYER_RATE == 0)
            disguise.show();

        if (motionLength == 0) {
            disguise.setPosition(location.getX(), location.getY(), location.getZ(), disguise.getYaw(), disguise.getPitch(), forceTeleport);
        } else {
            Vector normalizedMotion = getNormalizedMotion();
            disguise.setPosition(location.getX(), location.getY(), location.getZ(), calculateYaw(normalizedMotion), calculatePitch(normalizedMotion), forceTeleport);
        }
        disguise.setMotion(motion);

        lastDisguiseUpdateTick = aliveTicks;
    }

    private float calculateYaw(Vector normalizedMotion) {
        if (motionLength == 0)
            return 0;
        double PI_2 = NumberUtil.TAU_DOUBLE;
        return (float) Math.toDegrees((Math.atan2(-normalizedMotion.getX(), normalizedMotion.getZ()) + PI_2) % PI_2);
    }

    private float calculatePitch(Vector normalizedMotion) {
        if (motionLength == 0)
            return 0;
        return (float) Math.toDegrees(Math.atan(-normalizedMotion.getY() / Math.sqrt(NumberConversions.square(normalizedMotion.getX()) + NumberConversions.square(normalizedMotion.getZ()))));
    }

    /**
     * This method updates the projectile's position/velocity, and handles all physics interactions
     * during that movement. This method also updates {@link #distanceTravelled}.
     *
     * @return true if projectile should be removed from projectile runnable
     */
    public abstract boolean updatePosition();

    /**
     * Override this method to do something on start
     */
    public void onStart() {
        scriptEvent(ProjectileScript::onStart);
    }

    /**
     * Override this method to do something on end
     */
    public void onEnd() {
        scriptEvent(ProjectileScript::onEnd);
    }

    /**
     * Override this method to do something when projectile collides with block or living entity.
     *
     * @param hit the collided block or living entity
     */
    public void onCollide(RayTraceResult hit) {
        scriptEvent(proj -> proj.onCollide(hit));
    }

    protected void scriptEvent(Consumer<ProjectileScript<?>> consumer) {
        Iterator<ProjectileScript<?>> iterator = scripts.iterator();
        boolean removeProjectile = false;

        while (iterator.hasNext()) {
            ProjectileScript<?> script = iterator.next();

            // Remove the script if the script has requested to be removed.
            if (script.isRemoveScript()) {
                iterator.remove();
                continue;
            }

            consumer.accept(script);
            removeProjectile |= script.isRemoveProjectile();
        }

        if (removeProjectile)
            remove();
    }
}