package me.deecaad.weaponmechanics.weapon.stats;

import java.util.Arrays;
import java.util.Set;

public enum WeaponStat {

    UUID("VARCHAR(100) NOT NULL", String.class),
    WEAPON_TITLE("VARCHAR(100) NOT NULL", String.class),

    SKIN("VARCHAR(100)", String.class),
    HAND_SKIN("VARCHAR(100)", String.class),
    ATTACHMENTS("TEXT", Set.class),

    EQUIP_TIMES("INTEGER", Integer.class),

    // SHOTS_MISSED = SHOTS - TOTAL_HITS
    SHOTS("INTEGER", Integer.class),

    // TOTAL_HITS = head + body + arm + left + foot
    HEAD_HITS("INTEGER", Integer.class),
    BODY_HITS("INTEGER", Integer.class),
    ARM_HITS("INTEGER", Integer.class),
    LEG_HITS("INTEGER", Integer.class),
    FOOT_HITS("INTEGER", Integer.class),
    BACKSTABS("INTEGER", Integer.class),
    CRITICAL_HITS("INTEGER", Integer.class),
    LONGEST_DISTANCE_HIT("FLOAT", Float.class),

    TOTAL_DAMAGE("FLOAT", Float.class),

    // TOTAL_KILLS = player + other
    PLAYER_KILLS("INTEGER", Integer.class),
    OTHER_KILLS("INTEGER", Integer.class),
    PLAYER_ASSISTS("INTEGER", Integer.class),
    OTHER_ASSISTS("INTEGER", Integer.class),

    HEAD_KILLS("INTEGER", Integer.class),
    BODY_KILLS("INTEGER", Integer.class),
    ARM_KILLS("INTEGER", Integer.class),
    LEG_KILLS("INTEGER", Integer.class),
    FOOT_KILLS("INTEGER", Integer.class),
    BACKSTAB_KILLS("INTEGER", Integer.class),
    CRITICAL_KILLS("INTEGER", Integer.class),
    LONGEST_DISTANCE_KILL("FLOAT", Float.class),

    BLOCKS_DESTROYED("INTEGER", Integer.class);

    private final String columnType;
    private final Class<?> classType;
    private final Object defaultValue;

    public static final WeaponStat[] VALUES = values();

    WeaponStat(String columnType, Class<?> classType) {
        this.columnType = columnType;
        this.classType = classType;

        if (classType == Integer.class) {
            this.defaultValue = 0;
        } else if (classType == Float.class) {
            this.defaultValue = (float) 0.0;
        } else {
            // String, Set
            this.defaultValue = null;
        }
    }

    public Class<?> getClassType() {
        return classType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public static String getCreateTableString() {
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS weapon_stats (");
        Arrays.stream(VALUES).forEach(stat -> builder
            .append(stat.name())
            .append(" ")
            .append(stat.columnType)
            .append(", "));
        builder.append("PRIMARY KEY (UUID, WEAPON_TITLE))");

        return builder.toString();
    }
}