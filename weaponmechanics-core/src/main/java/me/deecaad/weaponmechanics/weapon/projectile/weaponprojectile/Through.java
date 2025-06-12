package me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.ray.BlockTraceResult;
import me.deecaad.core.utils.ray.EntityTraceResult;
import me.deecaad.core.utils.ray.RayTraceResult;
import org.bukkit.block.BlockType;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class Through implements Serializer<Through>, Cloneable {

    // -1 = infinite
    private double maximumThroughAmount;

    private ListHolder<BlockType> blocks;
    private ListHolder<EntityType> entities;

    /**
     * Default constructor for serializer
     */
    public Through() {
    }

    public Through(double maximumThroughAmount, ListHolder<BlockType> blocks, ListHolder<EntityType> entities) {
        this.maximumThroughAmount = maximumThroughAmount;
        this.blocks = blocks;
        this.entities = entities;
    }

    public double getMaximumThroughAmount() {
        return maximumThroughAmount;
    }

    public void setMaximumThroughAmount(double maximumThroughAmount) {
        this.maximumThroughAmount = maximumThroughAmount;
    }

    /**
     * @param projectile the projectile
     * @param hit the hit entity or block
     * @return true if projectile went through, false if projectile should die
     */
    public boolean handleThrough(WeaponProjectile projectile, RayTraceResult hit) {

        Double speedModifier;
        if (hit instanceof BlockTraceResult blockHit) {
            speedModifier = blocks != null ? blocks.isValid(blockHit.getBlockState().getType().asBlockType()) : null;
        } else if (hit instanceof EntityTraceResult entityHit) {
            speedModifier = entities != null ? entities.isValid(entityHit.getEntity().getType()) : null;
        } else {
            // should never occur, projectile should die
            return false;
        }

        // Speed modifier null would mean that it wasn't valid material or entity type
        if (speedModifier == null)
            return false;

        if (maximumThroughAmount != -1 && maximumThroughAmount - projectile.getThroughAmount() - hit.getThroughDistance() <= 0.0) {
            // Projectile should die
            return false;
        }

        if (speedModifier != 1.0)
            projectile.setMotion(projectile.getMotion().multiply(speedModifier));

        return true;
    }

    @Override
    public String getKeyword() {
        return "Through";
    }

    @Override
    @NotNull public Through serialize(@NotNull SerializeData data) throws SerializerException {
        ListHolder<BlockType> blocks = data.of("Blocks").serialize(new ListHolder<>(BlockType.class)).orElse(null);
        ListHolder<EntityType> entities = data.of("Entities").serialize(new ListHolder<>(EntityType.class)).orElse(null);

        if (blocks == null && entities == null) {
            throw data.exception(null, "'Through' requires at least one of 'Blocks' or 'Entities'");
        }

        double maximumThroughAmount = data.of("Maximum_Through_Amount").getDouble().orElse(-1.0);

        return new Through(maximumThroughAmount, blocks, entities);
    }

    @Override
    public Through clone() {
        try {
            return (Through) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
