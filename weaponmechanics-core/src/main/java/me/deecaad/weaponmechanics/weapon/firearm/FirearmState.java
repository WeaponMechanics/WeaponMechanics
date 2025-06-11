package me.deecaad.weaponmechanics.weapon.firearm;

public enum FirearmState {

    READY(0),
    OPEN(1),
    CLOSE(2);

    private final int id;

    FirearmState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}