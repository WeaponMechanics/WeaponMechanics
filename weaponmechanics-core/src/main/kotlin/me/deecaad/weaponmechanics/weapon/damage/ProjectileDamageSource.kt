package me.deecaad.weaponmechanics.weapon.damage

import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

class ProjectileDamageSource(
    val projectile: WeaponProjectile,
    override val damagePoint: DamagePoint?,
) : WeaponDamageSource() {
    override val damageType: WeaponDamageType
        get() = WeaponDamageType.PROJECTILE

    override val shooter: LivingEntity?
        get() = projectile.shooter

    override val weaponTitle: String
        get() = projectile.weaponTitle

    override val weaponStack: ItemStack?
        get() = projectile.weaponStack
}
