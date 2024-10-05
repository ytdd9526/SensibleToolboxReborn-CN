package io.github.thebusybiscuit.sensibletoolbox.blocks;


import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.GUIUtil;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.InventoryGUI;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.gadgets.NumericGadget;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBBlock;
import io.github.thebusybiscuit.sensibletoolbox.listeners.SoundMufflerListener;
import io.github.thebusybiscuit.sensibletoolbox.utils.IntRange;

/**
 * The {@link SoundMuffler} muffles or mutes nearby sounds.
 * This item requires ProtocolLib to be installed.
 *
 * @author desht
 * @author TheBusyBiscuit
 *
 * @see SoundMufflerListener
 *
 */
public class SoundMuffler extends BaseSTBBlock {

    public static final int DISTANCE = 8;

    // Can be between 0-100
    private int volume;

    public SoundMuffler() {
        volume = 10;
        createGUI();
    }

    public SoundMuffler(ConfigurationSection conf) {
        super(conf);
        volume = conf.getInt("volume");
        createGUI();
    }

    @Override
    protected InventoryGUI createGUI() {
        InventoryGUI gui = GUIUtil.createGUI(this, 9, ChatColor.DARK_AQUA + getItemName());

        gui.addGadget(new NumericGadget(gui, 0, "Volume", new IntRange(0, 100), getVolume(), 10, 1, newValue -> {
            setVolume(newValue);
            return true;
        }));

        return gui;
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        conf.set("volume", volume);
        return conf;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
        update(false);
    }

    @Override
    public Material getMaterial() {
        return Material.WHITE_WOOL;
    }

    @Override
    public String getItemName() {
        return "Sound Muffler";
    }

    @Override
    public String[] getLore() {
        return new String[] { "Reduces the volume of all sounds", "within a " + DISTANCE + "-block radius", "R-click: " + ChatColor.WHITE + " open configuration" };
    }

    @Override
    public Recipe getMainRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(getKey(), toItemStack());
        recipe.shape("WWW", "WNW", "WWW");
        recipe.setIngredient('W', Material.WHITE_WOOL);
        recipe.setIngredient('N', Material.NOTE_BLOCK);
        return recipe;
    }

    @Override
    public void onBlockRegistered(Location l, boolean isPlacing) {
        SensibleToolboxPlugin plugin = ((SensibleToolboxPlugin) getProviderPlugin());
        SoundMufflerListener listener = plugin.getSoundMufflerListener();
        listener.registerMuffler(this);

        super.onBlockRegistered(l, isPlacing);
    }

    @Override
    public void onBlockUnregistered(Location l) {
        SensibleToolboxPlugin plugin = ((SensibleToolboxPlugin) getProviderPlugin());
        SoundMufflerListener listener = plugin.getSoundMufflerListener();
        listener.unregisterMuffler(this);

        super.onBlockUnregistered(l);
    }

    @Override
    public void onInteractBlock(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !e.getPlayer().isSneaking()) {
            getGUI().show(e.getPlayer());
        }

        super.onInteractBlock(e);
    }

    @Override
    public int getTickRate() {
        return 40;
    }

    @Override
    public String[] getSignLabel(BlockFace face) {
        String[] label = super.getSignLabel(face);
        label[1] = ChatColor.DARK_RED + "Volume " + ChatColor.WHITE + getVolume();
        return label;
    }
}
