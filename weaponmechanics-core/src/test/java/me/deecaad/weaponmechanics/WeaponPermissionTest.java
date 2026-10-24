package me.deecaad.weaponmechanics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class WeaponPermissionTest {
    // Models permission integrations that invalidate cached inheritance on mutation.
    static class CountingMap extends HashMap<String, Boolean> {
        int invalidations;

        @Override
        public Boolean put(String k, Boolean v) {
            invalidations++;
            return super.put(k, v);
        }

        @Override
        public void putAll(Map<? extends String, ? extends Boolean> m) {
            super.putAll(m);
            invalidations++;
        }
    }

    static class Fixture implements AutoCloseable {
        MockedStatic<Bukkit> b = mockStatic(Bukkit.class);
        PluginManager pm = mock(PluginManager.class);
        Server server = mock(Server.class);
        Map<String, Permission> nodes = new HashMap<>();
        Player player = mock(Player.class);

        Fixture() {
            when(server.getPluginManager()).thenReturn(pm);
            b.when(Bukkit::getServer).thenReturn(server);
            b.when(Bukkit::getPluginManager).thenReturn(pm);
            b.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
            when(pm.getPermission(anyString()))
                    .thenAnswer(
                            i -> nodes.get(((String) i.getArgument(0)).toLowerCase(Locale.ROOT)));
            doAnswer(
                            i -> {
                                Permission p = i.getArgument(0);
                                nodes.put(p.getName(), p);
                                return null;
                            })
                    .when(pm)
                    .addPermission(any());
        }

        void run(List<String> weapons, boolean recalculate) {
            try {
                var pluginType = WeaponMechanics.class;
                Object plugin = mock(pluginType, CALLS_REAL_METHODS);
                var field = pluginType.getDeclaredField("weaponHandler");
                field.setAccessible(true);
                if (weapons != null) {
                    Object handler = mock(field.getType(), RETURNS_DEEP_STUBS);
                    Object info = field.getType().getMethod("getInfoHandler").invoke(handler);
                    when(info.getClass().getMethod("getSortedWeaponList").invoke(info))
                            .thenReturn(weapons);
                    field.set(plugin, handler);
                }
                var method =
                        pluginType.getDeclaredMethod("registerWeaponPermissions", boolean.class);
                method.setAccessible(true);
                method.invoke(plugin, recalculate);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public void close() {
            b.close();
        }
    }

    @Test
    public void publishesAllLinksWithOneInvalidation() throws Exception {
        try (Fixture f = new Fixture()) {
            Permission parent = new Permission("weaponmechanics.use.*", PermissionDefault.OP);
            CountingMap m = new CountingMap();
            var field = Permission.class.getDeclaredField("children");
            field.setAccessible(true);
            field.set(parent, m);
            f.nodes.put(parent.getName(), parent);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < 100; i++) names.add("weapon" + i);
            clearInvocations(f.pm);
            f.run(names, true);
            assertEquals(100, m.size());
            assertEquals(1, m.invalidations);
            verify(f.pm, times(1)).recalculatePermissionDefaults(parent);
            verify(f.player, times(1)).recalculatePermissions();
        }
    }

    @Test
    public void preservesDefaultsAndUnrelatedEdges() {
        try (Fixture f = new Fixture()) {
            Permission parent = new Permission("weaponmechanics.use.*", PermissionDefault.FALSE);
            parent.getChildren().put("thirdparty.node", false);
            parent.getChildren().put("weaponmechanics.use.pistol", false);
            Permission existing =
                    new Permission("weaponmechanics.use.pistol", PermissionDefault.TRUE);
            f.nodes.put(parent.getName(), parent);
            f.nodes.put(existing.getName(), existing);
            f.run(List.of("pistol", "rifle"), false);
            assertSame(existing, f.nodes.get(existing.getName()));
            assertEquals(PermissionDefault.TRUE, existing.getDefault());
            assertEquals(PermissionDefault.FALSE, parent.getDefault());
            assertEquals(false, parent.getChildren().get("thirdparty.node"));
            assertEquals(true, parent.getChildren().get("weaponmechanics.use.pistol"));
            assertEquals(true, parent.getChildren().get("weaponmechanics.use.rifle"));
            assertEquals(
                    PermissionDefault.FALSE, f.nodes.get("weaponmechanics.use.rifle").getDefault());
            verifyNoInteractions(f.player);
        }
    }

    @Test
    public void createsParentAndRepeatPreservesGraph() {
        try (Fixture f = new Fixture()) {
            f.run(List.of("pistol", "rifle"), false);
            Permission parent = f.nodes.get("weaponmechanics.use.*");
            assertEquals(PermissionDefault.OP, parent.getDefault());
            Map<String, Boolean> before = new HashMap<>(parent.getChildren());
            f.run(List.of("pistol", "rifle"), false);
            assertEquals(before, parent.getChildren());
            assertEquals(3, f.nodes.size());
        }
    }

    @Test
    public void emptyListStillHonorsExplicitPlayerRecalculation() {
        try (Fixture f = new Fixture()) {
            f.run(List.of(), true);
            assertTrue(f.nodes.get("weaponmechanics.use.*").getChildren().isEmpty());
            verify(f.player).recalculatePermissions();
        }
    }

    @Test
    public void nullHandlerHasNoSideEffects() {
        try (Fixture f = new Fixture()) {
            f.run(null, true);
            assertTrue(f.nodes.isEmpty());
            verifyNoInteractions(f.player);
        }
    }
}
