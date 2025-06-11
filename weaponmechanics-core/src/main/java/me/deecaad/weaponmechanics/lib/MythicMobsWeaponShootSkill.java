package me.deecaad.weaponmechanics.lib;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.WeaponMechanicsAPI;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class MythicMobsWeaponShootSkill implements ITargetedEntitySkill, ITargetedLocationSkill {

    private final String weaponTitle;
    private final double spread;
    private final boolean targetHead;

    public MythicMobsWeaponShootSkill(MythicLineConfig config) {

        String weaponTitle = config.getString(new String[]{"weaponTitle", "weapon"});
        this.spread = Math.max(0, Math.toRadians(config.getDouble(new String[]{"spread"}, 0.0)));
        this.targetHead = config.getBoolean("head", true);

        // Parse an accurate weaponTitle (Checking case/spelling)
        try {
            weaponTitle = WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler().getWeaponTitle(weaponTitle);
        } catch (IllegalArgumentException e) {
            // Error occurs if MythicMobs serializes before WeaponMechanics, so just skip fancy shit
        }

        this.weaponTitle = weaponTitle;
    }

    @Override
    public ThreadSafetyLevel getThreadSafetyLevel() {
        return ThreadSafetyLevel.SYNC_ONLY;
    }

    @Override
    public boolean getTargetsSpectators() {
        return false;
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity abstractEntity) {
        Entity entity = abstractEntity.getBukkitEntity();

        Location target;
        if (targetHead && entity.getType().isAlive()) {
            target = ((LivingEntity) entity).getEyeLocation();
        } else {
            target = entity.getLocation();
            target.setY(target.getY() + entity.getHeight() / 2.0);
        }

        return castAtLocation(skillMetadata, BukkitAdapter.adapt(target));
    }

    @Override
    public SkillResult castAtLocation(SkillMetadata skillMetadata, AbstractLocation abstractLocation) {
        Entity entity = skillMetadata.getCaster().getEntity().getBukkitEntity();

        // I don't think this is ever true, but their code isn't annotated,
        // so I am going to double-check to avoid a NPE.
        if (entity == null)
            return SkillResult.ERROR;

        // This probably doesn't often happen, but mythic mobs does allow for
        // non-living skill casters.
        if (!entity.getType().isAlive())
            return SkillResult.ERROR;

        Location target = BukkitAdapter.adapt(abstractLocation);

        // Skip the calculations if we can
        if (spread != 0.0) {
            // delta is always positive, since spread is always positive
            double delta = Math.tan(spread) * entity.getLocation().distance(target);
            target.setX(target.getX() + RandomUtil.range(-delta, delta));
            target.setY(target.getY() + RandomUtil.range(-delta, delta));
            target.setZ(target.getZ() + RandomUtil.range(-delta, delta));
        }

        WeaponMechanicsAPI.shoot((LivingEntity) entity, weaponTitle, target);
        return SkillResult.SUCCESS;
    }
}
