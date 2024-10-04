package io.github.thebusybiscuit.sensibletoolbox.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

import io.github.bakedlibs.dough.protection.ProtectionManager;
import io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin;
import io.github.thebusybiscuit.sensibletoolbox.api.energy.EnergyNet;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBBlock;
import io.github.thebusybiscuit.sensibletoolbox.core.storage.LocationManager;

/**
 * Top-level collection of utility methods for Sensible Toolbox.
 * 
 * @author desht
 */
public final class SensibleToolbox {

    private SensibleToolbox() {}

    private static SensibleToolboxPlugin instance;

    /**
     * Get an instance of the running Sensible Toolbox plugin.
     *
     * @return the SensibleToolboxPlugin instance
     */
    public static SensibleToolboxPlugin getInstance() {
        if (instance == null) {
            instance = (SensibleToolboxPlugin) Bukkit.getPluginManager().getPlugin("SensibleToolbox");

            if (instance == null || !instance.isEnabled()) {
                throw new IllegalStateException("SensibleToolbox plugin is not available!");
            }
        }

        return instance;
    }

    /**
     * Get the item registry instance, which handles all item registration,
     * retrieval and inspection.
     *
     * @return the STB item registry
     */
    public static ItemRegistry getItemRegistry() {
        return getInstance().getItemRegistry();
    }

    /**
     * Given a location, return the STB block at that location, if any.
     *
     * @param l
     *            the location to check
     * @return the STB block at that location, or null if there is none
     */
    public static BaseSTBBlock getBlockAt(Location l) {
        return LocationManager.getManager().get(l);
    }

    /**
     * Given a location, return the STB block at that location, if any.
     *
     * @param l
     *            the location to check
     * @param checkSign
     *            if true and the location contains a sign, then also
     *            check the location of the block the sign is attached
     *            to
     * @return the STB block at that location, or null if there is none
     */
    public static BaseSTBBlock getBlockAt(Location l, boolean checkSign) {
        return LocationManager.getManager().get(l, checkSign);
    }

    /**
     * Given a location, return the STB block at that location, if any.
     *
     * @param l
     *            the location to check
     * @param type
     *            the block must be an instance or a subclass of this type
     * @param checkSign
     *            if true and the location contains a sign, then also
     *            check the location of the block the sign is attached
     *            to
     * @return the STB block at that location, or null if there is no block of the given type
     */
    public static <T extends BaseSTBBlock> T getBlockAt(Location l, Class<T> type, boolean checkSign) {
        return LocationManager.getManager().get(l, type, checkSign);
    }

    /**
     * Get the friend manager object. This object is responsible for managing
     * the trust relationships between players, primarily to support
     * Restricted access mode on STB blocks.
     *
     * @return the friend manager
     */
    public static FriendManager getFriendManager() {
        return getInstance().getFriendManager();
    }

    /**
     * Get the energy net for the given block.
     *
     * @param block
     *            the block to check
     */
    public static EnergyNet getEnergyNet(Block block) {
        return getInstance().getEnergyNetManager().getEnergyNet(block);
    }

    public static ProtectionManager getProtectionManager() {
        return getInstance().getProtectionManager();
    }
}
