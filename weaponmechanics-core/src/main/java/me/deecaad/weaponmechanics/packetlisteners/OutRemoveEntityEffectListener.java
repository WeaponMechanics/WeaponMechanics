package me.deecaad.weaponmechanics.packetlisteners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.entity.Player;

public class OutRemoveEntityEffectListener implements PacketListener {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.REMOVE_ENTITY_EFFECT)
            return;

        Player player = event.getPlayer();
        WrapperPlayServerRemoveEntityEffect wrapper = new WrapperPlayServerRemoveEntityEffect(event);
        int entityId = wrapper.getEntityId();

        if (entityId != player.getEntityId())
            return;

        EntityWrapper entity = WeaponMechanics.getInstance().getEntityWrapper(player);

        if (!entity.getMainHandData().getZoomData().hasZoomNightVision() && !entity.getOffHandData().getZoomData().hasZoomNightVision())
            return;
        if (wrapper.getPotionType() != PotionTypes.NIGHT_VISION)
            return;

        event.setCancelled(true);
    }
}
