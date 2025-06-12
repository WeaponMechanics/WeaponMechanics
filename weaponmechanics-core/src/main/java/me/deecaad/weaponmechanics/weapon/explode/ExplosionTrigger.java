package me.deecaad.weaponmechanics.weapon.explode;

public enum ExplosionTrigger {

    /**
     * When the projectile is shot/thrown
     */
    SPAWN,

    /**
     * When the projectile hits a non-air and non-liquid block
     */
    BLOCK,

    /**
     * When the projectile hits an entity
     */
    ENTITY,
}