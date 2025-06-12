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

public class Sticky implements Serializer<Sticky>, Cloneable {

    private ListHolder<BlockType> blocks;
    private ListHolder<EntityType> entities;

    /**
     * Default constructor for serializer
     */
    public Sticky() {
    }

    public Sticky(ListHolder<BlockType> blocks, ListHolder<EntityType> entities) {
        this.blocks = blocks;
        this.entities = entities;
    }

    public boolean handleSticking(WeaponProjectile projectile, RayTraceResult hit) {
        Double isValid;
        if (hit instanceof BlockTraceResult blockHit) {
            isValid = blocks != null ? blocks.isValid(blockHit.getBlock().getType().asBlockType()) : null;
        } else if (hit instanceof EntityTraceResult entityHit) {
            isValid = entities != null ? entities.isValid(entityHit.getEntity().getType()) : null;
        } else {
            // Should never occur, projectile should die
            return false;
        }

        // Null means that it wasn't valid material or entity type
        if (isValid == null) {
            // Projectile should die
            return false;
        }

        projectile.setStickedData(new StickedData(hit));
        return true;
    }

    @Override
    public String getKeyword() {
        return "Sticky";
    }

    @Override
    @NotNull public Sticky serialize(@NotNull SerializeData data) throws SerializerException {
        ListHolder<BlockType> blocks = data.of("Blocks").serialize(new ListHolder<>(BlockType.class)).orElse(null);
        ListHolder<EntityType> entities = data.of("Entities").serialize(new ListHolder<>(EntityType.class)).orElse(null);

        if (blocks == null && entities == null) {
            throw data.exception(null, "'Sticky' requires at least one of 'Blocks' or 'Entities'");
        }

        return new Sticky(blocks, entities);
    }

    @Override
    public Sticky clone() {
        try {
            return (Sticky) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
