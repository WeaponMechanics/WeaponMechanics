package me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile;

import me.deecaad.core.utils.ray.BlockTraceResult;
import me.deecaad.core.utils.ray.EntityTraceResult;
import me.deecaad.core.utils.ray.RayTrace;
import me.deecaad.core.utils.ray.RayTraceResult;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.projectile.AProjectile;
import me.deecaad.weaponmechanics.weapon.weaponevents.ProjectileEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeaponProjectile extends AProjectile {

    // These may be modified by WMP. We have booleans to check if a new copy
    // was made of these variables.
    private ProjectileSettings projectileSettings;
    private boolean isProjectileSettingsChanged;
    private Sticky sticky;
    private boolean isStickyChanged;
    private Through through;
    private boolean isThroughChanged;
    private Bouncy bouncy;
    private boolean isBouncyChanged;

    private final ItemStack weaponStack;
    private final String weaponTitle;
    private final EquipmentSlot hand;

    private StickedData stickedData;
    private double throughAmount;
    private int bounces;
    private boolean rolling;

    // These are for through and bouncy to deny collision with
    // same block or entity right after colliding with it
    private int lastBlockUpdateTick;
    private Location lastBlock;
    private int lastEntityUpdateTick;
    private int lastEntity = -1;

    private final RayTrace rayTrace;

    public WeaponProjectile(ProjectileSettings projectileSettings, LivingEntity shooter, Location location,
        Vector motion, ItemStack weaponStack, String weaponTitle, EquipmentSlot hand,
        Sticky sticky, Through through, Bouncy bouncy) {
        super(shooter, location, motion);

        this.projectileSettings = projectileSettings;
        this.sticky = sticky;
        this.through = through;
        this.bouncy = bouncy;

        this.weaponStack = weaponStack;
        this.weaponTitle = weaponTitle;
        this.hand = hand;

        if (projectileSettings.isDisableEntityCollisions()) {
            this.rayTrace = new RayTrace()
                .withBlockFilter(this::equalToLastHit)
                .disableEntityChecks()
                .enableLiquidChecks()
                .withRaySize(projectileSettings.getSize());
        } else {
            this.rayTrace = new RayTrace()
                .withBlockFilter(this::equalToLastHit)
                .withEntityFilter(entity -> equalToLastHit(entity)
                    || (getShooter() != null && getAliveTicks() < 10 && entity.getEntityId() == getShooter().getEntityId())
                    || entity.getPassengers().contains(getShooter()))
                .enableLiquidChecks()
                .withRaySize(projectileSettings.getSize());
        }
    }

    /**
     * Clones the settings of this weapon projectile without shooting it
     *
     * @param location the cloned projectile's new start location
     * @param motion the cloned projectile's new motion
     * @return the cloned projectile
     */
    public WeaponProjectile clone(Location location, Vector motion) {
        return new WeaponProjectile(projectileSettings, getShooter(), location, motion, weaponStack, weaponTitle, hand, sticky, through, bouncy);
    }

    public ProjectileSettings getProjectileSettings() {
        if (isProjectileSettingsChanged)
            return projectileSettings;

        projectileSettings = projectileSettings.clone();
        isProjectileSettingsChanged = true;
        return projectileSettings;
    }

    public void setProjectileSettings(@NotNull ProjectileSettings projectileSettings) {
        this.projectileSettings = projectileSettings;
        isProjectileSettingsChanged = true;
    }

    public @Nullable Sticky getSticky() {
        if (isStickyChanged || sticky == null)
            return sticky;

        sticky = sticky.clone();
        isStickyChanged = true;
        return sticky;
    }

    public void setSticky(@Nullable Sticky sticky) {
        setSticky(sticky, true);
    }

    /**
     * Sets the sticky properties of this projectile (whether it sticks to blocks/entities). If
     * <code>isStickyChanged == true</code>, then no copy will be made. This means that the passed
     * <code>sticky</code> instance must be mutable.
     *
     * @param sticky The nullable sticky instance.
     * @param isStickyChanged true if sticky is mutable.
     */
    public void setSticky(@Nullable Sticky sticky, boolean isStickyChanged) {
        this.sticky = sticky;
        this.isStickyChanged = isStickyChanged;
    }

    public @Nullable Through getThrough() {
        if (isThroughChanged || through == null)
            return through;

        through = through.clone();
        isThroughChanged = true;
        return through;
    }

    public void setThrough(@Nullable Through through) {
        setThrough(through, true);
    }

    /**
     * Sets the through properties of this projectile (whether it passes through blocks/entities). If
     * <code>isThroughChanged == true</code>, then no copy will be made. This means that the passed
     * <code>through</code> instance must be mutable.
     *
     * @param through The nullable through instance.
     * @param isThroughChanged true if through is mutable.
     */
    public void setThrough(@Nullable Through through, boolean isThroughChanged) {
        this.through = through;
        this.isThroughChanged = isThroughChanged;
    }

    public @Nullable Bouncy getBouncy() {
        if (isBouncyChanged || bouncy == null)
            return bouncy;

        bouncy = bouncy.clone();
        isBouncyChanged = true;
        return bouncy;
    }

    public void setBouncy(@Nullable Bouncy bouncy) {
        setBouncy(bouncy, true);
    }

    /**
     * Sets the bouncy properties of this projectile (whether it bounces on blocks/entities). If
     * <code>isBouncyChanged == true</code>, then no copy will be made. This means that the passed
     * <code>through</code> instance must be mutable.
     *
     * @param bouncy The nullable bouncy instance.
     * @param isBouncyChanged true if bouncy is mutable.
     */
    public void setBouncy(@Nullable Bouncy bouncy, boolean isBouncyChanged) {
        this.bouncy = bouncy;
        this.isBouncyChanged = isBouncyChanged;
    }

    @Override
    public double getGravity() {
        return rolling || stickedData != null ? 0 : projectileSettings.getGravity();
    }

    @Override
    public double getMinimumSpeed() {
        return projectileSettings.getMinimumSpeed();
    }

    @Override
    public boolean isRemoveAtMinimumSpeed() {
        return projectileSettings.isRemoveAtMinimumSpeed();
    }

    @Override
    public double getMaximumSpeed() {
        return projectileSettings.getMaximumSpeed();
    }

    @Override
    public boolean isRemoveAtMaximumSpeed() {
        return projectileSettings.isRemoveAtMaximumSpeed();
    }

    @Override
    public double getDrag() {
        if (getCurrentBlock().isLiquid())
            return projectileSettings.getDecreaseInWater();
        else if (getWorld().isThundering() || getWorld().hasStorm())
            return projectileSettings.getDecreaseWhenRainingOrSnowing();
        else
            return projectileSettings.getDecrease();
    }

    @Override
    public int getMaximumAliveTicks() {
        return projectileSettings.getMaximumAliveTicks();
    }

    public boolean hasTravelledMaximumDistance() {
        double maximum = projectileSettings.getMaximumTravelDistance();
        return maximum != -1 && getDistanceTravelled() >= maximum;
    }

    /**
     * Can be null if for example API is used to shoot this projectile.
     *
     * @return the item stack used to shoot this projectile
     */
    @Nullable public ItemStack getWeaponStack() {
        return weaponStack;
    }

    /**
     * @return the weapon title used to shoot this projectile
     */
    public String getWeaponTitle() {
        return weaponTitle;
    }

    /**
     * Can be null through API
     *
     * @return the hand used to shoot this projectile.
     */
    public EquipmentSlot getHand() {
        return hand;
    }

    /**
     * @return the sticked data if this projectile is sticked to some entity or block
     */
    public StickedData getStickedData() {
        return stickedData;
    }

    /**
     * Set new sticked data. If new sticked data is null, sticked data is removed
     *
     * @param stickedData the new sticked data
     */
    public void setStickedData(StickedData stickedData) {
        if (stickedData == null) {
            this.stickedData = null;
            // This basically removes sticky
            setMotion(new Vector(
                ThreadLocalRandom.current().nextFloat() * 0.2,
                ThreadLocalRandom.current().nextFloat() * 0.2,
                ThreadLocalRandom.current().nextFloat() * 0.2));
            return;
        }

        // Just extra check if entity happens to die or block disappear
        if (stickedData.isBlockStick()) {
            if (stickedData.getBlock() == null) {
                return;
            }
        } else if (stickedData.getLivingEntity() == null) {
            return;
        }

        // Now we can safely set the sticked data and other required values

        this.stickedData = stickedData;
        setLocation(stickedData.getNewLocation());
        setMotion(new Vector(0, 0, 0));
    }

    /**
     * @return the amount of hit boxes this projectile has gone through using through feature
     */
    public double getThroughAmount() {
        return throughAmount;
    }

    /**
     * @return the amount of hit boxes this projectile has bounced off using bouncy feature
     */
    public int getBounces() {
        return bounces;
    }

    /**
     * Setting rolling to true disables projectile gravity
     *
     * @param rolling the new rolling state
     */
    public void setRolling(boolean rolling) {
        if (bouncy == null)
            return;

        this.rolling = rolling;
    }

    /**
     * @return whether projectile is currently rolling
     */
    public boolean isRolling() {
        return rolling;
    }

    @Override
    public boolean updatePosition() {

        Vector possibleNextLocation = getLocation().add(getMotion());
        if (!getWorld().isChunkLoaded(possibleNextLocation.getBlockX() >> 4, possibleNextLocation.getBlockZ() >> 4)) {
            // Remove projectile if next location would be in unloaded chunk
            return true;
        }

        if (stickedData != null) {
            Vector newLocation = stickedData.getNewLocation();
            if (newLocation == null) {
                // If this happens, either entity is dead or block isn't there anymore
                setStickedData(null);
            } else if (!stickedData.isBlockStick()) {
                // Update location and update distance travelled if living entity
                setRawLocation(newLocation);
                addDistanceTravelled(getLastLocation().distance(newLocation));
                return hasTravelledMaximumDistance();
            }
            return false;
        }

        // Don't check for new collisions if motion is empty
        if (getMotionLength() < Vector.getEpsilon())
            return false;

        // Returns sorted list of hits

        List<RayTraceResult> hits = rayTrace.cast(getWorld(), getLocation(), possibleNextLocation, getNormalizedMotion(),
            through == null ? 0.0 : through.getMaximumThroughAmount());
        if (hits == null) {

            // Check if can't keep rolling
            if (isRolling() && bouncy.checkForRollingCancel(this))
                return true;

            // No hits, simply update location and distance travelled
            setRawLocation(possibleNextLocation);
            addDistanceTravelled(getMotionLength());

            return hasTravelledMaximumDistance();
        }

        double distanceAlreadyAdded = 0;

        for (RayTraceResult hit : hits) {

            // Stay on track of current location and distance travelled on each loop
            setRawLocation(hit.getHitLocation());
            double add = hit.getHitMinClamped() - distanceAlreadyAdded;
            addDistanceTravelled(distanceAlreadyAdded += add);

            if (hasTravelledMaximumDistance()) {
                // Kill projectile since it can't go this far
                return true;
            }

            onCollide(hit);

            // We only want to let onCollide to be called onLiquid hits
            if (hit instanceof BlockTraceResult blockHit && blockHit.getBlock().isLiquid()) {
                continue;
            }

            // Returned true and that most likely means that block hit was cancelled, skipping...
            if (WeaponMechanics.getInstance().getWeaponHandler().getHitHandler().handleHit(hit, this))
                continue;

            // Through
            if (through != null && through.handleThrough(this, hit)) {
                // Continue since projectile went through.
                // We still need to check that other collisions also allows this
                throughAmount += hit.getThroughDistance();

                // Update last hit entity / block
                updateLastHit(hit);
                continue;
            }

            // Sticky
            if (sticky != null && sticky.handleSticking(this, hit)) {
                // Break since projectile sticked to entity or block
                return false;
            }

            // Bouncy and rolling
            if (bouncy != null) {

                // We want to check that projectile isn't already rolling
                // If it is already rolling we want to allow bouncing against hits
                if (!isRolling() && getMotionLength() < bouncy.getRequiredMotionToStartRollingOrDie()) {

                    // Returns true if projectile should die, false otherwise
                    return !(hit instanceof BlockTraceResult blockHit) || !bouncy.handleRolling(this, blockHit.getBlock());
                } else if (bouncy.handleBounce(this, hit)) {
                    // Break since projectile bounced to different direction
                    ++bounces;

                    // Update last hit entity / block
                    updateLastHit(hit);
                    return false;
                }
            }

            // Projectile should die if code reaches this point
            return true;
        }

        // Here we know that projectile didn't die on any collision.
        // We still have to update the location to last possible location.
        setRawLocation(possibleNextLocation);
        addDistanceTravelled(getMotionLength() - distanceAlreadyAdded);

        return hasTravelledMaximumDistance();
    }

    private void updateLastHit(RayTraceResult hit) {
        // Logic of +1 for last update tick:

        // Current alive tick is 5 in this case
        // lastXUpdateTick = 5 + 1

        // getAliveTicks <= lastXUpdateTick

        // getAliveTicks = 5 // CHECKS DURING CURRENT TICK
        // 5 <= 6 = TRUE
        // -> equalToLastHit can be true

        // getAliveTicks = 6 // CHECKS TICK AFTER HIT
        // 6 <= 6 = TRUE
        // -> equalToLastHit can be true

        // getAliveTicks = 7 // CHECKS 2 TICKS AFTER HIT
        // 7 <= 6 = FALSE
        // -> equalToLastHit is false even if the hit entity / block is same

        if (hit instanceof BlockTraceResult blockHit) {
            lastBlock = blockHit.getBlock().getLocation();
            lastBlockUpdateTick = getAliveTicks() + 1;
        } else if (hit instanceof EntityTraceResult entityHit) {
            lastEntity = entityHit.getEntity().getEntityId();
            lastEntityUpdateTick = getAliveTicks() + 1;
        }
    }

    private boolean equalToLastHit(Block hit) {
        Location hitBlock = hit.getLocation();
        return lastBlock != null && lastBlock.getBlockX() == hitBlock.getBlockX() && lastBlock.getBlockY() == hitBlock.getBlockY() && lastBlock.getBlockZ() == hitBlock.getBlockZ() // Check block
            && getAliveTicks() <= lastBlockUpdateTick; // Check hit tick
    }

    private boolean equalToLastHit(LivingEntity entity) {
        return lastEntity != -1 && lastEntity == entity.getEntityId() // Check entity
            && getAliveTicks() <= lastEntityUpdateTick; // Check hit tick
    }

    @Override
    public void onEnd() {
        super.onEnd();
        Bukkit.getPluginManager().callEvent(new ProjectileEndEvent(this));
    }
}