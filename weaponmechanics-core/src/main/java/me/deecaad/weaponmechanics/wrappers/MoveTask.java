package me.deecaad.weaponmechanics.wrappers;

import com.cjcrafter.foliascheduler.TaskImplementation;
import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import me.deecaad.core.compatibility.HitBox;
import me.deecaad.core.file.Configuration;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.events.PlayerJumpEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponStopShootingEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MoveTask implements Consumer<TaskImplementation<Void>> {

    private final EntityWrapper entityWrapper;
    private Location from;
    private int sameMatches;
    private int jumps;
    private int groundTicks;

    public MoveTask(EntityWrapper entityWrapper) {
        this.entityWrapper = entityWrapper;
        LivingEntity entity = entityWrapper.getEntity();
        this.from = entity.getLocation();
        if (entity instanceof Player) {
            this.jumps = ((Player) entity).getStatistic(Statistic.JUMP);
        } else {
            this.jumps = -1;
        }
    }

    @Override
    public void accept(@NotNull TaskImplementation task) {
        LivingEntity entity = entityWrapper.getEntity();
        if (entity == null || !entity.isValid() || entity.isDead()) { // Just an extra check in case something odd happened

            // Only cancel task IF it isn't player, otherwise just don't do anything
            if (!entityWrapper.isPlayer())
                task.cancel();

            return;
        }

        handleStopShooting(entityWrapper.getHandData(true));
        handleStopShooting(entityWrapper.getHandData(false));

        Location from = this.from;
        Location to = entity.getLocation();

        this.from = to;

        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        if (!config.getBoolean("Disabled_Trigger_Checks.Swim")) {
            if (isSwimming(entity)) {
                entityWrapper.setSwimming(true);

                // -> Can't be walking, standing, in mid air at same time
                return;
            } else {
                entityWrapper.setSwimming(false);
            }
        }

        boolean inMidairCheck = isInMidair(entity);

        if (!config.getBoolean("Disabled_Trigger_Checks.Standing_And_Walking")) {
            if (isSameLocationNonRotation(from, to)) {
                ++this.sameMatches;
            } else {
                this.sameMatches = 0;
            }
            if (this.sameMatches > 3) {
                entityWrapper.setStanding(true);

                // -> Can't be walking, swimming, in mid air at same time
                // Swimming is already returned above if it was true
                return;
            } else if (!inMidairCheck) {

                // Only walking if not in mid air
                entityWrapper.setWalking(true);
            }
        }

        // Needed for double jump
        if (inMidairCheck) {
            groundTicks = 0;
        } else {
            ++groundTicks;
        }

        if (!config.getBoolean("Disabled_Trigger_Checks.In_Midair")) {
            entityWrapper.setInMidair(inMidairCheck);
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        if (this.jumps != -1) {
            if (!config.getBoolean("Disabled_Trigger_Checks.Jump")) {
                if (from.getY() < to.getY() && !player.getLocation().getBlock().isLiquid()) {
                    int currentJumps = player.getStatistic(Statistic.JUMP);
                    int jumpsLast = this.jumps;
                    if (currentJumps != jumpsLast) {
                        this.jumps = currentJumps;
                        double yChange = to.getY() - from.getY();
                        if ((yChange < 0.035 || yChange > 0.037) && (yChange < 0.116 || yChange > 0.118)) {
                            Bukkit.getPluginManager().callEvent(new PlayerJumpEvent(player, false));
                        }
                    }
                }
            }
        }

        if (!config.getBoolean("Disabled_Trigger_Checks.Double_Jump")
            && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
            if (player.getFallDistance() > 3.0) {
                // https://minecraft.gamepedia.com/Damage#Fall_damage
                // Fall damage is 1♥ for each block of fall distance after the third. Thus, falling 4 blocks causes
                // 1♥ damage, 2♥ damage for 5 blocks, and so forth.

                // This enables fall damage for player. Double jump has to be made BEFORE falling more than 3 blocks

                if (player.getAllowFlight()) {
                    player.setAllowFlight(false);
                }
            } else if (!player.getAllowFlight() && this.groundTicks > 3) {
                // Only give double jump ability if been on ground for at least 3 ticks
                player.setAllowFlight(true);
            }
        }
    }

    private boolean isSameLocationNonRotation(Location location1, Location location2) {
        if (Double.doubleToLongBits(location1.getX()) != Double.doubleToLongBits(location2.getX())) {
            return false;
        }
        if (Double.doubleToLongBits(location1.getY()) != Double.doubleToLongBits(location2.getY())) {
            return false;
        }
        return Double.doubleToLongBits(location1.getZ()) == Double.doubleToLongBits(location2.getZ());
    }

    /**
     * Basically checks if entity is in mid air. Mid air is determined on if current block in player's
     * position doesn't have hit box and block below that doesn't have hit box either
     */
    private boolean isInMidair(LivingEntity livingEntity) {
        Block current = livingEntity.getLocation().getBlock();
        Block below = current.getRelative(BlockFace.DOWN);

        // Check for liquid as hit boxes are considered null if block is liquid
        if (current.isLiquid() || below.isLiquid())
            return false;

        HitBox belowHitBox = HitBox.getHitbox(below, false);
        HitBox currentHitBox = HitBox.getHitbox(current, false);
        return belowHitBox == null && currentHitBox == null;
    }

    private void handleStopShooting(HandData handData) {
        // Already fired the event
        if (handData.isFiredWeaponStopShootEvent())
            return;
        // Haven't shot the gun
        if (handData.getLastWeaponShotTitle() == null || handData.getLastWeaponShot() == null)
            return;
        // Hasn't been 1 second since they stopped shooting
        if (System.currentTimeMillis() - handData.getLastShotTime() <= 1200L)
            return;

        handData.setFiredWeaponStopShootEvent(true);
        WeaponStopShootingEvent event = new WeaponStopShootingEvent(handData.getLastWeaponShotTitle(), handData.getLastWeaponShot(), entityWrapper.getEntity(), handData.isMainhand()
            ? EquipmentSlot.HAND
            : EquipmentSlot.OFF_HAND, handData.getLastShotTime());
        Bukkit.getPluginManager().callEvent(event);
    }

    private static boolean isSwimming(LivingEntity livingEntity) {
        if (livingEntity.isInsideVehicle())
            return false;

        // 1.13 introduced block data for blocks like stairs and slabs, and can
        // be waterlogged. 1.13 also introduced the swimming mechanic.
        if (MinecraftVersions.UPDATE_AQUATIC.isAtLeast()) {
            if (livingEntity.isSwimming())
                return true;

            // We only care about the block the entity's head is in, since you
            // (mostly) fire a gun from the shoulder/upper body.
            Block block = livingEntity.getEyeLocation().getBlock();
            BlockData data = block.getBlockData();
            return block.isLiquid() || (data instanceof Waterlogged && ((Waterlogged) data).isWaterlogged());
        }

        // On versions below 1.13
        return livingEntity.getEyeLocation().getBlock().isLiquid();
    }
}