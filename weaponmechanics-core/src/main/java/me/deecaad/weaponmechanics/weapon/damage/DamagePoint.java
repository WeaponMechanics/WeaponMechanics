package me.deecaad.weaponmechanics.weapon.damage;

import me.deecaad.core.utils.StringUtil;

/**
 * This class highlights different points in a hit box. Basically having child hit boxes inside of
 * parent hit boxes
 * <p>
 * Back-stabs are not in here because a back-stab may also be head-shot, or a leg shot. It makes
 * more sense for back-stabs to be handled outside of this class because
 * <p>
 * Note that the rules defined here are fairly generalized. Entities that are horizontal (Like
 * sheep, cows, etc.) are going to have slightly different rules than vertical mobs.
 */
public enum DamagePoint {

    /**
     * If the damage is hitting head
     */
    HEAD,

    /**
     * If the damage is hitting body
     */
    BODY,

    /**
     * If the damage is hitting body AND the damage came from the side (35 degrees from the side
     * maximum)
     */
    ARMS,

    /**
     * If the damage is hitting legs
     */
    LEGS,

    /**
     * If the damage is hitting feet
     */
    FEET;

    private final String readable;

    DamagePoint() {
        readable = StringUtil.snakeToReadable(name());
    }

    public String getReadable() {
        return readable;
    }
}
