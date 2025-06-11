package me.deecaad.weaponmechanics.weapon.firearm;

/**
 * This class highlights the different types a Firearm can have.
 * <p>
 * So there are 2 different firearm actions, OPEN and CLOSE. These actions are triggered based on
 * weapon shooting and reloading. Comments on each type describe when the firearm opens and closes
 * based on those 2 triggers.
 * <p>
 * Some types also have aliases, <code>REVOLVER</code> and <code>BREAK</code>, for example. This is
 * because these types are called different things by different people.
 */
public enum FirearmType {

    /**
     * FirearmType for revolvers Shoot: Bang Reload: Open, then reload timer, then close Open: Empty
     * shells fall out Close: The block and spin of barrel
     */
    REVOLVER,

    /**
     * FirearmType for pump shotguns Shoot: Bang, then open, then close Reload: If Empty: Reload timer,
     * then open, then close Else: Reload timer Open: Pull the pump Close: Push the pump
     */
    PUMP,

    /**
     * FirearmType for bolt action and lever action rifles Shoot: Bang, then open, (bullet goes in),
     * then close Reload: If Empty: Open, reload timer, then close Else: Reload timer Open: Pull lever
     * back Close: Push lever forward
     */
    LEVER,

    /**
     * FirearmType Shoot: Bang Reload: If Empty: Open, then reload timer, then close Else: Reload timer
     * Open: Pull slide back Close: Slide returns, a bullet is chambered
     */
    SLIDE;

    public boolean hasShootActions() {
        return this == PUMP || this == LEVER;
    }
}
