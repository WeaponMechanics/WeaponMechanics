package me.deecaad.weaponmechanics.weapon.explode.regeneration;

import me.deecaad.weaponmechanics.weapon.explode.Explosion;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Comparator;

/**
 * This class outlines a sorter to define the order that blocks should regenerate from an explosion.
 * For most cases, generating the lowest layers before the highest layers is a good idea.
 */
public abstract class BlockRegenSorter implements Comparator<Block> {

    protected Location origin;
    protected Explosion explosion;

    public BlockRegenSorter(Location origin, Explosion explosion) {
        this.origin = origin;
        this.explosion = explosion;
    }
}
