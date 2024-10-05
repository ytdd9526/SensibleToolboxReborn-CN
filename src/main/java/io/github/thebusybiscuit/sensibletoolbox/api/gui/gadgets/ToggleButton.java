package io.github.thebusybiscuit.sensibletoolbox.api.gui.gadgets;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.sensibletoolbox.api.gui.InventoryGUI;
import io.github.thebusybiscuit.sensibletoolbox.utils.STBUtil;

/**
 * A GUI gadget which allows a toggleable (boolean) value to be
 * displayed and changed.
 * 
 * @author desht
 */
public class ToggleButton extends ClickableGadget {

    private final ItemStack trueTexture;
    private final ItemStack falseTexture;
    private final ToggleListener callback;
    private boolean value;

    /**
     * Constructs a toggle button gadget.
     *
     * @param gui
     *            the GUI to add the gadget to
     * @param slot
     *            the GUI slot to display the gadget in
     * @param value
     *            the initial value for the gadget
     * @param trueTexture
     *            the icon texture to display when the gadget is set to true
     * @param falseTexture
     *            the icon texture to display when the gadget is set to false
     * @param callback
     *            the code to run when the gadget is changed by a player
     */
    public ToggleButton(InventoryGUI gui, int slot, boolean value, ItemStack trueTexture, ItemStack falseTexture, ToggleListener callback) {
        super(gui, slot);
        this.trueTexture = trueTexture;
        this.falseTexture = falseTexture;
        this.callback = callback;
        this.value = value;
    }

    @Override
    public void onClicked(InventoryClickEvent e) {
        boolean newValue = !value;

        if (callback.run(newValue)) {
            value = newValue;
            e.setCurrentItem(getTexture());
        } else {
            // vetoed!
            if (e.getWhoClicked() instanceof Player) {
                STBUtil.complain((Player) e.getWhoClicked());
            }
        }
    }

    @Override
    public ItemStack getTexture() {
        return value ? trueTexture : falseTexture;
    }

    /**
     * Update this gadget's value.
     *
     * @param newValue
     *            the new value to set
     */
    public void setValue(boolean newValue) {
        if (value != newValue) {
            value = newValue;
            updateGUI();
        }
    }
}
