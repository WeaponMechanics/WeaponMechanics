package me.deecaad.weaponmechanics.weapon;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.Projectile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * A dummy serializer to handle basic weapon logic... No "Weapon" object is ever created, since
 * each section (Shoot, Info, Explosion, etc.) have their own serialization logic and get added
 */
public class WeaponSerializer implements Serializer<WeaponSerializer> {

    @Override
    public @NotNull WeaponSerializer serialize(@NotNull SerializeData data) throws SerializerException {
        String weaponTitle = data.getKey();

        // Ensure the weapon title uses the correct format, mostly for other plugin compatibility
        Pattern pattern = Pattern.compile("[A-Za-z0-9_]+");
        if (!pattern.matcher(weaponTitle).matches()) {
            throw data.exception(null, "Weapon title must only contain letters, numbers, and underscores!",
                    "For example, AK-47 is not allowed, but AK_47 is fine",
                    "This is only for the weapon title (the name defined in config), NOT the display name of the weapon. The display can be whatever you want.",
                    "Found weapon title: " + weaponTitle);
        }

        WeaponMechanics.getInstance().getWeaponHandler().getInfoHandler().addWeapon(weaponTitle);

        // return null since this is a dummy serializer
        return new WeaponSerializer();
    }
}
