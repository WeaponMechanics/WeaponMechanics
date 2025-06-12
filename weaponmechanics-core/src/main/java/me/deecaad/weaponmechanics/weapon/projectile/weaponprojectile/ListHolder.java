package me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.simple.DoubleSerializer;
import me.deecaad.core.file.simple.RegistryValueSerializer;
import org.bukkit.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ListHolder<T extends Keyed> implements Serializer<ListHolder<T>> {

    private Class<T> clazz;

    private boolean allowAny;
    private boolean whitelist;
    private double defaultSpeedMultiplier;
    private Map<T, Double> list;

    public ListHolder(boolean allowAny, boolean whitelist, double defaultSpeedMultiplier, Map<T, Double> list) {
        this.allowAny = allowAny;
        this.whitelist = whitelist;
        this.defaultSpeedMultiplier = defaultSpeedMultiplier;
        this.list = list;
    }

    public ListHolder(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * If this is null, that means key is NOT valid
     *
     * @param key the key
     * @return the speed modifier of key or null if it's not valid
     */
    public @Nullable Double isValid(T key) {
        if (allowAny) {
            // Since all values are valid, simply return speed modifier
            if (list == null)
                return defaultSpeedMultiplier;

            // The value of key might be null if it doesn't have value defined
            Double value = list.getOrDefault(key, defaultSpeedMultiplier);
            return value == null ? defaultSpeedMultiplier : value;
        }

        if (!whitelist) {
            // If blacklist and list contains key
            // -> Can't use
            // Else return speed modifier

            // Speed modifier wont work with blacklist
            return list.containsKey(key) ? null : defaultSpeedMultiplier;
        }

        // If whitelist and list DOES NOT contain key
        // -> Can't use
        // Else return speed modifier
        if (!list.containsKey(key))
            return null;

        // The value of key might be null if it doesn't have value defined
        Double value = list.getOrDefault(key, defaultSpeedMultiplier);
        return value == null ? defaultSpeedMultiplier : value;
    }

    @Override
    public @NotNull ListHolder<T> serialize(@NotNull SerializeData data) throws SerializerException {
        boolean allowAny = data.of("Allow_Any").getBool().orElse(false);

        Map<T, Double> mapList = new HashMap<>();
        List<List<Optional<Object>>> list = data.ofList("List")
            .addArgument(new RegistryValueSerializer<>(clazz, true))
            .requireAllPreviousArgs()
            .addArgument(new DoubleSerializer())
            .assertList();

        for (List<Optional<Object>> split : list) {
            List<T> matches = (List<T>) split.get(0).get();
            Double speedMultiplier = (Double) split.get(1).orElse(null);

            for (T validValue : matches) {
                // Speed multiplier is null if it isn't defined
                mapList.put(validValue, speedMultiplier);
            }
        }

        if (mapList.isEmpty()) {
            if (!allowAny) {
                throw data.exception(null, "'List' found without any valid options",
                    "This happens when 'Allow_Any: false' and 'List' is empty");
            }
            mapList = null;
        }

        double defaultSpeedMultiplier = data.of("Default_Speed_Multiplier").assertRange(0.0, null).getDouble().orElse(1.0);
        boolean whitelist = data.of("Whitelist").getBool().orElse(true);
        return new ListHolder<>(allowAny, whitelist, defaultSpeedMultiplier, mapList);
    }
}
