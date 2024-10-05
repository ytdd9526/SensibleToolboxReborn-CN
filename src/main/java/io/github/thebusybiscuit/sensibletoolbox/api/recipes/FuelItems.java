package io.github.thebusybiscuit.sensibletoolbox.api.recipes;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.desht.dhutils.Debugger;

/**
 * Represents items that may be converted to SCU by some machine. A machine
 * which does this conversion can create a static instance of this object, to
 * effectively use as a fuel dictionary.
 * 
 * @author desht
 */
public class FuelItems {

    private final Map<ItemStack, FuelValues> fuels = new HashMap<>();
    private final Map<Material, FuelValues> fuelMaterials = new EnumMap<>(Material.class);
    private final Set<ItemStack> fuelInfo = new HashSet<>();

    /**
     * Register an item as fuel.
     *
     * @param s
     *            the item to register
     * @param ignoreData
     *            true if the item's data value should be ignore; false otherwise
     * @param chargePerTick
     *            the amount of SCU generated per tick
     * @param burnTime
     *            the time in server ticks to convert the item into SCU
     */
    public void addFuel(ItemStack s, boolean ignoreData, double chargePerTick, int burnTime) {
        if (ignoreData) {
            fuelMaterials.put(s.getType(), new FuelValues(chargePerTick, burnTime));
        } else {
            fuels.put(getSingle(s), new FuelValues(chargePerTick, burnTime));
        }

        ItemStack info = s.clone();
        ItemMeta im = info.getItemMeta();
        im.setLore(Arrays.asList(ChatColor.GRAY + "" + ChatColor.ITALIC + get(s).toString()));
        info.setItemMeta(im);
        fuelInfo.add(info);
        Debugger.getInstance().debug("register burnable fuel: " + s + " -> " + get(s).toString());
    }

    public Collection<ItemStack> getFuelInfos() {
        return fuelInfo;
    }

    /**
     * Get the fuel values for the given item.
     *
     * @param s
     *            the item to check
     * @return the fuel values for the item, or null if this item is not known
     */
    public FuelValues get(ItemStack s) {
        FuelValues res = fuels.get(getSingle(s));
        return res == null ? fuelMaterials.get(s.getType()) : res;
    }

    /**
     * Check if the given can be used as a fuel.
     *
     * @param s
     *            the item to check
     * @return true if the item is a fuel, false otherwise
     */
    public boolean has(ItemStack s) {
        return fuels.containsKey(getSingle(s)) || fuelMaterials.containsKey(s.getType());
    }

    private ItemStack getSingle(ItemStack s) {
        if (s.getAmount() == 1) {
            return s;
        } else {
            ItemStack stack2 = s.clone();
            stack2.setAmount(1);
            return stack2;
        }
    }
}
