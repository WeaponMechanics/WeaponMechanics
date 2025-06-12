package me.deecaad.weaponmechanics.compatibility;

import me.deecaad.core.compatibility.CompatibilitySetup;

public class WeaponCompatibilityAPI {

    private static IWeaponCompatibility weaponCompatibility;

    public static IWeaponCompatibility getWeaponCompatibility() {
        if (weaponCompatibility == null) {
            weaponCompatibility = new CompatibilitySetup().getCompatibleVersion(IWeaponCompatibility.class, "me.deecaad.weaponmechanics.compatibility");
        }
        return weaponCompatibility;
    }
}