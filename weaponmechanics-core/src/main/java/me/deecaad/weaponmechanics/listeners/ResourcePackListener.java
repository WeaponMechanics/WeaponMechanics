package me.deecaad.weaponmechanics.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;

public class ResourcePackListener implements Listener {

    private String resourcePackVersion;
    private String resourcePackLink;

    public ResourcePackListener() {
    }

    private void determineVersion() {
        try {
            String link = "https://api.github.com/repos/WeaponMechanics/ResourcePack/releases/latest";
            URI uri = URI.create(link);
            URLConnection connection = uri.toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String tag = json.get("tag_name").getAsString();
            JsonArray assets = json.getAsJsonArray("assets");

            for (JsonElement asset : assets) {
                String fileName = asset.getAsJsonObject().get("name").getAsString();

                if (fileName.startsWith("WeaponMechanicsResourcePack-") && fileName.endsWith(".zip")) {
                    resourcePackVersion = tag;
                    resourcePackLink = asset.getAsJsonObject().get("browser_download_url").getAsString();
                    WeaponMechanics.getInstance().debugger.info("Found resource pack version " + tag + " at " + resourcePackLink);
                }
            }
        } catch (IOException ex) {
            WeaponMechanics.getInstance().debugger.fine("Failed to fetch resource pack version due to timeout", ex);
        }

        if (resourcePackVersion == null) {
            WeaponMechanics.getInstance().debugger.warning("Failed to fetch resource pack version! Enable debug mode for more logs.");
        }
    }

    public @Nullable String getResourcePackVersion() {
        if (resourcePackVersion == null) {
            determineVersion();
        }
        return resourcePackVersion;
    }

    public @Nullable String getResourcePackLink() {
        if (resourcePackLink == null) {
            determineVersion();
        }
        return resourcePackLink;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!WeaponMechanics.getInstance().getConfiguration().getBoolean("Resource_Pack_Download.Automatically_Send_To_Player"))
            return;

        String link = WeaponMechanics.getInstance().getConfiguration().getString("Resource_Pack_Download.Link");
        if (link == null || link.isEmpty()) {
            WeaponMechanics.getInstance().debugger.warning("Resource_Pack_Download Link was missing in the config.yml!",
                "If you don't want to send players the resource pack, please disable Automatically_Send_To_Player instead!");
            return;
        }

        if ("LATEST".equals(link)) {
            // This is the default link, meaning the Admin hasn't changed it. We
            // should use the latest version instead. Run it on a delay to make
            // sure the player has joined.
            WeaponMechanics.getInstance().getFoliaScheduler().entity(player).runDelayed(() -> player.setResourcePack(getResourcePackLink()), 10L);
            return;
        }
        WeaponMechanics.getInstance().debugger.fine("Sending " + player.getName() + " resource pack: " + link);
        player.setResourcePack(link);
    }

    @EventHandler
    public void onPack(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();

        if (WeaponMechanics.getInstance().getConfiguration().getBoolean("Resource_Pack_Download.Force_Player_Download")) {
            PlayerResourcePackStatusEvent.Status status = event.getStatus();

            if (status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD
                || status == PlayerResourcePackStatusEvent.Status.DECLINED) {

                // TODO consider adding a permission to allow people to be exempt
                String message = WeaponMechanics.getInstance().getConfiguration().getString("Resource_Pack_Download.Kick_Message");
                player.kickPlayer(StringUtil.colorBukkit(message));
            }
        }
    }
}
