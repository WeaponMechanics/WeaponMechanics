package me.deecaad.weaponmechanics.weapon.damage

import me.deecaad.weaponmechanics.weapon.explode.Explosion
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack

class ExplosionDamageSource(
    val explosion: Explosion,
    override val shooter: LivingEntity?,
    override val weaponTitle: String,
    override val weaponStack: ItemStack?,
    override val damageLocation: Location,
) : WeaponDamageSource() {
    override val damageType: WeaponDamageType
        get() = WeaponDamageType.EXPLOSION

    override val damagePoint: DamagePoint?
        get() = null // Explosions don't have a specific damage point

    override val effectedEquipment: EquipmentSlotGroup?
        get() = EquipmentSlotGroup.ARMOR // All armor is effected by explosions
}
