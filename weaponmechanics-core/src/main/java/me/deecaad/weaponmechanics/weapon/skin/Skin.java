package me.deecaad.weaponmechanics.weapon.skin;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link Skin} interface is to abstract out the {@link #apply(ItemStack)} method, since there
 * are 2 different skin systems that can work together.
 *
 * <p>
 * First you got the {@link BaseSkin}. The base skin works by having set values (for example,
 * <code>Custom_Model_Data: 72</code>). So any item will end up with a CMD of 72.
 *
 * <p>
 * Next you got the {@link RelativeSkin}. The relative skin ADDS a value to the "central" base skin
 * (The "central" base skin being the one you get from calling
 * {@link SkinSelector#getDefaultSkin()}). For example, using <code>Custom_Model_Data: +10</code>
 * with the above^^ {@link BaseSkin} will result in an item with a CMD of 82.
 */
public interface Skin {

    /**
     * Modifies the given item to be match this skin.
     *
     * @param item The non-null item to modify
     */
    void apply(@NotNull ItemStack item);
}
