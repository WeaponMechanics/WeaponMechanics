package me.deecaad.weaponmechanics.weapon.shoot;

/**
 * Simple class to help with handling spread and recoil change
 */
public class NumberModifier {

    private final double number;
    private final boolean percentage;

    public NumberModifier(double number, boolean percentage) {
        this.percentage = percentage;

        if (percentage) {
            // Convert from 100% to 1.0 format
            // Add 1 to spread to make it so that
            // -> 50% actually decreases 50% and 130% actually increases 30%
            // -> 100% would then mean that no changes are made
            this.number = number * 0.01;
            return;
        }
        this.number = number;
    }

    public double applyTo(double currentNumber) {
        return percentage ? currentNumber * this.number : currentNumber + this.number;
    }
}