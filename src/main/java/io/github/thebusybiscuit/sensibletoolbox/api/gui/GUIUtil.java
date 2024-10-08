package io.github.thebusybiscuit.sensibletoolbox.api.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.sensibletoolbox.core.gui.STBInventoryGUI;

/**
 * A collection of miscellaneous GUI-related utility methods.
 * 
 * @author desht
 */
public final class GUIUtil {

    private GUIUtil() {}

    /**
     * Get the GUI which the given player currently has open, if any.
     *
     * @param p
     *            the player
     * @return the GUI the player is currently viewing, or null
     */
    @Nullable
    public static InventoryGUI getOpenGUI(@Nonnull Player p) {
        return STBInventoryGUI.getOpenGUI(p);
    }

    /**
     * Make an item stack texture, given a material, title and possible lore.
     *
     * @param material
     *            the material data to use for the texture
     * @param title
     *            the texture's title (primary tooltip)
     * @param lore
     *            the extended information for the texture
     * @return a new item stack with the desired texture
     */
    public static ItemStack makeTexture(Material material, String title, String... lore) {
        ItemStack res = new ItemStack(material);
        ItemMeta meta = res.getItemMeta();
        meta.setDisplayName(title);

        if (lore.length > 0) {
            meta.setLore(makeLore(lore));
        }

        res.setItemMeta(meta);
        return res;
    }

    /**
     * Change the display name (primary tooltip) for a texture.
     *
     * @param s
     *            the item to modify
     * @param disp
     *            the new display name
     */
    public static void setDisplayName(ItemStack s, String disp) {
        ItemMeta meta = s.getItemMeta();
        meta.setDisplayName(disp);
        s.setItemMeta(meta);
    }

    /**
     * Construct some item lore with grey coloring, given a
     * text array.
     *
     * @param lore
     *            array containing the lore text
     * @return a list of properly colored lore text
     */
    @Nonnull
    public static List<String> makeLore(String... lore) {
        List<String> res = new ArrayList<>();

        for (String s : lore) {
            res.add(ChatColor.GRAY + s);
        }

        return res;
    }

    /**
     * Create a new inventory GUI for the given listener. This method
     * is normally used to create the GUI for a STB block object,
     * where the GUI is created when the object is placed.
     *
     * @param listener
     *            the STB item or block which owns the GUI
     * @param size
     *            the GUI size, in slots; must be a multiple of 9
     * @param title
     *            the GUI title
     * @return a new inventory GUI object
     */
    @Nonnull
    public static InventoryGUI createGUI(InventoryGUIListener listener, int size, String title) {
        return new STBInventoryGUI(listener, size, title);
    }

    /**
     * Create a new inventory GUI for the given player and listener. This
     * method is normally used to create the GUI for an STB item object,
     * where the GUI is created when the item is interacted with in some way.
     *
     * @param p
     *            the player to open the GUI for
     * @param listener
     *            the STB item or block which owns the GUI
     * @param size
     *            the GUI size, in slots; must be a multiple of 9
     * @param title
     *            the GUI title
     * @return a new inventory GUI object
     */
    @Nonnull
    public static InventoryGUI createGUI(Player p, InventoryGUIListener listener, int size, String title) {
        return new STBInventoryGUI(p, listener, size, title);
    }
}
