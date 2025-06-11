package me.deecaad.weaponmechanics.weapon.damage

import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack

class MeleeDamageSource(
    override val shooter: LivingEntity,
    override val weaponTitle: String,
    override val weaponStack: ItemStack,
    override val damagePoint: DamagePoint?,
    val isBackStab: Boolean,
) : WeaponDamageSource() {
    override val damageType: WeaponDamageType
        get() = WeaponDamageType.MELEE
}
