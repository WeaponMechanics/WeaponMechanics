package me.deecaad.weaponmechanics.weapon.damage;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.block.BlockCompatibility;
import me.deecaad.core.utils.DistanceUtil;
import me.deecaad.core.utils.NumberUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.SeaPickle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class that stores all block damage on the server.
 */
public final class BlockDamageData {

    private static final Map<ChunkPos, Map<Block, DamageData>> DAMAGE_MAP = new LinkedHashMap<>(256);
    public static final int MAX_BLOCK_CRACK = 9;
    public static final Material MASK = Material.valueOf("AIR");

    public static final double EPSILON = 1e-7;

    /**
     * Don't let anyone instantiate this class
     */
    private BlockDamageData() {
    }

    /**
     * Shorthand for {@link #damage(Block, double, boolean, boolean, Material)}
     */
    public static DamageData damage(@NotNull Block block, double damage, boolean isBreak, boolean isRegenerate) {
        return damage(block, damage, isBreak, isRegenerate, MASK);
    }

    /**
     * Damages the given <code>block</code> for the given percentage of <code>damage</code>. Note that
     * the returned value should be checked to schedule a block regeneration task.
     *
     * @param block The non-null block to damage.
     * @param damage The percentage of damage [0.0, 1.0].
     * @param isBreak Whether the block will break or not.
     * @param isRegenerate false is will cause inventories to drop and block updates.
     * @param mask The block to replace... Usually {@link #MASK}.
     * @return <code>true</code> if the block was broken.
     */
    public static DamageData damage(@NotNull Block block, double damage, boolean isBreak, boolean isRegenerate, Material mask) {
        ChunkPos pos = new ChunkPos(block);

        // Get the DamageData for the given block, or create a new one if needed
        Map<Block, DamageData> map = DAMAGE_MAP.computeIfAbsent(pos, k -> new HashMap<>());
        DamageData damageData = map.computeIfAbsent(block, DamageData::new);

        damageData.damage(damage, isBreak, isRegenerate, mask);
        return damageData;
    }

    @Nullable public static DamageData getBlockDamage(@NotNull Block block) {
        Map<Block, DamageData> map = DAMAGE_MAP.get(new ChunkPos(block));
        if (map == null)
            return null;

        // may return null
        return map.get(block);
    }

    /**
     * Returns true if the given block is broken by WeaponMechanics. Note that is only returns true if
     * the block is scheduled to regenerate. When regeneration is turned off, this method will return
     * false for any block.
     *
     * @param block The non-null block position to test.
     * @return true if the block is broken and going to regenerate.
     */
    public static boolean isBroken(@NotNull Block block) {
        DamageData data = getBlockDamage(block);
        return data != null && data.isBroken();
    }

    /**
     * Regenerates the given block by resetting it to the {@link BlockState} it had before being
     * destroyed. This also resets the block's durability, and removes it from the damage map.
     *
     * @param block The non-null block to regenerate
     */
    public static void regenerate(@NotNull Block block) {
        Map<Block, DamageData> data = DAMAGE_MAP.get(new ChunkPos(block));

        if (data == null)
            return;

        DamageData damage = data.get(block);
        damage.regenerate();
        data.remove(block);
    }

    /**
     * Regenerates all blocks in the given chunk. Note that the given chunk must have a world associated
     * with it (such that {@link Chunk#getWorld()}) will not return <code>null</code>.
     *
     * <p>
     * After regeneration, the chunk will be removed from the cache.
     *
     * @param chunk The non-null chunk to regenerate all blocks.
     * @see #regenerate(Block)
     */
    public static void regenerate(@NotNull Chunk chunk) {
        ChunkPos pos = new ChunkPos(chunk);

        regenerate(pos);
        DAMAGE_MAP.remove(pos);
    }

    /**
     * Regenerates all blocks in the given world.
     *
     * <p>
     * After regeneration, all chunks from the world will be removed from the cache.
     *
     * @param world The non-null world to regenerate all blocks.
     * @see #regenerate(Block)
     */
    public static void regenerate(@NotNull World world) {
        Iterator<ChunkPos> iterator = DAMAGE_MAP.keySet().iterator();

        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();

            if (pos.world.equals(world)) {
                regenerate(pos);
                iterator.remove();
            }
        }
    }

    public static void regenerateAll() {
        Iterator<ChunkPos> iterator = DAMAGE_MAP.keySet().iterator();

        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();
            regenerate(pos);
            iterator.remove();
        }
    }

    private static void regenerate(@NotNull ChunkPos pos) {
        Map<Block, DamageData> blocks = DAMAGE_MAP.get(pos);

        if (blocks == null)
            return;

        blocks.forEach((block, damage) -> damage.regenerate());
    }

    /**
     * The {@link org.bukkit.Chunk} class does not provide a hashing method, so we are stuck wrapping
     * the chunk in order to use a {@link HashMap}
     */
    public static class ChunkPos {

        private final World world;
        private final int x;
        private final int z;

        public ChunkPos(Block block) {
            this(block.getChunk());
        }

        public ChunkPos(Chunk chunk) {
            this.world = chunk.getWorld();
            this.x = chunk.getX();
            this.z = chunk.getZ();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ChunkPos chunkPos = (ChunkPos) o;
            return x == chunkPos.x && z == chunkPos.z && world.equals(chunkPos.world);
        }

        @Override
        public int hashCode() {
            return ((x * 31) ^ z) ^ world.hashCode();
        }
    }

    public static class DamageData {

        private final Block block;
        private double durability = 1.0; // Stores a value [0.0, 1.0]. 0.0 = broken

        private BlockState state = null; // Stores the BlockState of a block before it is broken
        private int packetId = -1; // Stores the ID used for the block cracking packet

        private DamageData(Block block) {
            this.block = block;
        }

        public void damage(double amount, boolean isBreak, boolean isRegenerate) {
            damage(amount, isBreak, isRegenerate, MASK);
        }

        public void damage(double amount, boolean isBreak, boolean isRegenerate, Material mask) {
            durability -= amount;

            // Either break the block or send a crack packet
            if (isBreak && isBroken()) {
                // Check WorldGuard to determine whether we can break blocks here
                if (CompatibilityAPI.getWorldGuardCompatibility().testFlag(block.getLocation(), null, "weapon-break-block"))
                    destroy(isRegenerate, mask);
                else
                    durability = 1.0;
            } else {
                sendCrackPacket();
            }
        }

        public boolean isBroken() {
            return durability <= EPSILON;
        }

        public void destroy(boolean isRegenerate, Material mask) {
            state = block.getState();

            // We need to clear the contents of the inventory(s). If we skip
            // this, items will be dropped on the ground and double chests will
            // not regenerate with all of their items.
            if (isRegenerate && state instanceof InventoryHolder) {
                Inventory inv;
                if (state instanceof Chest) {
                    inv = ((Chest) state).getBlockInventory();
                } else {
                    inv = ((InventoryHolder) state).getInventory();
                }
                inv.clear();
            }

            // #212 - Try to copy the block data from the previous block.
            boolean attemptCopy = WeaponMechanics.getInstance().getConfiguration().getBoolean("Explosions.Attempt_Copy_Data", false);
            if (attemptCopy) {
                BlockData oldData = block.getBlockData();
                BlockData newData = mask.createBlockData();

                if (newData instanceof MultipleFacing newFace && oldData instanceof MultipleFacing oldFace)
                    for (BlockFace face : oldFace.getAllowedFaces())
                        newFace.setFace(face, oldFace.hasFace(face));
                if (newData instanceof Orientable newOrient && oldData instanceof Orientable oldOrient)
                    newOrient.setAxis(oldOrient.getAxis());
                if (newData instanceof Ageable newAge && oldData instanceof Ageable oldAge)
                    newAge.setAge(oldAge.getAge());
                if (newData instanceof Lightable newLight && oldData instanceof Lightable oldLight)
                    newLight.setLit(oldLight.isLit());
                if (newData instanceof Candle newCandle && oldData instanceof Candle oldCandle)
                    newCandle.setCandles(oldCandle.getCandles());
                if (newData instanceof SeaPickle newPickle && oldData instanceof SeaPickle oldPickle)
                    newPickle.setPickles(oldPickle.getPickles());
                if (newData instanceof Waterlogged newWater && oldData instanceof Waterlogged oldWater)
                    newWater.setWaterlogged(oldWater.isWaterlogged());
                if (newData instanceof Rotatable newRotate && oldData instanceof Rotatable oldRotate)
                    newRotate.setRotation(oldRotate.getRotation());

                block.setBlockData(newData, false);
                return;
            }

            block.setType(mask, !isRegenerate);
        }

        public void regenerate() {
            if (state != null) {
                state.update(true, false);
                state = null;
            }

            durability = 1.0;
            sendCrackPacket();
        }

        public void remove() {
            Map<Block, DamageData> map = DAMAGE_MAP.get(new ChunkPos(block));
            map.remove(block);
        }

        public void sendCrackPacket() {
            if (packetId == -1) {
                packetId = BlockCompatibility.IDS.incrementAndGet();
            }

            // -1 will remove crack effect from block
            int crack = (durability >= 1.0 - EPSILON)
                ? -1
                : (int) NumberUtil.lerp(MAX_BLOCK_CRACK, 0, durability);

            Object packet = CompatibilityAPI.getBlockCompatibility().getCrackPacket(block, crack, packetId);
            DistanceUtil.sendPacket(block.getLocation(), packet);
        }
    }
}
