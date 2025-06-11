package me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile;

import me.deecaad.core.compatibility.HitBox;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.ray.BlockTraceResult;
import me.deecaad.core.utils.ray.EntityTraceResult;
import me.deecaad.core.utils.ray.RayTraceResult;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class Bouncy implements Serializer<Bouncy>, Cloneable {

    // -1 = infinite
    private int maximumBounceAmount;

    private ListHolder<BlockType> blocks;
    private ListHolder<EntityType> entities;

    private double requiredMotionToStartRollingOrDie;
    private ListHolder<BlockType> rollingBlocks;

    /**
     * Default constructor for serializer
     */
    public Bouncy() {
    }

    public Bouncy(int maximumBounceAmount, ListHolder<BlockType> blocks, ListHolder<EntityType> entities,
        double requiredMotionToStartRollingOrDie, ListHolder<BlockType> rollingBlocks) {
        this.maximumBounceAmount = maximumBounceAmount;
        this.blocks = blocks;
        this.entities = entities;
        this.requiredMotionToStartRollingOrDie = requiredMotionToStartRollingOrDie;
        this.rollingBlocks = rollingBlocks;
    }

    public int getMaximumBounceAmount() {
        return maximumBounceAmount;
    }

    public void setMaximumBounceAmount(int maximumBounceAmount) {
        this.maximumBounceAmount = maximumBounceAmount;
    }

    /**
     * If rolling isn't used, this value is used determine whether projectile can't bounce anymore. If
     * rolling is used, this value is used to determine when projectile starts rolling instead of
     * bouncing.
     *
     * @return the required motion to start rolling or die
     */
    public double getRequiredMotionToStartRollingOrDie() {
        return requiredMotionToStartRollingOrDie;
    }

    public void setRequiredMotionToStartRollingOrDie(double requiredMotionToStartRolling) {
        this.requiredMotionToStartRollingOrDie = requiredMotionToStartRolling;
    }

    /**
     * @param projectile the projectile
     * @param hit the hit entity or block
     * @return true if projectile bounced or started rolling, false if projectile should die
     */
    public boolean handleBounce(WeaponProjectile projectile, RayTraceResult hit) {
        Double speedModifier;
        if (hit instanceof BlockTraceResult blockHit) {
            speedModifier = blocks != null ? blocks.isValid(blockHit.getBlock().getType().asBlockType()) : null;
        } else if (hit instanceof EntityTraceResult entityHit) {
            speedModifier = entities != null ? entities.isValid(entityHit.getEntity().getType()) : null;
        } else {
            // should never occur, so projectile should die
            return false;
        }

        // Speed modifier null would mean that it wasn't valid material or entity type
        if (speedModifier == null || (maximumBounceAmount > 0 && maximumBounceAmount - projectile.getBounces() < 1)) {
            // Projectile should die
            return false;
        }

        Vector motion = projectile.getMotion();
        if (speedModifier != 1.0)
            motion.multiply(speedModifier);

        switch (hit.getHitFace()) {
            case UP, DOWN -> motion.setY(-motion.getY());
            case EAST, WEST -> motion.setX(-motion.getX());
            case NORTH, SOUTH -> motion.setZ(-motion.getZ());
            default -> {
            }
        }

        projectile.setMotion(motion);

        return true;
    }

    /**
     * @param projectile the projectile
     * @param block the block below
     * @return true if projectile kept rolling, false if projectile should die
     */
    public boolean handleRolling(WeaponProjectile projectile, Block block) {
        if (rollingBlocks == null)
            return false;

        Double speedModifier = rollingBlocks.isValid(block.getType().asBlockType());

        if (speedModifier == null) {
            // Projectile should die since block wasn't valid
            return false;
        }

        projectile.setRolling(true);

        Vector motion = projectile.getMotion();
        motion.multiply(speedModifier);

        // Remove vertical motion since projectile should start/keep rolling
        motion.setY(0);

        projectile.setMotion(motion);
        return true;
    }

    /**
     * @param projectile the projectile
     * @return true if projectile is unable to keep rolling AND should die
     */
    public boolean checkForRollingCancel(WeaponProjectile projectile) {
        Vector slightlyBelow = projectile.getLocation().add(new Vector(0, -0.05, 0));
        Block slightlyBelowBlock = projectile.getWorld().getBlockAt(slightlyBelow.getBlockX(), slightlyBelow.getBlockY(), slightlyBelow.getBlockZ());
        if (HitBox.getHitbox(slightlyBelowBlock, false) != null) {
            // Check speed modifier of block below and apply it
            if (!handleRolling(projectile, slightlyBelowBlock)) {
                // Block below wasn't valid rolling block, remove projectile
                return true;
            }
            if (projectile.getMotionLength() < 0.05) {
                // The motion is so slow at this point, simply apply sticked data to block below
                projectile.setStickedData(new StickedData(projectile, slightlyBelowBlock));
                projectile.setRolling(false);
            }
        } else {
            // Block below is air or passable block, toggle rolling off
            // When rolling is off, gravity is applied again
            projectile.setRolling(false);
        }
        return false;
    }

    @Override
    public String getKeyword() {
        return "Bouncy";
    }

    @Override
    @NotNull public Bouncy serialize(@NotNull SerializeData data) throws SerializerException {
        ListHolder<BlockType> blocks = data.of("Blocks").serialize(new ListHolder<>(BlockType.class)).orElse(null);
        ListHolder<EntityType> entities = data.of("Entities").serialize(new ListHolder<>(EntityType.class)).orElse(null);

        if (blocks == null && entities == null) {
            throw data.exception(null, "'Bouncy' requires at least one of 'Blocks' or 'Entities'");
        }

        // 0 or negative numbers will lead to infinite amounts of bouncing.
        int maximumBounceAmount = data.of("Maximum_Bounce_Amount").getInt().orElse(1);

        ListHolder<BlockType> rollingBlocks = data.of("Rolling.Blocks").serialize(new ListHolder<>(BlockType.class)).orElse(null);
        double requiredMotionToStartRolling = data.of("Rolling.Required_Motion_To_Start_Rolling").assertRange(0.0, null).getDouble().orElse(6.0) / 20;

        return new Bouncy(maximumBounceAmount, blocks, entities, requiredMotionToStartRolling, rollingBlocks);
    }

    @Override
    public Bouncy clone() {
        try {
            return (Bouncy) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}