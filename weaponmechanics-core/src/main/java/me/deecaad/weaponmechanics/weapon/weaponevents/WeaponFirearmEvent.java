package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.weaponmechanics.weapon.firearm.FirearmAction;
import me.deecaad.weaponmechanics.weapon.firearm.FirearmState;
import me.deecaad.weaponmechanics.weapon.firearm.FirearmType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * When a weapon's {@link FirearmState} changes.
 */
public class WeaponFirearmEvent extends WeaponEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FirearmAction action;
    private final FirearmState state;
    private MechanicManager mechanics;
    private int time;

    public WeaponFirearmEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, EquipmentSlot hand, FirearmAction action, FirearmState state) {
        super(weaponTitle, weaponStack, shooter, hand);
        this.action = action;
        this.state = state;

        time = -1;
    }

    /**
     * The config options of the firearm. You probably do not want to modify this value.
     *
     * @return The firearm config options.
     */
    public FirearmAction getAction() {
        return action;
    }

    /**
     * Returns the firearm type.
     *
     * @return The firearm type.
     */
    public FirearmType getType() {
        return action.getFirearmType();
    }

    /**
     * Returns whether the state is OPEN, CLOSE, or READY.
     *
     * @return The firearm state.
     */
    public FirearmState getState() {
        return state;
    }

    /**
     * The mechanics that will be used (usually for sounds).
     *
     * @return The mechanics that will be played after the event.
     */
    public MechanicManager getMechanics() {
        if (mechanics == null)
            return state == FirearmState.CLOSE ? action.getClose() : action.getOpen();

        return mechanics;
    }

    /**
     * The mechanics that will be used (usually for sounds).
     *
     * @param mechanics The mechanics that will be played after the event.
     */
    public void setMechanics(MechanicManager mechanics) {
        this.mechanics = mechanics;
    }

    /**
     * Returns how long it takes for the firearm action to be completed.
     *
     * @return The firearm action time.
     */
    public int getTime() {
        if (time == -1)
            return state == FirearmState.CLOSE ? action.getCloseTime() : action.getOpenTime();

        return time;
    }

    /**
     * Sets how long it takes for the firearm action to be completed.
     *
     * @param time The firearm action time.
     */
    public void setTime(int time) {
        this.time = time;
    }

    public void useMechanics(CastData castData, boolean isOpen) {
        if (isOpen) {
            if (mechanics != null)
                mechanics.use(castData);
            else if (action.getOpen() != null)
                action.getOpen().use(castData);
        } else {
            if (mechanics != null)
                mechanics.use(castData);
            else if (action.getClose() != null)
                action.getClose().use(castData);
        }
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
