package me.deecaad.weaponmechanics.lib;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import me.deecaad.weaponmechanics.WeaponMechanicsAPI;
import org.bukkit.entity.LivingEntity;

public class MythicMobsReloadingCondition implements IEntityCondition {

    public MythicMobsReloadingCondition(MythicLineConfig config) {
    }

    @Override
    public boolean check(AbstractEntity abstractEntity) {
        if (!abstractEntity.getBukkitEntity().getType().isAlive())
            return false;

        return WeaponMechanicsAPI.isReloading((LivingEntity) abstractEntity.getBukkitEntity());
    }
}
