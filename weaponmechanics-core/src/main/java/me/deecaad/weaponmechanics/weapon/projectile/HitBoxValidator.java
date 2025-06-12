package me.deecaad.weaponmechanics.weapon.projectile;

import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.IValidator;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.damage.DamagePoint;
import org.bukkit.entity.EntityType;

public class HitBoxValidator implements IValidator {

    /**
     * Default constructor for validator
     */
    public HitBoxValidator() {
    }

    @Override
    public String getKeyword() {
        return "Entity_Hitboxes";
    }

    @Override
    public void validate(Configuration configuration, SerializeData data) throws SerializerException {
        MechanicsLogger debug = WeaponMechanics.getInstance().debugger;
        for (EntityType entityType : EntityType.values()) {
            if (!entityType.isAlive())
                continue;

            double head = data.of(entityType.name() + "." + DamagePoint.HEAD.name()).getDouble().orElse(-1.0);
            double body = data.of(entityType.name() + "." + DamagePoint.BODY.name()).getDouble().orElse(-1.0);
            double legs = data.of(entityType.name() + "." + DamagePoint.LEGS.name()).getDouble().orElse(-1.0);
            double feet = data.of(entityType.name() + "." + DamagePoint.FEET.name()).getDouble().orElse(-1.0);

            if (head < 0 || body < 0 || legs < 0 || feet < 0) {
                debug.warning("Entity type " + entityType.name() + " is missing some of its damage point values, please add it",
                    "Located at file /WeaponMechanics/config.yml in Entity_Hitboxes." + entityType.name() + " in configurations",
                    "Its missing one of these: HEAD, BODY, LEGS or FEET");

                putDefaults(configuration, entityType);
                continue;
            }

            boolean horizontalEntity = configuration.getBoolean("Entity_Hitboxes." + entityType.name() + ".Horizontal_Entity", false);
            if (horizontalEntity && head > 0.0) {
                debug.warning("Entity type " + entityType.name() + " hit box had horizontal entity true and HEAD was not 0.0",
                    "Located at file /WeaponMechanics/config.yml in Entity_Hitboxes." + entityType.name() + " in configurations",
                    "When using horizontal entity true HEAD should be set to 0.0!");

                // Set default value to BODY
                putDefaults(configuration, entityType);
                continue;
            }

            double sumOf = head + body + legs + feet;
            if (Math.abs(sumOf - 1.0) > 1e-5) { // If the numbers are not super close together (floating point issues)
                debug.warning("Entity type " + entityType.name() + " hit box values sum doesn't match 1.0",
                    "Located at file /WeaponMechanics/config.yml in Entity_Hitboxes." + entityType.name() + " in configurations",
                    "Now the total sum was " + sumOf + ", please make it 1.0.");

                putDefaults(configuration, entityType);
            }
        }
    }

    /**
     * Simply resets hit boxes to default is they're missing or are invalid
     *
     * @param basicConfiguration the config.yml configuration instance
     * @param entityType the entity type
     */
    private void putDefaults(Configuration basicConfiguration, EntityType entityType) {
        basicConfiguration.set("Entity_Hitboxes." + entityType.name() + "." + DamagePoint.HEAD.name(), 0.0);

        // Set default value to BODY 100%
        basicConfiguration.set("Entity_Hitboxes." + entityType.name() + "." + DamagePoint.BODY.name(), 1.0);

        basicConfiguration.set("Entity_Hitboxes." + entityType.name() + "." + DamagePoint.LEGS.name(), 0.0);
        basicConfiguration.set("Entity_Hitboxes." + entityType.name() + "." + DamagePoint.FEET.name(), 0.0);

        basicConfiguration.set("Entity_Hitboxes." + entityType.name() + "." + DamagePoint.ARMS.name(), false);
        basicConfiguration.set("Entity_Hitboxes." + entityType.name() + ".Horizontal_Entity", false);
    }
}