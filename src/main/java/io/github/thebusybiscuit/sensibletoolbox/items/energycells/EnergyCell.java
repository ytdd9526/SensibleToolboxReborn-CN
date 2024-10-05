package io.github.thebusybiscuit.sensibletoolbox.items.energycells;

import javax.annotation.Nonnull;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.energy.Chargeable;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBItem;
import io.github.thebusybiscuit.sensibletoolbox.utils.STBUtil;
import io.github.thebusybiscuit.sensibletoolbox.utils.UnicodeSymbol;

public abstract class EnergyCell extends BaseSTBItem implements Chargeable {

    private double charge;

    protected EnergyCell() {
        setCharge(0.0);
    }

    public EnergyCell(ConfigurationSection conf) {
        setCharge(conf.getDouble("charge"));
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        conf.set("charge", getCharge());
        return conf;
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_HELMET;
    }

    @Override
    public String[] getLore() {
        return new String[] { "Stores up to " + UnicodeSymbol.ELECTRICITY.toUnicode() + " " + getMaxCharge() + " SCU" };
    }

    @Override
    public String[] getExtraLore() {
        return new String[] { STBUtil.getChargeString(this) };
    }

    public abstract Color getCellColor();

    @Override
    public double getCharge() {
        return charge;
    }

    @Override
    public void setCharge(double charge) {
        this.charge = charge;
    }

    @Override
    public boolean isWearable() {
        return false;
    }

    @Override
    public ItemStack toItemStack(int amount) {
        ItemStack res = super.toItemStack(amount);
        ItemMeta meta = res.getItemMeta();
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(getCellColor());
            res.setItemMeta(meta);
        }
        return res;
    }

    @Override
    public void onInteractItem(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getHand() == EquipmentSlot.OFF_HAND) {
                e.setCancelled(true);
                return;
            }
            e.setCancelled(true);
            chargeHotbarItems(e.getPlayer());
        }
    }

    private void chargeHotbarItems(@Nonnull Player p) {
        if (getCharge() > 0) {
            int held = p.getInventory().getHeldItemSlot();

            for (int slot = 0; slot < 8; slot++) {
                if (slot == held) {
                    continue;
                }

                ItemStack s = p.getInventory().getItem(slot);
                BaseSTBItem item = SensibleToolbox.getItemRegistry().fromItemStack(s);

                if (item instanceof Chargeable) {
                    Chargeable c = (Chargeable) item;
                    double toTransfer = Math.min(c.getMaxCharge() - c.getCharge(), c.getChargeRate());

                    if (toTransfer > 0) {
                        toTransfer = Math.min(toTransfer, getCharge());
                        setCharge(getCharge() - toTransfer);
                        p.setItemInHand(toItemStack());
                        c.setCharge(c.getCharge() + toTransfer);
                        p.getInventory().setItem(slot, item.toItemStack());
                        break;
                    }
                }
            }
        }
    }
}
