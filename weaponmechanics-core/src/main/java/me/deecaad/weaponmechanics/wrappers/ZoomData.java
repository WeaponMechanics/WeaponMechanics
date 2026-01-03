package me.deecaad.weaponmechanics.wrappers;

import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.MechanicManager;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.scope.ScopeHandler;
import me.deecaad.weaponmechanics.weapon.weaponevents.WeaponScopeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.vivecraft.api.VRAPI;
import org.vivecraft.api.data.VRBodyPartData;
import org.vivecraft.api.data.VRPose;

public class ZoomData {

    private final HandData handData;
    private double zoomAmount;
    private int zoomStacks;
    private boolean zoomNightVision;
    private ItemStack scopeWeaponStack;
    private String scopeWeaponTitle;

    public ZoomData(HandData handData) {
        this.handData = handData;
    }

    public HandData getHandData() {
        return handData;
    }

    /**
     * @return <code>true</code> if the entity is scoped.
     */
    public boolean isZooming() {
        EntityWrapper entityWrapper = handData.getEntityWrapper();
        if (Bukkit.getPluginManager().getPlugin("Vivecraft_Spigot_Extensions") != null) {
            if (entityWrapper.getEntity() instanceof Player player) {
                VRPose pose = VRAPI.instance().getVRPose(player);
                if (pose != null) {
                    // Get the position and direction from player metadata
                    VRBodyPartData controller = handData.isMainhand() ? pose.getMainHand() : pose.getOffHand();
                    VRBodyPartData head = pose.getHead();
                    return controller.getDir().dot(head.getDir()) > 0.94;
                }
            }
        }
        return zoomAmount != 0;
    }

    /**
     * @return The magnitude of the scope amount, or 0 for not scoping.
     */
    public double getZoomAmount() {
        return zoomAmount;
    }

    /**
     * Note: This method does not set the player's FOV. This method should not be used unless you know
     * what you are doing.
     *
     * @param zoomAmount How far the player is zoomed in.
     */
    public void setZoomAmount(double zoomAmount) {
        this.zoomAmount = zoomAmount;
    }

    /**
     * @return How many times stacky-scope was used, or 0.
     */
    public int getZoomStacks() {
        return zoomStacks;
    }

    /**
     * Note: This method does not set the player's FOV. This method should not be used unless you know
     * what you are doing.
     *
     * @param zoomStacks the new zoom stack amount.
     */
    public void setZoomStacks(int zoomStacks) {
        this.zoomStacks = Math.max(0, zoomStacks);
    }

    /**
     * @return <code>true</code> if entity is scoped into a night vision scope.
     */
    public boolean hasZoomNightVision() {
        return zoomNightVision;
    }

    /**
     * Note: This method does not set the player's potion effects. This method should not be used unless
     * you know what you are doing.
     *
     * @param zoomNightVision whether zoom night vision is on
     */
    public void setZoomNightVision(boolean zoomNightVision) {
        this.zoomNightVision = zoomNightVision;
    }

    public void ifZoomingForceZoomOut() {
        if (isZooming()) {

            // IF player is in VR this happens
            if (getZoomAmount() == 0)
                return;

            EntityWrapper entityWrapper = handData.getEntityWrapper();

            ScopeHandler scopeHandler = WeaponMechanics.getInstance().getWeaponHandler().getScopeHandler();
            scopeHandler.updateZoom(entityWrapper, this, 0);
            setZoomStacks(0);
            scopeHandler.useNightVision(entityWrapper, this, false);

            MechanicManager zoomOffMechanics = WeaponMechanics.getInstance().getWeaponConfigurations().getObject(this.scopeWeaponTitle + ".Scope.Zoom_Off.Mechanics", MechanicManager.class);

            WeaponScopeEvent weaponScopeEvent = new WeaponScopeEvent(this.scopeWeaponTitle, this.scopeWeaponStack,
                entityWrapper.getEntity(), getHandData().isMainhand() ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND,
                WeaponScopeEvent.ScopeType.OUT, 0, 0, zoomOffMechanics);
            Bukkit.getPluginManager().callEvent(weaponScopeEvent);

            // Get Mechanics from event, so we can let plugins modify them.
            if (weaponScopeEvent.getMechanics() != null)
                weaponScopeEvent.getMechanics().use(new CastData(entityWrapper.getEntity(), this.scopeWeaponTitle, this.scopeWeaponStack));
        }

        // This just ensures that these are set to null
        setScopeData(null, null);
    }

    public void setScopeData(String weaponTitle, ItemStack weaponStack) {
        this.scopeWeaponTitle = weaponTitle;
        this.scopeWeaponStack = weaponStack;
    }
}