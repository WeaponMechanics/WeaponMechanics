package me.deecaad.weaponmechanics.weapon.shoot.spread;

/**
 * This class encapsulates an (x, y) point with a value.
 */
public class Point {

    private double yaw;
    private double pitch;

    public Point(double yaw, double pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }
}
