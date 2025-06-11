package me.deecaad.weaponmechanics.weapon.damage

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack

/**
 * The source of damage that caused a living entity to take damage.
 */
abstract class WeaponDamageSource {
    /**
     * The type of damage (e.g. melee, projectile, explosion).
     */
    abstract val damageType: WeaponDamageType

    /**
     * The entity that caused the damage.
     */
    abstract val shooter: LivingEntity?

    /**
     * The title of the weapon that caused the damage.
     */
    abstract val weaponTitle: String

    /**
     * The weapon item that launched the projectile, if any.
     */
    abstract val weaponStack: ItemStack?

    /**
     * The specific "body part" that was hit, if any.
     */
    abstract val damagePoint: DamagePoint?

    /**
     * The location of where the damage came from, like the origin of an explosion,
     * or the shooter's location.
     */
    open val damageLocation: Location?
        get() = shooter?.eyeLocation

    /**
     * The equipment slot group that was hit.
     */
    open val effectedEquipment: EquipmentSlotGroup?
        get() =
            when (damagePoint) {
                DamagePoint.HEAD -> EquipmentSlotGroup.HEAD
                DamagePoint.BODY, DamagePoint.ARMS -> EquipmentSlotGroup.CHEST
                DamagePoint.LEGS -> EquipmentSlotGroup.LEGS
                DamagePoint.FEET -> EquipmentSlotGroup.FEET
                null -> null
            }
}
