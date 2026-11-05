package me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.ray.BlockTraceResult;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BlockPlacement implements Serializer<BlockPlacement> {

    private static final double[][] LINE_OF_SIGHT_SAMPLES = {
        {0.5, 0.5, 0.5},
        {0.25, 0.75, 0.25},
        {0.75, 0.25, 0.75}
    };

    public enum PlacementMode {
        PLACE_ADJACENT,
        REPLACE,
        REPLACE_AIR,
        REMOVE
    }

    private Material block;
    private PlacementMode mode;
    private int removeAfterTicks;
    private boolean lineOfSight;
    private double radius;
    private double chance;

    public BlockPlacement() {
    }

    public BlockPlacement(Material block, PlacementMode mode, int removeAfterTicks, boolean lineOfSight, double radius, double chance) {
        this.block = block;
        this.mode = mode;
        this.removeAfterTicks = removeAfterTicks;
        this.lineOfSight = lineOfSight;
        this.radius = radius;
        this.chance = chance;
    }

    public void handleBlockPlacement(BlockTraceResult result, WeaponProjectile projectile) {
        if (getPlacedMaterial() == null) {
            return;
        }

        Block target = result.getBlock();
        if (mode == PlacementMode.PLACE_ADJACENT) {
            target = target.getRelative(result.getHitFace());
            if (!target.getType().isAir() && !target.isLiquid() && !target.isPassable()) {
                return;
            }
        }

        BlockState oldState = target.getState();
        target.setType(getPlacedMaterial());

        if (removeAfterTicks > 0) {
            WeaponMechanics.getInstance().getFoliaScheduler().global().runDelayed(() -> oldState.update(true, true), removeAfterTicks);
        }
    }

    public void handleExplosionPlacement(@Nullable Location origin, List<Block> blocks) {
        if (mode == PlacementMode.PLACE_ADJACENT) {
            return;
        }

        Material placedMaterial = getPlacedMaterial();
        if (placedMaterial == null) {
            return;
        }

        List<BlockState> oldStates = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (Block candidate : blocks) {
            if (mode == PlacementMode.REPLACE_AIR && !candidate.getType().isAir() && !candidate.isPassable()) {
                continue;
            }

            if (radius > 0 && origin != null) {
                Vector blockCenter = candidate.getLocation().add(0.5, 0.5, 0.5).toVector();
                if (blockCenter.distanceSquared(origin.toVector()) > radiusSquared) {
                    continue;
                }
            }

            if (lineOfSight && origin != null && isLineOfSightBlocked(origin, candidate)) {
                continue;
            }

            if (chance < 1.0 && ThreadLocalRandom.current().nextDouble() >= chance) {
                continue;
            }

            oldStates.add(candidate.getState());
            candidate.setType(placedMaterial);
        }

        if (removeAfterTicks <= 0 || oldStates.isEmpty()) {
            return;
        }

        WeaponMechanics.getInstance().getFoliaScheduler().global().runDelayed(() -> {
            for (BlockState state : oldStates) {
                state.update(true, true);
            }
        }, removeAfterTicks);
    }

    private @Nullable Material getPlacedMaterial() {
        if (mode == PlacementMode.REMOVE) {
            return Material.AIR;
        }

        return block;
    }

    private boolean isLineOfSightBlocked(Location origin, Block candidate) {
        Vector originVector = origin.toVector();

        for (double[] sample : LINE_OF_SIGHT_SAMPLES) {
            Vector target = candidate.getLocation().add(sample[0], sample[1], sample[2]).toVector();
            Vector direction = target.clone().subtract(originVector);
            double distance = direction.length();
            if (distance <= 0) {
                continue;
            }

            RayTraceResult hit = origin.getWorld().rayTraceBlocks(origin, direction.normalize(), distance, FluidCollisionMode.NEVER, true);
            if (hit == null) {
                continue;
            }

            Block hitBlock = hit.getHitBlock();
            if (hitBlock == null || !hitBlock.equals(candidate)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getKeyword() {
        return "Block_Placement";
    }

    @Override
    public @NotNull BlockPlacement serialize(@NotNull SerializeData data) throws SerializerException {
        PlacementMode mode = data.of("Mode").getEnum(PlacementMode.class).orElse(PlacementMode.PLACE_ADJACENT);

        Material block = data.of("Block").getEnum(Material.class).orElse(null);
        if (mode != PlacementMode.REMOVE && (block == null || !block.isBlock())) {
            WeaponMechanics.getInstance().getDebugger().warning(
                "Block_Placement at '" + data.getKey() + "' needs a placeable block material in 'Block' for mode " + mode + ", found '" + block + "'.",
                "Use a solid block like STONE, SPONGE, DIRT, etc. Block placement will be skipped.");
            block = null;
        }

        int removeAfterTicks = data.of("Remove_After_Ticks").getInt().orElse(-1);
        boolean lineOfSight = data.of("Line_Of_Sight").getBool().orElse(false);
        double radius = data.of("Radius").getDouble().orElse(-1.0);
        double chance = data.of("Chance").getDouble().orElse(1.0);
        if (chance < 0.0 || chance > 1.0) {
            WeaponMechanics.getInstance().getDebugger().warning(
                "'Chance' at '" + data.getKey() + "' must be between 0.0 and 1.0 but was " + chance + ".",
                "Clamping to the valid range.");
            chance = Math.max(0.0, Math.min(1.0, chance));
        }

        return new BlockPlacement(block, mode, removeAfterTicks, lineOfSight, radius, chance);
    }
}
