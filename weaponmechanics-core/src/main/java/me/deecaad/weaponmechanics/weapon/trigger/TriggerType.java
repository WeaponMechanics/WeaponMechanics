package me.deecaad.weaponmechanics.weapon.trigger;

public enum TriggerType {

    START_SNEAK,
    END_SNEAK,
    DOUBLE_SNEAK,

    START_SPRINT,
    END_SPRINT,

    RIGHT_CLICK,
    LEFT_CLICK,
    MELEE,

    DROP_ITEM,

    JUMP,
    DOUBLE_JUMP,

    START_SWIM,
    END_SWIM,

    START_GLIDE,
    END_GLIDE,

    SWAP_HANDS,

    START_WALK,
    END_WALK,

    START_IN_MIDAIR,
    END_IN_MIDAIR,

    START_STAND,
    END_STAND;

    public boolean isSprintType() {
        return this == TriggerType.START_SPRINT || this == TriggerType.END_SPRINT;
    }

    public boolean isRightOrLeft() {
        return this == RIGHT_CLICK || this == LEFT_CLICK;
    }
}