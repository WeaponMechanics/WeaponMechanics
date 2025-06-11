package me.deecaad.weaponmechanics.packetlisteners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.entity.Player;

public class OutEntityEffectListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EFFECT)
            return;

        WrapperPlayServerEntityEffect wrapper = new WrapperPlayServerEntityEffect(event);
        int id = wrapper.getEntityId();

        // A negative ID doesn't happen in Vanilla Minecraft. When this is
        // negative, it means that ScopeCompatibility sent this packet.
        if (id < 0) {
            wrapper.setEntityId(-id);
            wrapper.write();
            return;
        }

        // Only modify this packet if it is for the player it is sent to
        if (id != ((Player) event.getPlayer()).getEntityId())
            return;

        EntityWrapper entity = WeaponMechanics.getInstance().getEntityWrapper(event.getPlayer());

        if (!entity.getMainHandData().getZoomData().hasZoomNightVision() && !entity.getOffHandData().getZoomData().hasZoomNightVision())
            return;
        if (wrapper.getPotionType() != PotionTypes.NIGHT_VISION)
            return;

        event.setCancelled(true);
    }
}
