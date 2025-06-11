package me.deecaad.weaponmechanics.weapon.explode.raytrace;

import me.deecaad.core.compatibility.HitBox;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class TraceCollision {

    public static final TraceCollision BLOCK = new TraceCollision(true, false, true);
    public static final TraceCollision ENTITY = new TraceCollision(false, true, true);
    public static final TraceCollision BLOCK_OR_ENTITY = new TraceCollision(true, true, true);
    public static final TraceCollision BLOCKS = new TraceCollision(true, false, false);
    public static final TraceCollision ENTITIES = new TraceCollision(false, true, false);
    public static final TraceCollision ALL = new TraceCollision(true, true, false);

    private final boolean hitBlock;
    private final boolean hitEntity;
    private final boolean first;

    private TraceCollision(boolean hitBlock, boolean hitEntity, boolean first) {
        this.hitBlock = hitBlock;
        this.hitEntity = hitEntity;
        this.first = first;
    }

    public TraceCollision(TraceCollision collision) {
        this.hitBlock = collision.hitBlock;
        this.hitEntity = collision.hitEntity;
        this.first = collision.first;
    }

    public boolean isHitBlock() {
        return hitBlock;
    }

    public boolean isHitEntity() {
        return hitEntity;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean canHit(Block block) {
        return HitBox.getHitbox(block, false) != null;
    }

    public boolean canHit(Entity entity) {
        return true;
    }
}
