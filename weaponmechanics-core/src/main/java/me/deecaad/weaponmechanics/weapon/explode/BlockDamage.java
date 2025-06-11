package me.deecaad.weaponmechanics.weapon.explode;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.ChanceSerializer;
import me.deecaad.core.file.simple.EnumValueSerializer;
import me.deecaad.core.file.simple.IntSerializer;
import me.deecaad.core.file.simple.RegistryValueSerializer;
import me.deecaad.core.utils.RandomUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.damage.BlockDamageData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BlockDamage implements Serializer<BlockDamage> {

    /**
     * Determines whether a block is broken, cracked, or skip damage.
     */
    public enum BreakMode {
        CANCEL,
        BREAK,
        CRACK
    }

    /**
     * Holds data from config for a material.
     *
     * @param mode How the block should be damaged (or not damaged).
     * @param blockDurability The maximum number of hits the block can take.
     * @param mask if mode == BREAK, the broken block is replaced with this.
     */
    public record DamageConfig(BreakMode mode, int blockDurability, Material mask) {
    }

    private double dropBlockChance;
    private int damage;

    private int defaultBlockDurability;
    private Material defaultMask;
    private BreakMode defaultMode;

    private Map<BlockType, DamageConfig> blocks;

    /**
     * Default constructor for serializers
     */
    public BlockDamage() {
    }

    /**
     * Constructor with arguments.
     *
     * @param dropBlockChance The [0, 1] chance to drop broken blocks as items.
     * @param damage The damage (usually 1) each hit.
     * @param defaultBlockDurability The default health any block has.
     * @param defaultMask The default mask (usually AIR) any block has.
     * @param defaultMode The default breaking mode any block has.
     * @param blocks Per-block overrides.
     */
    public BlockDamage(double dropBlockChance, int damage, int defaultBlockDurability, Material defaultMask,
        BreakMode defaultMode, Map<BlockType, DamageConfig> blocks) {
        this.dropBlockChance = dropBlockChance;
        this.damage = damage;
        this.defaultBlockDurability = defaultBlockDurability;
        this.defaultMask = defaultMask;
        this.defaultMode = defaultMode;
        this.blocks = blocks;
    }

    public double getDropBlockChance() {
        return dropBlockChance;
    }

    public void setDropBlockChance(double dropBlockChance) {
        this.dropBlockChance = dropBlockChance;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getDefaultBlockDurability() {
        return defaultBlockDurability;
    }

    public void setDefaultBlockDurability(int defaultBlockDurability) {
        this.defaultBlockDurability = defaultBlockDurability;
    }

    public Material getDefaultMask() {
        return defaultMask;
    }

    public void setDefaultMask(Material defaultMask) {
        this.defaultMask = defaultMask;
    }

    public BreakMode getDefaultMode() {
        return defaultMode;
    }

    public void setDefaultMode(BreakMode defaultMode) {
        this.defaultMode = defaultMode;
    }

    public Map<BlockType, DamageConfig> getBlocks() {
        return blocks;
    }

    public void setBlocks(Map<BlockType, DamageConfig> blocks) {
        this.blocks = blocks;
    }

    public BreakMode getBreakMode(Block block) {
        return getBreakMode(block.getType().asBlockType());
    }

    /**
     * Returns the behavior of the given material. Should it break? Should it crack? Or should it ignore
     * all damage?
     *
     * @param material The non-null material to check.
     * @return The non-null break mode of the material.
     */
    public @NotNull BreakMode getBreakMode(@NotNull BlockType material) {
        DamageConfig config = blocks.get(material);
        return config == null ? defaultMode : config.mode;
    }

    public int getDurability(@NotNull Block block) {
        return getDurability(block.getType().asBlockType());
    }

    /**
     * Returns the number of hits before the block is broken, or <code>-1</code> if the block should not
     * be broken.
     *
     * @param material The non-null material to check.
     * @return The number of hits before the block breaks.
     */
    public int getDurability(@NotNull BlockType material) {
        DamageConfig config = blocks.get(material);
        return config == null ? defaultBlockDurability : config.blockDurability;
    }

    public @NotNull Material getMask(@NotNull Block block) {
        return getMask(block.getType().asBlockType());
    }

    /**
     * Returns the block the given material should be replaced with, or <code>null</code> if the block
     * shouldn't be replaced.
     *
     * @param material The non-null material to check.
     * @return The nullable material to use as a mask.
     */
    public Material getMask(BlockType material) {
        DamageConfig config = blocks.get(material);
        return config == null ? defaultMask : config.mask;
    }

    /**
     * Returns <code>true</code> if at least 1 block can be broken through the
     * {@link #damage(Block, Player, boolean)} method. Useful for checking if a weapon can be used for
     * griefing.
     *
     * @return true if blocks can be broken.
     */
    public boolean canBreakBlocks() {
        if (defaultMode == BreakMode.BREAK)
            return true;

        return blocks.values().stream().anyMatch(config -> config.mode == BreakMode.BREAK);
    }

    /**
     * Applies damage to te given block. Any previous damage that was applied is STACKED to see if the
     * block should be broken. Depending on the block, damage might be skipped, the block may be
     * cracked, or the block may break.
     *
     * <p>
     * Regeneration is not handled automatically. You have to check the return value and handle
     * regeneration in a schedules task. If you pass <code>true</code>, blocks will be broken WITHOUT
     * block updates. If you pass <code>false</code>, blocks will be broken and neighboring blocks will
     * be updated (making accurate regeneration impossible).
     *
     * <blockquote>
     * 
     * <pre>{@code
     *      boolean regenerate = true;
     *      BlockDamageData.DamageData data = BlockDamage#damage(block, null, regenerate);
     *
     *      if (regenerate) {
     *          if (blockDamage.isBreak() && data.isBroken()) {
     *              new BukkitRunnable() {
     *                  public void run() {
     *                      data.regenerate();
     *                      data.remove();
     *                  }
     *              }.runTaskLater(plugin, 200L); // 10 seconds
     *          }
     *      } else if (data.isBroken()) {
     *          data.remove();
     *      }
     * }</pre>
     * 
     * </blockquote>
     *
     * @param block The non-null block to damage.
     * @param player The nullable player who is breaking block, explosions should always give null
     * @param isRegenerate Use true if you want to have perfect regeneration.
     * @return The DamageData wrapping the block, or null if no damage was applied.
     */
    @Nullable public BlockDamageData.DamageData damage(Block block, @Nullable Player player, boolean isRegenerate) {
        BreakMode blockBreakMode = getBreakMode(block);
        if (blockBreakMode != BreakMode.CANCEL && !BlockDamageData.isBroken(block)) {

            boolean dropItems = blockBreakMode == BreakMode.BREAK;
            Collection<ItemStack> drops = null;

            // Only use these if its BREAK, not CRACK
            if (blockBreakMode == BreakMode.BREAK) {

                // #307, people don't want protection plugins to interfere sometimes
                boolean disableBlockBreakEvent = WeaponMechanics.getInstance().getConfiguration().getBoolean("Disable_Block_Break_Event");

                // When you provide the player to this method, other plugins can
                // cancel the block damage. This is used for protection plugins
                // (other than world-edit, which is checked separately).
                if (!disableBlockBreakEvent && player != null) {

                    BlockBreakEvent breakEvent = new BlockBreakEvent(block, player) {
                        @Override
                        public @NotNull String getEventName() {
                            return "WeaponMechanicsBlockDamage";
                        }
                    };
                    Bukkit.getPluginManager().callEvent(breakEvent);

                    if (breakEvent.isCancelled())
                        return null;

                    dropItems = breakEvent.isDropItems();
                }

                // Calculate dropped blocks BEFORE the block is broken.
                drops = RandomUtil.chance(dropBlockChance) ? block.getDrops() : null;
                if (block.getState() instanceof InventoryHolder inv && !isRegenerate) {
                    if (drops == null)
                        drops = new ArrayList<>();

                    if (inv instanceof Chest)
                        Collections.addAll(drops, ((Chest) inv).getBlockInventory().getContents());
                    else
                        Collections.addAll(drops, inv.getInventory().getContents());
                }

            }

            int max = getDurability(block);
            BlockDamageData.DamageData data = BlockDamageData.damage(block, (double) damage / (double) max, getBreakMode(block) == BreakMode.BREAK, isRegenerate, getMask(block));
            if (data.isBroken() && drops != null && dropItems) {

                Location location = block.getLocation();
                for (ItemStack item : drops) {
                    if (item != null && item.getType() != Material.AIR && item.getAmount() > 0)
                        block.getWorld().dropItemNaturally(location, item);
                }
            }

            return data;
        }

        return null;
    }

    @Override
    public String getKeyword() {
        return "Block_Damage";
    }

    @Override
    public boolean letPassThrough(@NotNull String key) {
        return key.endsWith("Spawn_Falling_Block_Chance") || key.endsWith("Regenerate_After_Ticks");
    }

    @Override
    @NotNull public BlockDamage serialize(@NotNull SerializeData data) throws SerializerException {

        // Added November 1st, 2022 to detect out-dated configurations
        if (data.has("Block_List") || data.has("Shots_To_Break_Blocks")) {
            throw data.exception(null, "Found an outdated configuration!",
                "You need to update your 'Block_Damage' to match the new format");
        }

        Double dropBlockChance = data.of("Drop_Broken_Block_Chance").serialize(ChanceSerializer.class).orElse(0.0);

        int damagePerHit = data.of("Damage_Per_Hit").assertRange(0, null).getInt().orElse(1);
        int defaultBlockDurability = data.of("Default_Block_Durability").assertRange(0, null).getInt().orElse(1);
        BreakMode defaultMode = data.of("Default_Mode").getEnum(BreakMode.class).orElse(BreakMode.BREAK);
        Material defaultMask = data.of("Default_Mask").getEnum(Material.class).orElse(Material.AIR);

        Map<BlockType, DamageConfig> blocks = new HashMap<>();
        List<List<Optional<Object>>> list = data.ofList("Blocks")
            .addArgument(new RegistryValueSerializer<>(BlockType.class, true))
            .addArgument(new EnumValueSerializer<>(BreakMode.class, false))
            .requireAllPreviousArgs()
            .addArgument(new IntSerializer(1))
            .addArgument(new RegistryValueSerializer<>(BlockType.class, false))
            .assertExists().assertList();

        for (int i = 0; i < list.size(); i++) {
            List<Optional<Object>> split = list.get(i);

            List<BlockType> materials = (List<BlockType>) split.get(0).get();
            BreakMode mode = ((List<BreakMode>) split.get(1).get()).getFirst();
            Optional<Integer> blockDurability = (Optional<Integer>) (Optional<?>) split.get(2);
            Material mask = (Material) split.get(3).orElse(null);

            // Cannot apply a mask to blocks that cannot be broken
            if (mode != BreakMode.BREAK && mask != null) {
                throw data.listException("Blocks", i, "You cannot use material masks with '" + mode + "'",
                    "Found mask: " + mask,
                    "In order to use masks, use 'BREAK' mode");
            }

            // Cannot use durability or masks with blocks that cancel durability
            if (mode == BreakMode.CANCEL && blockDurability.isPresent()) {
                throw data.listException("Blocks", i, "You cannot use durability with 'CANCEL'",
                    "Found durability: " + blockDurability.get(),
                    "In order to use durability, use 'BREAK' or 'CRACK'");
            }

            // If user wants to break or crack this block, then they are
            // trying to override the default block durability. Hence,
            // block durability is a required argument.
            if ((mode == BreakMode.CRACK || mode == BreakMode.BREAK) && blockDurability.isEmpty()) {
                int goodNumber = (int) materials.get(0).getBlastResistance() + 1;
                throw data.listException("Blocks", i, "When using '" + mode + "', you MUST also use durability",
                    "Found: " + blockDurability,
                    "Use a number like " + goodNumber + "' instead");
            }

            if (mask == null)
                mask = Material.AIR;

            DamageConfig config = new DamageConfig(mode, blockDurability.orElse(-1), mask);
            for (BlockType mat : materials)
                blocks.put(mat, config);
        }

        return new BlockDamage(dropBlockChance, damagePerHit, defaultBlockDurability, defaultMask, defaultMode, blocks);
    }
}
