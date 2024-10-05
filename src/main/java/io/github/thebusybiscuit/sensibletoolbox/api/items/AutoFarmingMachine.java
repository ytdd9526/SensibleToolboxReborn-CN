package io.github.thebusybiscuit.sensibletoolbox.api.items;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import io.github.thebusybiscuit.sensibletoolbox.api.STBInventoryHolder;
import io.github.thebusybiscuit.sensibletoolbox.core.storage.LocationManager;
import io.github.thebusybiscuit.sensibletoolbox.utils.VanillaInventoryUtils;

public abstract class AutoFarmingMachine extends BaseSTBMachine {

    public AutoFarmingMachine() {
        super();
    }

    public AutoFarmingMachine(ConfigurationSection conf) {
        super(conf);
    }

    public abstract double getScuPerCycle();

    @Override
    public boolean acceptsEnergy(BlockFace face) {
        return true;
    }

    @Override
    public boolean suppliesEnergy(BlockFace face) {
        return false;
    }

    @Override
    public int getTickRate() {
        double speedMultiplier = getSpeedMultiplier();
        int baseTickRate = 60;
        return (int) Math.max(1, baseTickRate / (1 + speedMultiplier));
    }

    @Override
    public int getMaxCharge() {
        return 2500;
    }

    @Override
    public int getChargeRate() {
        return 25;
    }

    @Override
    public int[] getInputSlots() {
        return new int[0];
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] { 10, 11, 12, 13, 14, 15 };
    }

    @Override
    public int[] getUpgradeSlots() {
        return new int[] { 43, 44 };
    }

    @Override
    public int getUpgradeLabelSlot() {
        return 42;
    }

    @Override
    public int getEnergyCellSlot() {
        return 36;
    }

    @Override
    public int getChargeDirectionSlot() {
        return 37;
    }

    @Override
    public int getInventoryGUISize() {
        return 45;
    }

    @Override
    public void onServerTick() {
        handleAutoEjection();
        super.onServerTick();
    }

    protected void handleAutoEjection() {
        if (getAutoEjectDirection() != null && getAutoEjectDirection() != BlockFace.SELF) {
            for (int slot : getOutputSlots()) {
                ItemStack s = getInventoryItem(slot);

                if (s != null) {
                    if (autoEject(s)) {
                        int amount = s.getAmount() > 3 ? s.getAmount() - 4 : 0;
                        s.setAmount(amount);
                        setInventoryItem(slot, s);
                        setJammed(false);
                    }

                    break;
                }
            }
        }
    }

    private boolean autoEject(@Nonnull ItemStack result) {
        Location l = getRelativeLocation(getAutoEjectDirection());
        Block target = l.getBlock();
        ItemStack i = result.clone();
        i.setAmount(1);

        if (!target.getType().isSolid() || Tag.WALL_SIGNS.isTagged(target.getType())) {
            // no (solid) block there - just drop the item
            Item item = l.getWorld().dropItem(l.add(0.5, 0.5, 0.5), i);
            item.setVelocity(new Vector(0, 0, 0));
            return true;
        } else {
            BaseSTBBlock stb = LocationManager.getManager().get(l);
            int nInserted = stb instanceof STBInventoryHolder ? ((STBInventoryHolder) stb).insertItems(i, getAutoEjectDirection().getOppositeFace(), false, getOwner()) : VanillaInventoryUtils.vanillaInsertion(target, i, 1, getAutoEjectDirection().getOppositeFace(), false, getOwner());
            return nInserted > 0;
        }
    }

}
