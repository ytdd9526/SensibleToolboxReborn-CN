package io.github.thebusybiscuit.sensibletoolbox.items;

import java.util.UUID;

import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBItem;
import io.github.thebusybiscuit.sensibletoolbox.items.components.SimpleCircuit;
import me.desht.dhutils.MiscUtil;

public class LandMarker extends BaseSTBItem {

    private Location l;

    public LandMarker() {
        l = null;
    }

    public LandMarker(ConfigurationSection conf) {
        if (conf.contains("worldId")) {
            UUID worldId = UUID.fromString(conf.getString("worldId"));
            World w = Bukkit.getWorld(worldId);
            if (w != null) {
                l = new Location(w, conf.getInt("x"), conf.getInt("y"), conf.getInt("z"));
            } else {
                l = null;
            }
        } else {
            l = null;
        }
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();

        if (l != null) {
            conf.set("worldId", l.getWorld().getUID().toString());
            conf.set("x", l.getBlockX());
            conf.set("y", l.getBlockY());
            conf.set("z", l.getBlockZ());
        }

        return conf;
    }

    @Override
    public Material getMaterial() {
        return Material.FIREWORK_ROCKET;
    }

    @Override
    public String getItemName() {
        return "Land Marker";
    }

    @Override
    public String[] getLore() {
        return new String[] { "Stores positions via Sensible GPS", "R-Click block: store position", "R-Click air: clear position" };
    }

    @Override
    public Recipe getMainRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(getKey(), toItemStack());
        recipe.shape(" T ", " C ", " S ");
        SimpleCircuit sc = new SimpleCircuit();
        registerCustomIngredients(sc);
        recipe.setIngredient('T', Material.REDSTONE_TORCH);
        recipe.setIngredient('C', sc.getMaterial());
        recipe.setIngredient('S', Material.STICK);
        return recipe;
    }

    @Override
    public String getDisplaySuffix() {
        return l == null ? null : MiscUtil.formatLocation(l);
    }

    public Location getMarkedLocation() {
        return l;
    }

    public void setMarkedLocation(Location l) {
        this.l = l;
    }

    @Override
    public void onInteractItem(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_AIR) && getMarkedLocation() != null) {
            setMarkedLocation(null);
            updateHeldItemStack(e.getPlayer(), e.getHand());
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.6F);
        } else if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) && !e.getClickedBlock().getLocation().equals(l)) {
            BaseSTBBlock stb = SensibleToolbox.getBlockAt(e.getClickedBlock().getLocation());
            if (stb == null) {
                setMarkedLocation(e.getClickedBlock().getLocation());
                updateHeldItemStack(e.getPlayer(), e.getHand());
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
            } else {
                stb.onInteractBlock(e);
            }
        }
        e.setCancelled(true);
    }
}
