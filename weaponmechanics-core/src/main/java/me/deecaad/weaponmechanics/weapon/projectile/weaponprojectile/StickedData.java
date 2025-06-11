package me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile;

import me.deecaad.core.compatibility.HitBox;
import me.deecaad.core.utils.ray.BlockTraceResult;
import me.deecaad.core.utils.ray.EntityTraceResult;
import me.deecaad.core.utils.ray.RayTraceResult;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StickedData {

    private Location blockLocation;
    private LivingEntity livingEntity;
    private final Vector relativeLocation;
    private final String worldName;

    public StickedData(@NotNull RayTraceResult hit) {
        if (hit instanceof BlockTraceResult blockHit) {
            blockLocation = blockHit.getBlock().getLocation();
            relativeLocation = hit.getHitLocation().clone().subtract(blockLocation.toVector());
            worldName = blockLocation.getWorld().getName();
        } else if (hit instanceof EntityTraceResult entityHit) {
            livingEntity = entityHit.getEntity();
            relativeLocation = hit.getHitLocation().clone().subtract(livingEntity.getLocation().toVector());
            worldName = livingEntity.getWorld().getName();
        } else {
            throw new IllegalArgumentException("RayTraceResult is not BlockTraceResult or EntityTraceResult");
        }
    }

    public StickedData(@NotNull WeaponProjectile projectile, @NotNull Block block) {
        blockLocation = block.getLocation();
        relativeLocation = projectile.getLocation().add(new Vector(0, 0.05, 0)).subtract(blockLocation.toVector());
        worldName = blockLocation.getWorld().getName();
    }

    public @Nullable Vector getNewLocation() {
        if (livingEntity != null) {
            return livingEntity.isDead() || !worldName.equals(livingEntity.getWorld().getName()) ? null : livingEntity.getLocation().clone().add(relativeLocation).toVector();
        }
        return HitBox.getHitbox(blockLocation.getBlock(), false) == null ? null : blockLocation.clone().add(relativeLocation).toVector();
    }

    public boolean isBlockStick() {
        return blockLocation != null;
    }

    public @Nullable LivingEntity getLivingEntity() {
        return livingEntity == null || livingEntity.isDead() || !worldName.equals(livingEntity.getWorld().getName()) ? null : livingEntity;
    }

    public @Nullable Block getBlock() {
        if (blockLocation == null)
            return null;
        Block block = blockLocation.getBlock();
        return HitBox.getHitbox(block, false) == null ? null : block;
    }
}