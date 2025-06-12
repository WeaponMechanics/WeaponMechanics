package me.deecaad.weaponmechanics.weapon.damage;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.simple.DoubleSerializer;
import me.deecaad.core.file.simple.RegistryValueSerializer;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A class that holds all the damage modifiers that can be applied to a victim. This includes armor
 * modifiers, damage point modifiers, movement modifiers, and more. Each modifier is a percentage
 * that is added to the damage dealt to the victim. The final damage is clamped between
 * {@link #getMin()} and {@link #getMax()}.
 */
public class DamageModifier implements Serializer<DamageModifier> {

    // For clamping bounds
    private double min;
    private double max;

    // Armor modifiers
    private double perArmorPoint;
    private Object2DoubleMap<ItemType> armorModifiers;
    private Object2DoubleMap<Enchantment> enchantmentModifiers;

    // DamagePoint modifiers
    private double headModifier;
    private double bodyModifier;
    private double armsModifier;
    private double legsModifier;
    private double feetModifier;
    private double backModifier;

    // Movement modifiers
    private double sneakingModifier;
    private double walkingModifier;
    private double swimmingModifier;
    private double sprintingModifier;
    private double inMidairModifier;

    // Misc modifiers
    private double shieldModifier;
    private Object2DoubleMap<EntityType> entityTypeModifiers;
    private Object2DoubleMap<PotionEffectType> potionEffectModifiers;

    /**
     * Default constructor for serializer
     */
    public DamageModifier() {
    }

    public DamageModifier(double min, double max, double perArmorPoint, Object2DoubleMap<ItemType> armorModifiers, Object2DoubleMap<Enchantment> enchantmentModifiers,
        double headModifier, double bodyModifier, double armsModifier, double legsModifier, double feetModifier, double backModifier,
        double sneakingModifier, double walkingModifier, double swimmingModifier, double sprintingModifier, double inMidairModifier,
        double shieldModifier, Object2DoubleMap<EntityType> entityTypeModifiers, Object2DoubleMap<PotionEffectType> potionEffectModifiers) {
        this.min = min;
        this.max = max;
        this.perArmorPoint = perArmorPoint;
        this.armorModifiers = armorModifiers;
        this.enchantmentModifiers = enchantmentModifiers;
        this.headModifier = headModifier;
        this.bodyModifier = bodyModifier;
        this.armsModifier = armsModifier;
        this.legsModifier = legsModifier;
        this.feetModifier = feetModifier;
        this.backModifier = backModifier;
        this.sneakingModifier = sneakingModifier;
        this.walkingModifier = walkingModifier;
        this.swimmingModifier = swimmingModifier;
        this.sprintingModifier = sprintingModifier;
        this.inMidairModifier = inMidairModifier;
        this.shieldModifier = shieldModifier;
        this.entityTypeModifiers = entityTypeModifiers;
        this.potionEffectModifiers = potionEffectModifiers;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getPerArmorPoint() {
        return perArmorPoint;
    }

    public void setPerArmorPoint(double perArmorPoint) {
        this.perArmorPoint = perArmorPoint;
    }

    public Object2DoubleMap<ItemType> getArmorModifiers() {
        return armorModifiers;
    }

    public void setArmorModifiers(Object2DoubleMap<ItemType> armorModifiers) {
        this.armorModifiers = armorModifiers;
    }

    public Object2DoubleMap<Enchantment> getEnchantmentModifiers() {
        return enchantmentModifiers;
    }

    public void setEnchantmentModifiers(Object2DoubleMap<Enchantment> enchantmentModifiers) {
        this.enchantmentModifiers = enchantmentModifiers;
    }

    public double getHeadModifier() {
        return headModifier;
    }

    public void setHeadModifier(double headModifier) {
        this.headModifier = headModifier;
    }

    public double getBodyModifier() {
        return bodyModifier;
    }

    public void setBodyModifier(double bodyModifier) {
        this.bodyModifier = bodyModifier;
    }

    public double getArmsModifier() {
        return armsModifier;
    }

    public void setArmsModifier(double armsModifier) {
        this.armsModifier = armsModifier;
    }

    public double getLegsModifier() {
        return legsModifier;
    }

    public void setLegsModifier(double legsModifier) {
        this.legsModifier = legsModifier;
    }

    public double getFeetModifier() {
        return feetModifier;
    }

    public void setFeetModifier(double feetModifier) {
        this.feetModifier = feetModifier;
    }

    public double getBackModifier() {
        return backModifier;
    }

    public void setBackModifier(double backModifier) {
        this.backModifier = backModifier;
    }

    public double getSneakingModifier() {
        return sneakingModifier;
    }

    public void setSneakingModifier(double sneakingModifier) {
        this.sneakingModifier = sneakingModifier;
    }

    public double getWalkingModifier() {
        return walkingModifier;
    }

    public void setWalkingModifier(double walkingModifier) {
        this.walkingModifier = walkingModifier;
    }

    public double getSwimmingModifier() {
        return swimmingModifier;
    }

    public void setSwimmingModifier(double swimmingModifier) {
        this.swimmingModifier = swimmingModifier;
    }

    public double getSprintingModifier() {
        return sprintingModifier;
    }

    public void setSprintingModifier(double sprintingModifier) {
        this.sprintingModifier = sprintingModifier;
    }

    public double getInMidairModifier() {
        return inMidairModifier;
    }

    public void setInMidairModifier(double inMidairModifier) {
        this.inMidairModifier = inMidairModifier;
    }

    public Object2DoubleMap<EntityType> getEntityTypeModifiers() {
        return entityTypeModifiers;
    }

    public void setEntityTypeModifiers(Object2DoubleMap<EntityType> entityTypeModifiers) {
        this.entityTypeModifiers = entityTypeModifiers;
    }

    public Object2DoubleMap<PotionEffectType> getPotionEffectModifiers() {
        return potionEffectModifiers;
    }

    public void setPotionEffectModifiers(Object2DoubleMap<PotionEffectType> potionEffectModifiers) {
        this.potionEffectModifiers = potionEffectModifiers;
    }

    /**
     * Accumulates each rate that should be applied to the damage. The result is clamped between
     * {@link #getMin()} and {@link #getMax()}.
     *
     * @param wrapper The victim being damaged.
     * @param point Where the victim was hit.
     * @param isBackStab If the hit came from behind.
     * @return The clamped rate to multiply damage by. Defaults to 1.0.
     */
    public double getRate(@NotNull EntityWrapper wrapper, @Nullable DamagePoint point, boolean isBackStab) {
        LivingEntity victim = wrapper.getEntity();
        double rate = 1.0;

        // 95% of weapons should use this, since it is an "all encompassing" value.
        // Since armor may have custom amounts of protection, this attribute covers that.
        AttributeInstance armorAttribute = victim.getAttribute(Attribute.ARMOR);
        if (armorAttribute != null) {
            rate += perArmorPoint * armorAttribute.getValue();
        }

        // If admins want diamond armor to be invulnerable to damage, this is where they do it.
        // This is also where we check enchantment damage contributions
        EntityEquipment equipment = victim.getEquipment();
        if ((enchantmentModifiers != null || armorModifiers != null) && equipment != null) {
            for (ItemStack armor : equipment.getArmorContents()) {
                if (armor == null)
                    continue;

                // Armor type (DIAMOND, CHAIN, GOLD, etc.)
                if (armorModifiers != null)
                    rate += armorModifiers.getDouble(armor.getType().asItemType());

                // Enchantments (PROTECTION 4, PROJECTILE PROJECTION, etc)
                if (enchantmentModifiers != null) {
                    for (Map.Entry<Enchantment, Integer> entry : armor.getEnchantments().entrySet()) {
                        rate += enchantmentModifiers.getDouble(entry.getKey()) * entry.getValue();
                    }
                }
            }
        }

        // Headshots do more damage than feet shots
        if (point != null) {
            rate += switch (point) {
                case HEAD -> headModifier;
                case BODY -> bodyModifier;
                case ARMS -> armsModifier;
                case LEGS -> legsModifier;
                case FEET -> feetModifier;
            };
        }

        // Backstabs hurt more
        if (isBackStab)
            rate += backModifier;

        // Running targets take more damage than stationary "braced" targets
        if (wrapper.isSneaking())
            rate += sneakingModifier;
        if (wrapper.isWalking())
            rate += walkingModifier;
        if (wrapper.isSwimming())
            rate += swimmingModifier;
        if (wrapper.isSprinting())
            rate += sprintingModifier;
        if (wrapper.isInMidair())
            rate += inMidairModifier;

        // For the shield modifier, the player must be blocking with a shield
        // and be facing the bullet.
        if (!isBackStab && wrapper.getEntity() instanceof Player player && player.isBlocking())
            rate += shieldModifier;

        // Do double damage to zombies, half damage to players (PVE scenario), for example
        if (entityTypeModifiers != null) {
            rate += entityTypeModifiers.getDouble(victim.getType());
        }

        // Let potion effects contribute to the damage dealt
        if (potionEffectModifiers != null) {
            for (Object2DoubleMap.Entry<PotionEffectType> entry : potionEffectModifiers.object2DoubleEntrySet()) {
                if (victim.hasPotionEffect(entry.getKey()))
                    rate += entry.getDoubleValue();
            }
        }

        // Clamp the rate within bounds
        return NumberUtil.clamp(rate, min, max);
    }

    /**
     * Clamps the given rate between {@link #getMin()} and {@link #getMax()}.
     *
     * @param rate The rate to clamp.
     * @return The clamped rate.
     */
    public double clamp(double rate) {
        return NumberUtil.clamp(rate, min, max);
    }

    /**
     * Applies all rates from this damage modifier to the given damage. This is equivalent to
     * multiplying the damage by {@link #getRate(EntityWrapper, DamagePoint, boolean)}.
     *
     * @param damage The damage to apply rates to.
     * @param wrapper The victim being damaged.
     * @param point Where the victim was hit.
     * @param isBackStab If the hit came from behind.
     * @return The damage multiplied by the rate.
     */
    public double applyRates(double damage, EntityWrapper wrapper, DamagePoint point, boolean isBackStab) {
        return damage * getRate(wrapper, point, isBackStab);
    }

    @Override
    public String getKeyword() {
        return "Damage_Modifiers";
    }

    @NotNull @Override
    public DamageModifier serialize(@NotNull SerializeData data) throws SerializerException {
        double min = data.of("Min").assertRange(0.0, null).getDouble().orElse(0.20);
        double max = data.of("Max").assertRange(min, null).getDouble().orElse(10.00);

        double perArmorPoint = data.of("Per_Armor_Point").getDouble().orElse(0.0);

        // Per material armor modifiers
        Object2DoubleMap<ItemType> armorModifiers = new Object2DoubleOpenHashMap<>();
        List<List<Optional<Object>>> armorSplitList = data.ofList("Armor")
            .addArgument(new RegistryValueSerializer<>(ItemType.class, true))
            .addArgument(new DoubleSerializer())
            .requireAllPreviousArgs()
            .assertList();

        for (List<Optional<Object>> split : armorSplitList) {
            List<ItemType> armorMaterial = (List<ItemType>) split.get(0).get();
            double percentage = (double) split.get(1).get();

            for (ItemType armor : armorMaterial) {
                armorModifiers.put(armor, percentage);
            }
        }

        // Per enchantment armor modifiers
        Object2DoubleMap<Enchantment> enchantmentModifiers = new Object2DoubleOpenHashMap<>();
        List<List<Optional<Object>>> enchantmentSplitList = data.ofList("Enchantments")
            .addArgument(new RegistryValueSerializer<>(Enchantment.class, true))
            .addArgument(new DoubleSerializer())
            .requireAllPreviousArgs()
            .assertList();

        for (List<Optional<Object>> split : enchantmentSplitList) {
            List<Enchantment> enchantments = (List<Enchantment>) split.get(0).get();
            double percentage = (double) split.get(1).get();

            for (Enchantment enchantment : enchantments) {
                enchantmentModifiers.put(enchantment, percentage);
            }
        }

        double headModifier = data.of("Head").getDouble().orElse(0.0);
        double bodyModifier = data.of("Body").getDouble().orElse(0.0);
        double armsModifier = data.of("Arms").getDouble().orElse(0.0);
        double legsModifier = data.of("Legs").getDouble().orElse(0.0);
        double feetModifier = data.of("Feet").getDouble().orElse(0.0);
        double backModifier = data.of("Back").getDouble().orElse(0.0);

        double sneakingModifier = data.of("Sneaking").getDouble().orElse(0.0);
        double walkingModifier = data.of("Walking").getDouble().orElse(0.0);
        double swimmingModifier = data.of("Swimming").getDouble().orElse(0.0);
        double sprintingModifier = data.of("Sprinting").getDouble().orElse(0.0);
        double inMidairModifier = data.of("In_Midair").getDouble().orElse(0.0);

        double shieldModifier = data.of("Shielding").getDouble().orElse(0.0);

        Object2DoubleMap<EntityType> entityTypeModifiers = new Object2DoubleOpenHashMap<>();
        List<List<Optional<Object>>> entitySplitList = data.ofList("Entities")
            .addArgument(new RegistryValueSerializer<>(EntityType.class, true))
            .addArgument(new DoubleSerializer())
            .requireAllPreviousArgs()
            .assertList();

        for (List<Optional<Object>> split : entitySplitList) {
            List<EntityType> entityTypes = (List<EntityType>) split.get(0).get();
            double percentage = (double) split.get(1).get();

            for (EntityType entity : entityTypes) {
                entityTypeModifiers.put(entity, percentage);
            }
        }

        Object2DoubleMap<PotionEffectType> potionEffectModifiers = new Object2DoubleOpenHashMap<>();
        List<List<Optional<Object>>> potionSplitList = data.ofList("Potions")
            .addArgument(new RegistryValueSerializer<>(PotionEffectType.class, true))
            .addArgument(new DoubleSerializer())
            .requireAllPreviousArgs()
            .assertList();
        for (List<Optional<Object>> split : potionSplitList) {
            List<PotionEffectType> potions = (List<PotionEffectType>) split.get(0).get();
            double percentage = (double) split.get(1).get();

            for (PotionEffectType potion : potions) {
                potionEffectModifiers.put(potion, percentage);
            }
        }

        return new DamageModifier(min, max, perArmorPoint, armorModifiers, enchantmentModifiers, headModifier, bodyModifier, armsModifier, legsModifier, feetModifier, backModifier,
            sneakingModifier, walkingModifier, swimmingModifier, sprintingModifier, inMidairModifier, shieldModifier, entityTypeModifiers, potionEffectModifiers);
    }
}
