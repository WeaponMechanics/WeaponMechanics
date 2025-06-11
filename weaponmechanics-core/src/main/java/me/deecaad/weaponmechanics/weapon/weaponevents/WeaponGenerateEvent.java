package me.deecaad.weaponmechanics.weapon.weaponevents;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Called whenever a weapon is generated via command or API. This event can be used to modify
 * weapons as players generate them. WeaponMechanicsPlus does this to automatically add attachments
 * based on arguments.
 */
public class WeaponGenerateEvent extends WeaponEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Map<String, Object> data;

    public WeaponGenerateEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, Map<String, Object> data) {
        super(weaponTitle, weaponStack, shooter, null);
        this.data = data;
    }

    public @NotNull Map<String, Object> getData() {
        return data;
    }

    public @Nullable CommandSender getSender() {
        return getOrDefault("sender", null);
    }

    public <T> @Nullable T getOrDefault(String key, T value) {
        // noinspection unchecked
        return (T) data.getOrDefault(key, value);
    }

    @Override
    @NotNull public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
