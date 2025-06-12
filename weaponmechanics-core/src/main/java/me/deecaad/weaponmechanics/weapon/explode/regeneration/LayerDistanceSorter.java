package me.deecaad.weaponmechanics.weapon.explode.regeneration;

import me.deecaad.weaponmechanics.weapon.explode.Explosion;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class LayerDistanceSorter extends BlockRegenSorter {

    public LayerDistanceSorter(Location origin, Explosion explosion) {
        super(origin, explosion);
    }

    @Override
    public int compare(Block a, Block b) {
        int height = Integer.compare(a.getY(), b.getY());

        if (height != 0)
            return height;
        else
            return calculateDistance(a, b);
    }

    private int calculateDistance(Block a, Block b) {
        int x = origin.getBlockX();
        int y = origin.getBlockY();
        int z = origin.getBlockZ();

        int distanceA = square(a.getX() - x) + square(a.getY() - y) + square(a.getZ() - z);
        int distanceB = square(b.getX() - x) + square(b.getY() - y) + square(b.getZ() - z);

        // Negative for outer blocks before inner blocks
        return -Integer.compare(distanceA, distanceB);
    }

    private static int square(int a) {
        return a * a;
    }
}
