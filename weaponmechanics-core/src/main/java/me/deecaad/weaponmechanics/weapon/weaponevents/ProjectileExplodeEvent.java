package me.deecaad.weaponmechanics.weapon.weaponevents;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.weaponmechanics.weapon.explode.regeneration.BlockRegenSorter;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called whenever an explosion is spawned by a weapon (When a projectile explodes, usually). While
 * this event is {@link Cancellable}, you should <i>ALWAYS</i> cancel
 * {@link ProjectilePreExplodeEvent} (this will skip a lot more calculations, saving that precious
 * CPU).
 */
public class ProjectileExplodeEvent extends ProjectileEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private List<Block> blocks;
    private BlockRegenSorter sorter;
    private Object2DoubleMap<LivingEntity> entities;

    private MechanicManager mechanics;

    private boolean isCancelled;

    public ProjectileExplodeEvent(WeaponProjectile projectile, List<Block> blocks, BlockRegenSorter sorter,
        Object2DoubleMap<LivingEntity> entities, MechanicManager mechanics) {
        super(projectile);

        this.blocks = blocks;
        this.sorter = sorter;
        this.entities = entities;

        this.mechanics = mechanics;
    }

    /**
     * Returns the list of blocks that were affected by the
     * {@link me.deecaad.weaponmechanics.weapon.explode.shapes.ExplosionShape}. You can modify this
     * list.
     *
     * @return The non-null list of blocks affected.
     */
    public List<Block> getBlocks() {
        return blocks;
    }

    /**
     * Overrides the current list of blocks affected by the explosion. Using this method may cause
     * compatibility issues with other plugins using this event, so use this carefully.
     *
     * @param blocks The non-null list of blocks to be destroyed.
     */
    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    /**
     * A {@link BlockRegenSorter} simply sorts the block list after this event is called. This is nice
     * for regeneration, as we can make the explosions regenerate in a "satisfying pattern".
     *
     * @return The current block regen sorter.
     */
    public BlockRegenSorter getSorter() {
        return sorter;
    }

    /**
     * Sets the sorter that will decide the order blocks regenerate in.
     *
     * @param sorter The block sorter to use, or null.
     * @see #getSorter()
     */
    public void setSorter(BlockRegenSorter sorter) {
        this.sorter = sorter;
    }

    /**
     * Gets the list of entities mapped to their exposure (A number 0..1 that determines how exposed
     * that entity is to the explosion. Higher numbers mean more damage).
     *
     * @return The non-null entities mapped to their exposures.
     */
    public Object2DoubleMap<LivingEntity> getEntities() {
        return entities;
    }

    /**
     * Overrides the current list of entities affected by the explosion. Using this method may cause
     * compatibility issues with other plugins using this event, so use this carefully.
     *
     * @param entities The non-null map of entities and their exposures (0..1).
     */
    public void setEntities(Object2DoubleMap<LivingEntity> entities) {
        this.entities = entities;
    }

    public MechanicManager getMechanics() {
        return mechanics;
    }

    public void setMechanics(MechanicManager mechanics) {
        if (this.mechanics != null)
            this.mechanics.clearDirty(); // clear any modifications
        this.mechanics = mechanics;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }
}
