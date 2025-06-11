package me.deecaad.weaponmechanics.weapon.explode;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.entity.FakeEntity;
import me.deecaad.core.compatibility.worldguard.WorldGuardCompatibility;
import me.deecaad.core.file.*;
import me.deecaad.core.file.serializers.ChanceSerializer;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.core.utils.VectorUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.damage.BlockDamageData;
import me.deecaad.weaponmechanics.weapon.explode.exposures.DefaultExposure;
import me.deecaad.weaponmechanics.weapon.explode.exposures.ExplosionExposure;
import me.deecaad.weaponmechanics.weapon.explode.exposures.ExplosionExposures;
import me.deecaad.weaponmechanics.weapon.explode.regeneration.BlockRegenSorter;
import me.deecaad.weaponmechanics.weapon.explode.regeneration.LayerDistanceSorter;
import me.deecaad.weaponmechanics.weapon.explode.regeneration.RegenerationData;
import me.deecaad.weaponmechanics.weapon.explode.shapes.DefaultExplosion;
import me.deecaad.weaponmechanics.weapon.explode.shapes.ExplosionShape;
import me.deecaad.weaponmechanics.weapon.explode.shapes.ExplosionShapes;
import me.deecaad.weaponmechanics.weapon.projectile.RemoveOnBlockCollisionProjectile;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import me.deecaad.weaponmechanics.weapon.stats.WeaponStat;
import me.deecaad.weaponmechanics.weapon.weaponevents.ProjectileExplodeEvent;
import me.deecaad.weaponmechanics.weapon.weaponevents.ProjectilePreExplodeEvent;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.PlayerWrapper;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;


@SearcherFilter(SearchMode.ON_DEMAND)
public class Explosion implements Serializer<Explosion> {

    private ExplosionShape shape;
    private ExplosionExposure exposure;
    private BlockDamage blockDamage;
    private RegenerationData regeneration;
    private Detonation detonation;
    private double blockChance;
    private double knockbackRate;
    private ClusterBomb cluster;
    private AirStrike airStrike;
    private Flashbang flashbang;
    private MechanicManager mechanics;

    /**
     * Default constructor for serializer.
     */
    public Explosion() {
    }

    /**
     * The main constructor for explosions. See parameters.
     *
     * @param shape The non-null shape that determines the pattern in which all blocks are destroyed.
     * @param exposure The non-null method to determine how exposed each entity is to the origin of this
     *        explosion.
     * @param blockDamage The nullable data to determine how each block is damaged. If null is used,
     *        blocks will not be damaged.
     * @param regeneration The nullable data to determine how blocks are regenerated after being broken
     *        by {@link BlockDamage}.
     * @param detonation The object containing information about when explosion should detonate.
     * @param blockChance The chance [0, 1] for block from {@link BlockDamage} to spawn a packet based
     *        falling block.
     * @param knockbackRate Use true to enable vanilla MC explosion knockback.
     * @param clusterBomb The nullable cluster bomb (Children explosions).
     * @param airStrike The nullable airstrike (Explosions from the air).
     * @param flashbang The nullable flashbang (To blind players).
     * @param mechanics The nullable mechanics, spawned at the origin of the explosion.
     */
    public Explosion(ExplosionShape shape, ExplosionExposure exposure, BlockDamage blockDamage,
        RegenerationData regeneration, Detonation detonation, double blockChance, double knockbackRate,
        ClusterBomb clusterBomb, AirStrike airStrike, Flashbang flashbang, MechanicManager mechanics) {

        this.shape = shape;
        this.exposure = exposure;
        this.blockDamage = blockDamage;
        this.regeneration = regeneration;
        this.detonation = detonation;
        this.blockChance = blockChance;
        this.knockbackRate = knockbackRate;
        this.cluster = clusterBomb;
        this.airStrike = airStrike;
        this.flashbang = flashbang;
        this.mechanics = mechanics;
    }

    public ExplosionShape getShape() {
        return shape;
    }

    public ExplosionExposure getExposure() {
        return exposure;
    }

    public BlockDamage getBlockDamage() {
        return blockDamage;
    }

    public RegenerationData getRegeneration() {
        return regeneration;
    }

    public Detonation getDetonation() {
        return detonation;
    }

    public double getBlockChance() {
        return blockChance;
    }

    public boolean isKnockback() {
        return knockbackRate != 0.0;
    }

    public double getKnockbackRate() {
        return knockbackRate;
    }

    public ClusterBomb getCluster() {
        return cluster;
    }

    public AirStrike getAirStrike() {
        return airStrike;
    }

    public Flashbang getFlashbang() {
        return flashbang;
    }

    public MechanicManager getMechanics() {
        return mechanics;
    }

    public void handleExplosion(LivingEntity cause, WeaponProjectile projectile, ExplosionTrigger trigger) {
        handleExplosion(cause, null, projectile, trigger);
    }

    public void handleExplosion(LivingEntity cause, @Nullable Location origin, WeaponProjectile projectile, ExplosionTrigger trigger) {
        if (projectile.getIntTag("explosion-detonation") == 1)
            return;

        Detonation currentDetonation;
        if (airStrike != null && airStrike.getDetonation() != null && projectile.getIntTag("airstrike-bomb") == 1) {
            // Only use airstrike's own detonation on its bombs
            currentDetonation = airStrike.getDetonation();
        } else if (cluster != null && cluster.getDetonation() != null && projectile.getIntTag("cluster-split-level") >= 1) {
            // Only use cluster bomb's own detonation on its bombs
            currentDetonation = cluster.getDetonation();
        } else {
            // Otherwise use default detonation
            currentDetonation = detonation;
        }

        if (!currentDetonation.getTriggers().contains(trigger))
            return;

        // Set to 1 to indicate that this projectile has been detonated
        projectile.setIntTag("explosion-detonation", 1);

        WeaponMechanics.getInstance().getFoliaScheduler().region(projectile.getBukkitLocation()).runDelayed(() -> {
            ProjectilePreExplodeEvent event = new ProjectilePreExplodeEvent(projectile, Explosion.this);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return;

            event.getExplosion().explode(cause, origin != null ? origin : projectile.getLocation().toLocation(projectile.getWorld()), projectile);

            if (currentDetonation.isRemoveProjectileOnDetonation()) {
                projectile.remove();
            }
        }, currentDetonation.getDelay());
    }

    public void explode(LivingEntity cause, Location origin, WeaponProjectile projectile) {

        // Grab these once
        Configuration config = WeaponMechanics.getInstance().getConfiguration();
        MechanicsLogger debugger = WeaponMechanics.getInstance().getDebugger();

        // Handle worldguard flags
        WorldGuardCompatibility worldGuard = CompatibilityAPI.getWorldGuardCompatibility();
        EntityWrapper entityWrapper = WeaponMechanics.getInstance().getEntityWrapper(cause);
        PlayerWrapper playerWrapper = cause.getType() == EntityType.PLAYER ? (PlayerWrapper) entityWrapper : null;
        if (!worldGuard.testFlag(origin, playerWrapper != null ? playerWrapper.getPlayer() : null, "weapon-explode")) {
            Object obj = worldGuard.getValue(origin, "weapon-explode-message");
            if (obj != null && !obj.toString().isEmpty()) {
                Component component = MiniMessage.miniMessage().deserialize(obj.toString());
                Audience audience = WeaponMechanics.getInstance().getAdventure().sender(entityWrapper.getEntity());
                audience.sendMessage(component);
            }
            return;
        }

        // If the projectile is rolling or sticked to block
        // -> add 0.5 Y to ensure the explosion doesn't happen slightly inside the block
        if (projectile != null && (projectile.isRolling() || projectile.getStickedData() != null)) {
            origin.add(0.0, 0.5, 0.0);
        }

        // If the projectile uses airstrikes, then the airstrike should be
        // triggered instead of the explosion.
        if (projectile != null && airStrike != null && projectile.getIntTag("airstrike-bomb") == 0) {
            airStrike.trigger(origin, cause, projectile);
            return;
        }

        List<Block> blocks = shape.getBlocks(origin);
        BlockRegenSorter sorter = new LayerDistanceSorter(origin, this);
        Object2DoubleMap<LivingEntity> entities = exposure.mapExposures(origin, shape);
        MechanicManager mechanics = this.mechanics;
        if (projectile != null) {
            // This event is not cancellable. If developers want to cancel
            // explosions, they should use ProjectilePreExplodeEvent
            ProjectileExplodeEvent event = new ProjectileExplodeEvent(projectile, blocks, sorter, entities, mechanics);
            Bukkit.getPluginManager().callEvent(event);
            blocks = event.getBlocks();
            sorter = event.getSorter();
            entities = event.getEntities();
            mechanics = event.getMechanics();

            // Use Bukkit's EntityExplodeEvent to allow other protection plugins
            // (Towny, for example) to cancel the explosion or filter blocks w/o
            // explicitly depending on WeaponMechanics.
            if (blockDamage != null && !blocks.isEmpty() && !WeaponMechanics.getInstance().getConfiguration().getBoolean("Disable_Entity_Explode_Event")) {
                EntityExplodeEvent entityExplodeEvent = new EntityExplodeEvent(cause, origin, blocks, 5, ExplosionResult.DESTROY);
                Bukkit.getPluginManager().callEvent(entityExplodeEvent);
                if (entityExplodeEvent.isCancelled())
                    return;
            }
        }

        // Sort the blocks into different categories (To make regeneration more
        // reliable). In the future, this may also be used to filter out
        // redstone contraptions.
        List<Block> transparent = new ArrayList<>(blocks.size());
        List<Block> solid = new ArrayList<>(blocks.size());
        for (Block block : blocks) {
            if (block.getType().isSolid()) {
                solid.add(block);
            } else if (!block.isEmpty()) {
                transparent.add(block);
            }
        }

        // Sorting the blocks is crucial to making block regeneration look
        // good. Generally, sorters should generate lower blocks before higher
        // blocks, and outer blocks before inner blocks. If the sorter is null,
        // the sorting stage will be skipped, but this will cause blocks to
        // regenerate in an undefined order.
        try {
            if (sorter == null) {
                debugger.warning("Null sorter used while regenerating explosion... Was this intentional?");
            } else {
                solid.sort(sorter);
            }
        } catch (IllegalArgumentException e) {
            debugger.severe("A plugin modified the explosion block sorter with an illegal sorter! " +
                "Please report this error to the developers of that plugin. Sorter: " + sorter, e);
        }

        // When blockDamage is null, we should not attempt to damage blocks or
        // spawn falling blocks. We also don't need to worry about regeneration.
        if (blockDamage != null) {
            int timeOffset = regeneration == null ? -1 : (solid.size() * regeneration.getInterval() / regeneration.getMaxBlocksPerUpdate());

            damageBlocks(transparent, true, origin, timeOffset, playerWrapper, projectile);
            damageBlocks(solid, false, origin, 0, playerWrapper, projectile);
        }

        if (projectile != null && projectile.getWeaponTitle() != null) {
            WeaponMechanics.getInstance().getWeaponHandler().getDamageHandler().tryUseExplosion(this, projectile, origin, entities);

            // isKnockback will cause vanilla-like explosion knockback. The
            // higher your exposure, the greater the knockback.
            if (isKnockback()) {
                Vector originVector = origin.toVector();
                entities.forEach((entity, exposure) -> {
                    exposure *= knockbackRate;

                    // Normalized vector between the explosion and entity involved
                    Vector between = VectorUtil.setLength(entity.getLocation().toVector().subtract(originVector), exposure);
                    Vector motion = entity.getVelocity().add(between);

                    entity.setVelocity(motion);
                });
            }

            if (cluster != null)
                cluster.trigger(projectile, cause, origin);

        } else {

            // This occurs because of the command /wm test
            // Useful for debugging, and can help users decide which
            // size explosion they may want
            entities.forEach((entity, impact) -> {
                entity.sendMessage(ChatColor.RED + "You suffered " + impact * 100 + "% of the impact");
            });
        }

        if (flashbang != null)
            flashbang.trigger(exposure, projectile, origin);
        if (mechanics != null) { // NOT this.mechanics for event
            CastData cast = new CastData(cause, projectile == null ? null : projectile.getWeaponTitle(), projectile == null ? null : projectile.getWeaponStack());
            cast.setTargetLocation(origin);
            mechanics.use(cast);
        }
    }

    protected void damageBlocks(List<Block> blocks, boolean isAtOnce, Location origin, int timeOffset, PlayerWrapper playerWrapper, WeaponProjectile projectile) {
        boolean isRegenerate = regeneration != null;

        if (isRegenerate)
            timeOffset += regeneration.getTicksBeforeStart();

        List<BlockDamageData.DamageData> brokenBlocks = isRegenerate ? new ArrayList<>(regeneration.getMaxBlocksPerUpdate()) : null;
        Location temp = new Location(null, 0, 0, 0);

        int blocksBroken = 0;

        int size = blocks.size();
        for (int i = 0; i < size; i++) {
            Block block = blocks.get(i);

            // We need the BlockState for falling blocks. If we get the state
            // after breaking the block, we will get AIR (not good for visual
            // effects).
            BlockState state = block.getState();

            // ALWAYS give null player on explosions so BlockBreakEvent isn't called.
            // Explosions already call EntityExplodeEvent. Single block breaks should
            // only call the BlockBreakEvent
            BlockDamageData.DamageData data = blockDamage.damage(block, null, isRegenerate);

            // This happens when a block is blacklisted or block break was cancelled
            if (data == null)
                continue;

            // Group blocks together to reduce task scheduling. After
            // reaching the bound, we can schedule a task to generate later.
            if (isRegenerate) {
                brokenBlocks.add(data);

                if (brokenBlocks.size() == regeneration.getMaxBlocksPerUpdate() || i == size - 1) {
                    int time = timeOffset + ((isAtOnce ? size : i) / regeneration.getMaxBlocksPerUpdate() * regeneration.getInterval());

                    List<BlockDamageData.DamageData> finalBrokenBlocks = new ArrayList<>(brokenBlocks);
                    WeaponMechanics.getInstance().getFoliaScheduler().region(block).runDelayed(() -> {
                        for (BlockDamageData.DamageData blockWrapper : finalBrokenBlocks) {

                            // The blocks may have been regenerated already
                            if (blockWrapper.isBroken()) {
                                blockWrapper.regenerate();
                                blockWrapper.remove();
                            }
                        }
                    }, time);
                    // Reset back to 0 elements, so we can continue adding
                    // blocks to regenerate to the list.
                    brokenBlocks.clear();
                }
            } else if (data.isBroken()) {
                data.remove();
            }

            if (data.isBroken() && blockDamage.getBreakMode(state.getType().asBlockType()) == BlockDamage.BreakMode.BREAK) {

                // For stat tracking
                blocksBroken += 1;

                if (RandomUtil.chance(blockChance)) {
                    Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                    Vector velocity = loc.toVector().subtract(origin.toVector()).normalize(); // normalize to slow down

                    // We want blocks to fly out of the newly formed crater.
                    velocity.setY(Math.abs(velocity.getY()));

                    // This method will add the falling block to the WeaponMechanics
                    // ticker, but it is only added on the next tick. This is
                    // important, since otherwise the projectile would spawn BEFORE
                    // all the blocks were destroyed.
                    spawnFallingBlock(loc, state, velocity);
                }
            }
        }

        if (blocksBroken != 0 && playerWrapper != null && playerWrapper.getStatsData() != null
            && projectile != null && projectile.getWeaponTitle() != null)
            playerWrapper.getStatsData().add(projectile.getWeaponTitle(), WeaponStat.BLOCKS_DESTROYED, blocksBroken);
    }

    protected void spawnFallingBlock(Location location, BlockState state, Vector velocity) {
        FakeEntity disguise = CompatibilityAPI.getEntityCompatibility().generateFakeEntity(location, state);

        RemoveOnBlockCollisionProjectile projectile = new RemoveOnBlockCollisionProjectile(location, velocity, disguise);
        projectile.setIntTag("explosion-falling-block", 1);
        WeaponMechanics.getInstance().getProjectileSpawner().spawn(projectile);
    }

    @Override
    public String getKeyword() {
        return "Explosion";
    }

    @Override
    public String getWikiLink() {
        return "https://cjcrafter.gitbook.io/weaponmechanics/weapon-modules/explosion";
    }

    @Override
    @NotNull public Explosion serialize(@NotNull SerializeData data) throws SerializerException {
        Serializer<ExplosionExposure> exposureSerializer = data.of("Explosion_Exposure")
                .getBukkitRegistry(ExplosionExposure.class, ExplosionExposures.REGISTRY)
                .orElse(new DefaultExposure());
        Serializer<ExplosionShape> shapeSerializer = data.of("Explosion_Shape")
                .getBukkitRegistry(ExplosionShape.class, ExplosionShapes.REGISTRY)
                .orElse(new DefaultExplosion());

        ExplosionExposure exposure = data.of("Explosion_Type_Data").serialize(exposureSerializer).get();
        ExplosionShape shape = data.of("Explosion_Type_Data").serialize(shapeSerializer).get();

        BlockDamage blockDamage = data.of("Block_Damage").serialize(BlockDamage.class).orElse(null);
        RegenerationData regeneration = data.of("Regeneration").serialize(RegenerationData.class).orElse(null);

        // Mistake that happens when copy-pasting. Explosions use the
        // 'Regeneration' config, not this option from WMC.
        if (data.has("Block_Damage.Ticks_Before_Regenerate")) {
            throw data.exception("Block_Damage.Ticks_Before_Regenerate", "You cannot use 'Ticks_Before_Regenerate' in Explosions",
                "Use the 'Explosion.Regeneration' section instead", "Wiki: https://cjcrafter.gitbook.io/weaponmechanics/weapon-modules/explosion#regeneration");
        }

        // This check determines if the player tried to use Block Regeneration
        // when blocks cannot even be broken in the first place. Easy mistake
        // to make when copying/pasting and deleting chunks of config.
        if ((blockDamage == null || !blockDamage.canBreakBlocks()) && regeneration != null) {
            throw data.exception(null, "Found an Explosion that defines 'Regeneration' when 'Block_Damage' cannot break blocks!",
                "This happens when 'Block_Damage.Break_Blocks: false' or when 'Block_Damage' was not added AND you tried to add 'Regeneration'");
        }

        // This is a required argument to determine when a projectile using this
        // explosion should explode (onEntityHit, onBlockHit, after delay, etc.)
        Detonation detonation = data.of("Detonation").assertExists().serialize(Detonation.class).get();

        double blockChance = data.step(new BlockDamage()).of("Spawn_Falling_Block_Chance").serialize(new ChanceSerializer()).orElse(0.0);
        double knockbackRate = data.of("Knockback_Multiplier").getDouble().orElse(1.0);

        // These 4 options are all nullable and not required for an explosion
        // to occur. It is very interesting when they are all used together :p
        ClusterBomb clusterBomb = data.of("Cluster_Bomb").serialize(ClusterBomb.class).orElse(null);
        AirStrike airStrike = data.of("Airstrike").serialize(AirStrike.class).orElse(null);
        Flashbang flashbang = data.of("Flashbang").serialize(Flashbang.class).orElse(null);
        MechanicManager mechanics = data.of("Mechanics").serialize(MechanicManager.class).orElse(null);

        return new Explosion(shape, exposure, blockDamage, regeneration, detonation, blockChance,
            knockbackRate, clusterBomb, airStrike, flashbang, mechanics);
    }
}
