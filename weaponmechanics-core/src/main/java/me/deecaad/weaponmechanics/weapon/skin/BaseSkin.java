package me.deecaad.weaponmechanics.weapon.skin;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;

public class BaseSkin implements Skin, Serializer<BaseSkin> {

    private Optional<ItemType> type;
    private Optional<NamespacedKey> itemModel;
    private OptionalInt customModelData;

    /**
     * Default constructor for Serializer
     */
    public BaseSkin() {
    }

    public BaseSkin(int customModelData) {
        this.type = Optional.empty();
        this.itemModel = Optional.empty();
        this.customModelData = OptionalInt.of(customModelData);
    }

    public BaseSkin(
        @NotNull Optional<ItemType> type,
        @NotNull Optional<NamespacedKey> itemModel,
        @NotNull OptionalInt customModelData) {
        this.type = type;
        this.itemModel = itemModel;
        this.customModelData = customModelData;
    }

    public boolean hasType() {
        return type.isPresent();
    }

    public @Nullable ItemType getType() {
        return type.orElse(null);
    }

    public boolean hasCustomModelData() {
        return customModelData.isPresent();
    }

    public int getCustomModelData() {
        return customModelData.orElse(Integer.MIN_VALUE);
    }

    /**
     * Applies this skin to given item stack
     *
     * @param weapon the item stack for which to apply skin
     */
    public void apply(@NotNull ItemStack weapon) {
        if (type.isPresent() && weapon.getType().asItemType() != type.get())
            weapon.setType(type.get().asMaterial());

        boolean hasMetaChanges = false;
        ItemMeta meta = weapon.getItemMeta();

        // Happens when somebody tries to apply a skin to air
        if (meta == null)
            throw new IllegalArgumentException("Tried to apply skin to item without meta: " + weapon);

        if (itemModel.isPresent() && (!meta.hasItemModel() || !meta.getItemModel().equals(itemModel.get()))) {
            meta.setItemModel(itemModel.get());
            hasMetaChanges = true;
        }

        if (customModelData.isPresent() && (!meta.hasCustomModelData() || meta.getCustomModelData() != customModelData.getAsInt())) {
            meta.setCustomModelData(customModelData.getAsInt());
            hasMetaChanges = true;
        }

        if (hasMetaChanges) {
            weapon.setItemMeta(meta);
        }
    }

    @Override
    public @NotNull BaseSkin serialize(@NotNull SerializeData data) throws SerializerException {
        Optional<ItemType> type = data.of("Type").getBukkitRegistry(ItemType.class);
        Optional<NamespacedKey> itemModel = data.of("Item_Model").getNamespacedKey();
        OptionalInt customModelData = data.of("Custom_Model_Data").getInt();

        // Should use at least 1
        if (type.isEmpty() && itemModel.isEmpty() && customModelData.isEmpty()) {
            throw data.exception(null, "Tried to create a skin without using 'Type' or 'Item_Model' or 'Custom_Model_Data'... did you misspell something?",
                "Double check your configs to make sure it is spelled right.");
        }

        return new BaseSkin(type, itemModel, customModelData);
    }
}