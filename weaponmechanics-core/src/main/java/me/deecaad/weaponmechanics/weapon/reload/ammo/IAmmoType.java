package me.deecaad.weaponmechanics.weapon.reload.ammo;

import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import org.bukkit.inventory.ItemStack;

public interface IAmmoType {

    boolean hasAmmo(PlayerWrapper playerWrapper);

    int removeAmmo(ItemStack weaponStack, PlayerWrapper playerWrapper, int amount, int maximumMagazineSize);

    void giveAmmo(ItemStack weaponStack, PlayerWrapper playerWrapper, int amount, int maximumMagazineSize);

    int getMaximumAmmo(PlayerWrapper playerWrapper, int maximumMagazineSize);
}