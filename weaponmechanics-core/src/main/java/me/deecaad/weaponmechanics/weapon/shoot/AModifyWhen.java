package me.deecaad.weaponmechanics.weapon.shoot;

import me.deecaad.core.file.Serializer;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;

public abstract class AModifyWhen implements Serializer<AModifyWhen> {

    private NumberModifier always;
    private NumberModifier zooming;
    private NumberModifier sneaking;
    private NumberModifier crawling;
    private NumberModifier standing;
    private NumberModifier walking;
    private NumberModifier riding;
    private NumberModifier sprinting;
    private NumberModifier dualWielding;
    private NumberModifier swimming;
    private NumberModifier inMidair;
    private NumberModifier gliding;
    /**
     * Default constructor for serializer.
     */
    public AModifyWhen() {
    }

    public AModifyWhen(NumberModifier always, NumberModifier zooming, NumberModifier sneaking,
        NumberModifier crawling, NumberModifier standing, NumberModifier walking,
        NumberModifier riding, NumberModifier sprinting, NumberModifier dualWielding,
        NumberModifier swimming, NumberModifier inMidair, NumberModifier gliding) {
        this.always = always;
        this.zooming = zooming;
        this.sneaking = sneaking;
        this.crawling = crawling;
        this.standing = standing;
        this.walking = walking;
        this.riding = riding;
        this.sprinting = sprinting;
        this.dualWielding = dualWielding;
        this.swimming = swimming;
        this.inMidair = inMidair;
        this.gliding = gliding;
    }

    /**
     * Applies all changes from this modifier to given number
     *
     * @param entityWrapper the entity wrapper used to check circumstances
     * @param tempNumber the number
     * @return the modified number
     */
    public double applyChanges(EntityWrapper entityWrapper, double tempNumber) {
        if (always != null) {
            tempNumber = always.applyTo(tempNumber);
        }
        if (zooming != null && (entityWrapper.getMainHandData().getZoomData().isZooming() || entityWrapper.getOffHandData().getZoomData().isZooming())) {
            tempNumber = zooming.applyTo(tempNumber);
        }
        if (sneaking != null && entityWrapper.isSneaking()) {
            tempNumber = sneaking.applyTo(tempNumber);
        }
        if (entityWrapper instanceof PlayerWrapper playerWrapper) {
            if (crawling != null && playerWrapper.isCrawling()) {
                tempNumber = crawling.applyTo(tempNumber);
            }
        }
        if (standing != null && entityWrapper.isStanding()) {
            tempNumber = standing.applyTo(tempNumber);
        }
        if (walking != null && entityWrapper.isWalking()) {
            tempNumber = walking.applyTo(tempNumber);
        }
        if (riding != null && entityWrapper.isRiding()) {
            tempNumber = riding.applyTo(tempNumber);
        }
        if (sprinting != null && entityWrapper.isSprinting()) {
            tempNumber = sprinting.applyTo(tempNumber);
        }
        if (dualWielding != null && entityWrapper.isDualWielding()) {
            tempNumber = dualWielding.applyTo(tempNumber);
        }
        if (swimming != null && entityWrapper.isSwimming()) {
            tempNumber = swimming.applyTo(tempNumber);
        }
        if (inMidair != null && entityWrapper.isInMidair()) {
            tempNumber = inMidair.applyTo(tempNumber);
        }
        if (gliding != null && entityWrapper.isGliding()) {
            tempNumber = gliding.applyTo(tempNumber);
        }

        return tempNumber;
    }
}
