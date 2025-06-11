package me.deecaad.weaponmechanics.packetlisteners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import me.deecaad.core.MechanicsLogger;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import me.deecaad.weaponmechanics.wrappers.ZoomData;
import org.bukkit.entity.Player;

public class OutAbilitiesListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PLAYER_ABILITIES)
            return;

        Player player = event.getPlayer();
        if (player == null)
            return;

        EntityWrapper entity = WeaponMechanics.getInstance().getEntityWrapper(player);
        ZoomData mainZoomData = entity.getMainHandData().getZoomData();
        ZoomData offZoomData = entity.getOffHandData().getZoomData();

        // Player is not scoped in, no need to modify their FOV
        if (!mainZoomData.isZooming() && !offZoomData.isZooming())
            return;

        double zoomAmount = mainZoomData.isZooming() ? mainZoomData.getZoomAmount() : offZoomData.getZoomAmount();

        // Player is in VR (Vivecraft must be installed!)
        if (zoomAmount == 0)
            return;

        WrapperPlayServerPlayerAbilities wrapper = new WrapperPlayServerPlayerAbilities(event);
        wrapper.setFOVModifier(getScope(zoomAmount));
        wrapper.write();
    }

    /**
     * From this one you can fetch the scope level values. It can be either be for attributes or
     * abilities
     *
     * @param level the scope level
     * @return the amount of zoom (when using abilities or attributes depending on level)
     */
    public static float getScope(double level) {
        if (level < 1.0 || level > 10.0) {
            WeaponMechanics.getInstance().debugger.severe("Tried to get scope level of " + level + ", but only levels between 1 and 10 are allowed.");
            return 0;
        }
        return (float) (1.0 / (20.0 / level - 10.0)); // checking for division by zero is not needed here, Java gives Infinity when dividing by zero.
        // ABILITIES packet correctly understands the meaning of Infinity
    }
}
