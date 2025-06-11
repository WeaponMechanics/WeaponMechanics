package me.deecaad.weaponmechanics.weapon.reload.ammo;

import me.deecaad.core.utils.AdventureUtil;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.utils.CustomTag;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;

public class ItemAmmo implements IAmmoType {

    // Defined in ammo types list
    private final String ammoTitle;
    private final ItemStack bulletItem;
    private final ItemStack magazineItem;
    private final AmmoConverter ammoConverter;

    public ItemAmmo(String ammoTitle, ItemStack bulletItem, ItemStack magazineItem, AmmoConverter ammoConverter) {
        this.ammoTitle = ammoTitle;
        this.bulletItem = bulletItem;
        this.magazineItem = magazineItem;
        this.ammoConverter = ammoConverter;
    }

    public ItemStack getBulletItem() {
        return bulletItem == null ? null : bulletItem.clone();
    }

    public ItemStack getMagazineItem() {
        return magazineItem == null ? null : magazineItem.clone();
    }

    @Override
    public boolean hasAmmo(PlayerWrapper wrapper) {
        PlayerInventory inventory = wrapper.getPlayer().getInventory();
        boolean convert = NumberUtil.hasMillisPassed(wrapper.getLastAmmoConvert(), WeaponMechanics.getInstance().getConfiguration().getInt("Milliseconds_Between_Ammo_Conversions", 10000));
        boolean hasAmmo = false;

        // Check hotbar + inventory slots
        for (int i = 0; i < 36; ++i) {

            // Do not consume held item since it is probably a weapon
            if (i == inventory.getHeldItemSlot())
                continue;

            ItemStack potentialAmmo = inventory.getItem(i);
            if (potentialAmmo == null || potentialAmmo.getType() == Material.AIR)
                continue;

            // The conversion process attempts to convert all items in your
            // inventory every time you reload (With a 10-second cool down).
            // When we are on cool down (convert = false), then we just want
            // to return true ASAP for performance.
            String potentialAmmoName = CustomTag.AMMO_TITLE.getString(potentialAmmo);
            if (Objects.equals(ammoTitle, potentialAmmoName)) {
                hasAmmo = true;
                if (!convert)
                    return true;
            }

            // 1. Don't do conversion checks if there is no converter.
            // 2. Don't do conversion checks if we are on cool down
            // 3. Item *IS* an ammo item, but not the correct one. Skip it.
            if (ammoConverter == null || !convert || potentialAmmoName != null)
                continue;

            // Determine if this item matches the bullet template, or the
            // magazine template (or neither).
            ItemStack ammoTemplate = null;
            if (bulletItem != null && ammoConverter.isMatch(potentialAmmo, bulletItem))
                ammoTemplate = bulletItem.clone();
            if (magazineItem != null && ammoConverter.isMatch(potentialAmmo, magazineItem))
                ammoTemplate = magazineItem.clone();

            // Item did not match either of the ammo templates, skip it.
            if (ammoTemplate == null)
                continue;

            // Handle conversion
            potentialAmmo.setType(ammoTemplate.getType());
            potentialAmmo.setItemMeta(ammoTemplate.getItemMeta());
            AdventureUtil.updatePlaceholders(wrapper.getPlayer(), potentialAmmo);

            inventory.setItem(i, potentialAmmo);
            hasAmmo = true;
        }

        // Regardless of whether we converted any ammo, we should reset the
        // timer, so we have at least 10 seconds between conversion checks.
        if (ammoConverter != null)
            wrapper.convertedAmmo();

        return hasAmmo;
    }

    @Override
    public int removeAmmo(ItemStack weapon, PlayerWrapper wrapper, int amount, int maximumMagSize) {
        PlayerInventory inventory = wrapper.getPlayer().getInventory();
        int magazineSlot = -1;
        int total = 0;

        for (int i = 0; i < 36; i++) {

            // Do not consume held item since it is probably a weapon
            if (i == inventory.getHeldItemSlot())
                continue;

            ItemStack potentialAmmo = inventory.getItem(i);
            if (potentialAmmo == null || potentialAmmo.getType() == Material.AIR)
                continue;

            // No conversion checks here (Conversions are handled by the
            // hasAmmo() method). If the ammo type doesn't match, SKIP.
            String potentialAmmoName = CustomTag.AMMO_TITLE.getString(potentialAmmo);
            if (!Objects.equals(ammoTitle, potentialAmmoName))
                continue;

            // Consider that people will configure both BULLETS and MAGAZINES.
            // Users will shoot their gun until it is half empty, and expect it
            // to be reloaded using BULLET items (so no ammo is wasted).
            boolean isMagazine = CustomTag.AMMO_MAGAZINE.getInteger(potentialAmmo) == 1;
            boolean canUseMag = total == 0 && (bulletItem == null || amount >= maximumMagSize);

            if (isMagazine) {
                magazineSlot = i;
                if (canUseMag) {
                    consumeItem(inventory, i, potentialAmmo, 1);
                    return amount;
                }
            } else if (bulletItem != null) {

                // If the one stack of bullets is enough to fill the gun, then
                // consume only that stack and stop.
                if (potentialAmmo.getAmount() >= amount) {
                    // amount -= potentialAmmo.getAmount();
                    total += amount;
                    consumeItem(inventory, i, potentialAmmo, amount);
                    return total;
                }

                // Completely consume the ammo
                amount -= potentialAmmo.getAmount();
                total += potentialAmmo.getAmount();
                inventory.setItem(i, null);
            }
        }

        // In order for this code to execute, the weapon has to be PARTIALLY
        // full, with bullet items configured (but no bullets in the inventory),
        // and magazines in the inventory. So this reload was probably manually
        // triggered by the player, so we should use the magazines in the inventory.
        if (total == 0 && magazineSlot != -1) {
            consumeItem(inventory, magazineSlot, inventory.getItem(magazineSlot), 1);
            return amount;
            // TODO refund individual bullets?
        }

        return total;
    }

    private void consumeItem(PlayerInventory playerInventory, int index, ItemStack itemStack, int amount) {
        int itemAmount = itemStack.getAmount();
        if (itemAmount > amount) {
            itemStack.setAmount(itemAmount - amount);
            playerInventory.setItem(index, itemStack);
        } else {
            playerInventory.setItem(index, null);
        }
    }

    @Override
    public void giveAmmo(ItemStack weaponStack, PlayerWrapper playerWrapper, int amount, int maximumMagazineSize) {
        Player player = playerWrapper.getPlayer();

        if (magazineItem != null) {
            int magazinesGiveAmount = amount / maximumMagazineSize;
            if (magazinesGiveAmount > 0) {
                giveOrDrop(player, magazineItem.clone(), magazinesGiveAmount);
            }

            // Give rest of the ammo as bullet items back if defined
            if (bulletItem != null) {
                int remainder = amount % maximumMagazineSize;
                if (remainder > 0) {
                    giveOrDrop(player, bulletItem.clone(), remainder);
                }
            }
            return;
        }

        // Magazines weren't used so simply give bullet items using given amount
        giveOrDrop(player, bulletItem.clone(), amount);
    }

    @Override
    public int getMaximumAmmo(PlayerWrapper playerWrapper, int maximumMagazineSize) {
        PlayerInventory playerInventory = playerWrapper.getPlayer().getInventory();

        int amount = 0;

        // Only check hotbar slots and normal inventory slots meaning slots from 0 to 35
        for (int i = 0; i < 36; ++i) {

            // Don't check currently held slot either since it's weapon
            if (i == playerInventory.getHeldItemSlot())
                continue;

            ItemStack potentialAmmo = playerInventory.getItem(i);
            if (potentialAmmo == null || potentialAmmo.getType() == Material.AIR)
                continue;

            String potentialAmmoName = CustomTag.AMMO_TITLE.getString(potentialAmmo);
            if (potentialAmmoName == null || !potentialAmmoName.equals(ammoTitle))
                continue;

            // Now we know it's actually an ammo item
            if (CustomTag.AMMO_MAGAZINE.getInteger(potentialAmmo) == 1) {
                amount += (potentialAmmo.getAmount() * maximumMagazineSize);
            } else {
                amount += potentialAmmo.getAmount();
            }
        }
        return amount;
    }

    private void giveOrDrop(Player player, ItemStack itemStack, int amount) {

        // More compatible this way if this gets changed in the future
        int maximumStackSize = player.getInventory().getMaxStackSize();

        while (amount > maximumStackSize) {

            // Keep cloning since we do modifications for same item constantly
            // while adding it to player inventory
            ItemStack clone = itemStack.clone();

            clone.setAmount(amount);
            giveOrDrop(player, clone);
            amount -= maximumStackSize;
        }

        if (amount > 0) {

            // Given item stack is cloned in all cases
            itemStack.setAmount(amount);

            giveOrDrop(player, itemStack);
        }
    }

    private void giveOrDrop(Player player, ItemStack itemStack) {
        Inventory inventory = player.getInventory();
        AdventureUtil.updatePlaceholders(player, itemStack);

        // Check if inventory doesn't have any free slots
        if (inventory.firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation().add(0.0, 1.0, 0.0), itemStack);
            return;
        }
        inventory.addItem(itemStack);
    }
}
