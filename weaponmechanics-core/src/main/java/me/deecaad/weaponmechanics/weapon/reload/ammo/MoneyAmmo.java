package me.deecaad.weaponmechanics.weapon.reload.ammo;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.vault.IVaultCompatibility;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MoneyAmmo implements IAmmoType {

    private final double moneyAsAmmoCost;

    public MoneyAmmo(double moneyAsAmmoCost) {
        this.moneyAsAmmoCost = moneyAsAmmoCost;
    }

    @Override
    public boolean hasAmmo(PlayerWrapper playerWrapper) {
        IVaultCompatibility vault = CompatibilityAPI.getVaultCompatibility();
        return vault.getBalance(playerWrapper.getPlayer()) >= moneyAsAmmoCost;
    }

    @Override
    public int removeAmmo(ItemStack weaponStack, PlayerWrapper playerWrapper, int amount, int maximumMagazineSize) {
        IVaultCompatibility vault = CompatibilityAPI.getVaultCompatibility();
        if (amount == 0)
            return 0;
        Player player = playerWrapper.getPlayer();
        double balance = vault.getBalance(player);
        if (balance == 0)
            return 0;

        double removeMoney = this.moneyAsAmmoCost * amount;

        // Check if there isn't enough money to withdraw
        if (removeMoney > balance) {

            // Recalculate amount to match the maximum amount that can be withdrawed
            amount = (int) (balance / moneyAsAmmoCost);
            if (amount == 0)
                return 0;

            vault.withdrawBalance(player, amount * moneyAsAmmoCost);
            return amount;
        }

        vault.withdrawBalance(player, removeMoney);
        return amount;
    }

    @Override
    public void giveAmmo(ItemStack weaponStack, PlayerWrapper playerWrapper, int amount, int maximumMagazineSize) {
        IVaultCompatibility vault = CompatibilityAPI.getVaultCompatibility();
        if (amount == 0)
            return;
        vault.depositBalance(playerWrapper.getPlayer(), this.moneyAsAmmoCost * amount);
    }

    @Override
    public int getMaximumAmmo(PlayerWrapper playerWrapper, int maximumMagazineSize) {
        IVaultCompatibility vault = CompatibilityAPI.getVaultCompatibility();
        double balance = vault.getBalance(playerWrapper.getPlayer());
        if (balance == 0)
            return 0;

        // Divide with money cost and convert to int
        return (int) (balance / moneyAsAmmoCost);
    }
}