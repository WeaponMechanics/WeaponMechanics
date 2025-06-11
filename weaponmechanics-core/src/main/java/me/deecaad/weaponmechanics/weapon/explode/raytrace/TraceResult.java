package me.deecaad.weaponmechanics.weapon.explode.raytrace;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.Set;

public class TraceResult {

    private final Set<Entity> entities;
    private final Set<Block> blocks;

    TraceResult(@Nullable Entity entity, @Nullable Block block) {
        entities = (entity == null) ? Collections.emptySet() : Collections.singleton(entity);
        blocks = (block == null) ? Collections.emptySet() : Collections.singleton(block);
    }

    TraceResult(@NotNull Set<Entity> entities, @NotNull Set<Block> blocks) {
        this.entities = entities;
        this.blocks = blocks;
    }

    @NotNull public Set<Entity> getEntities() {
        return entities;
    }

    /**
     * @return The first Entity found in the RayTrace
     */
    @Nullable public Entity getOneEntity() {
        return entities.stream().findFirst().orElse(null);
    }

    @NotNull public Set<Block> getBlocks() {
        return blocks;
    }

    /**
     * @return The first Block found in the RayTrace
     */
    @Nullable public Block getOneBlock() {
        return blocks.stream().findFirst().orElse(null);
    }

    /**
     * Returns <code>true</code> if no blocks and no entities were hit during the ray trace.
     *
     * @return true if nothing was hit
     */
    public boolean isEmpty() {
        return blocks.isEmpty() && entities.isEmpty();
    }
}
