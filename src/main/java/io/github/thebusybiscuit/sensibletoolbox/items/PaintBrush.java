package io.github.thebusybiscuit.sensibletoolbox.items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Art;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.Colorable;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.GUIUtil;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.InventoryGUI;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.gadgets.ButtonGadget;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBBlock;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBItem;
import io.github.thebusybiscuit.sensibletoolbox.blocks.PaintCan;
import io.github.thebusybiscuit.sensibletoolbox.utils.HoloMessage;
import io.github.thebusybiscuit.sensibletoolbox.utils.STBUtil;
import io.github.thebusybiscuit.sensibletoolbox.utils.UnicodeSymbol;

import me.desht.dhutils.Debugger;

public class PaintBrush extends BaseSTBItem {

    private int paintLevel;
    private DyeColor color;

    public PaintBrush() {
        super();
        color = DyeColor.WHITE;
        paintLevel = 0;
    }

    public PaintBrush(ConfigurationSection conf) {
        super(conf);
        setPaintLevel(conf.getInt("paintLevel"));
        setColor(DyeColor.valueOf(conf.getString("color")));
    }

    public int getMaxPaintLevel() {
        return 25;
    }

    public int getPaintLevel() {
        return paintLevel;
    }

    public void setPaintLevel(int paintLevel) {
        this.paintLevel = paintLevel;
    }

    @Nonnull
    public DyeColor getColor() {
        return color;
    }

    public void setColor(@Nonnull DyeColor color) {
        this.color = color;
    }

    @Override
    public boolean isEnchantable() {
        return false;
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration res = super.freeze();
        res.set("paintLevel", paintLevel);
        res.set("color", color == null ? "" : color.toString());
        return res;
    }

    @Override
    public Material getMaterial() {
        return Material.GOLDEN_SHOVEL;
    }

    @Override
    public String getItemName() {
        return "Paintbrush";
    }

    @Override
    public String[] getLore() {
        return new String[] { "Paints colorable blocks:", " Wool, carpet, stained clay/glass", "R-click block: paint up to " + getMaxBlocksAffected() + " blocks", UnicodeSymbol.ARROW_UP.toUnicode() + " + R-click block: paint block", UnicodeSymbol.ARROW_UP.toUnicode() + " + R-click air: empty brush", "R-click paintings: Change artwork", };
    }

    @Override
    public ItemStack toItemStack(int amount) {
        ItemStack s = super.toItemStack(amount);
        STBUtil.levelToDurability(s, getPaintLevel(), getMaxPaintLevel());
        return s;
    }

    @Override
    public String getDisplaySuffix() {
        return getPaintLevel() > 0 ? getPaintLevel() + " " + STBUtil.dyeColorToChatColor(getColor()) + getColor() : null;
    }

    @Override
    public Recipe getMainRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(getKey(), toItemStack());
        recipe.shape("R", "S", "S");
        recipe.setIngredient('R', Material.STRING);
        recipe.setIngredient('S', Material.STICK);
        return recipe;
    }

    protected int getMaxBlocksAffected() {
        return 9;
    }

    @Override
    public void onInteractItem(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            BaseSTBBlock stb = SensibleToolbox.getBlockAt(b.getLocation(), true);

            if (stb instanceof PaintCan) {
                refillFromCan((PaintCan) stb);
                e.setCancelled(true);
            } else if (okToColor(b, stb)) {
                int painted = paint(p, b);

                if (painted > 0) {
                    p.playSound(p.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.0F, 1.5F);
                }
            }
        } else if (e.getAction() == Action.RIGHT_CLICK_AIR && p.isSneaking()) {
            setPaintLevel(0);
        }

        updateHeldItemStack(e.getPlayer(), e.getHand());
        e.setCancelled(true);
    }

    private int paint(@Nonnull Player p, @Nonnull Block b) {
        // Bukkit Colorable interface doesn't cover all colorable blocks at this time, only Wool
        if (p.isSneaking()) {
            // paint a single block
            return paintBlocks(p, b);
        } else {
            // paint multiple blocks around the clicked block
            Block[] blocks = findBlocksAround(b);
            return paintBlocks(p, blocks);
        }
    }

    private boolean okToColor(Block b, BaseSTBBlock stb) {
        if (stb != null && !(stb instanceof Colorable)) {
            // we don't want blocks which happen to use a
            // Colorable material to be paintable
            return false;
        }

        if (getBlockColor(b) == getColor() || getPaintLevel() <= 0) {
            return false;
        }

        return STBUtil.isColorable(b.getType()) || b.getType() == Material.GLASS || b.getType() == Material.GLASS_PANE;
    }

    private void refillFromCan(@Nonnull PaintCan can) {
        int needed;

        if (this.getColor() == can.getColor()) {
            needed = this.getMaxPaintLevel() - this.getPaintLevel();
        } else {
            this.setPaintLevel(0);
            needed = this.getMaxPaintLevel();
        }

        int actual = Math.min(needed, can.getPaintLevel());
        Debugger.getInstance().debug(can + " has " + can.getPaintLevel() + " of " + can.getColor() + "; " + "try to fill brush with " + needed + ", actual = " + actual);

        if (actual > 0) {
            this.setColor(can.getColor());
            this.setPaintLevel(this.getPaintLevel() + actual);
            can.setPaintLevel(can.getPaintLevel() - actual);
            Debugger.getInstance().debug("brush now = " + this.getPaintLevel() + " " + this.getColor() + ", can now = " + can.getPaintLevel() + " " + can.getColor());
        }
    }

    @Override
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!e.getHand().equals(EquipmentSlot.HAND)) {
            return;
        }
        e.setCancelled(true);

        if (getPaintLevel() <= 0) {
            return;
        }

        Entity ent = e.getRightClicked();
        int paintUsed = 0;

        if (ent instanceof Colorable) {
            ((Colorable) ent).setColor(getColor());
            paintUsed = 1;
        } else if (ent instanceof Painting) {
            Art art = ((Painting) ent).getArt();

            if (getPaintLevel() >= art.getBlockHeight() * art.getBlockWidth()) {
                openArtworkMenu(e.getPlayer(), e.getHand(), (Painting) e);
            } else {
                Location l = ent.getLocation().add(0, -art.getBlockHeight() / 2.0, 0);
                HoloMessage.popup(e.getPlayer(), l, ChatColor.RED + "Not enough paint!");
            }
        } else if (e instanceof Wolf) {
            Wolf wolf = (Wolf) e;
            wolf.setCollarColor(getColor());
            paintUsed = 1;
        }

        if (paintUsed > 0) {
            setPaintLevel(getPaintLevel() - paintUsed);
            updateHeldItemStack(e.getPlayer(), e.getHand());
            e.getPlayer().playSound(ent.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.0F, 1.5F);
        }
    }

    @Nonnull
    private Block[] findBlocksAround(@Nonnull Block b) {
        Set<Block> blocks = new HashSet<>();
        find(b, b.getType(), blocks, getMaxBlocksAffected());
        return blocks.toArray(new Block[0]);
    }

    private void find(Block b, Material mat, Set<Block> blocks, int max) {
        if (b.getType() != mat || getBlockColor(b) == getColor() || blocks.contains(b) || blocks.size() > max) {
            return;
        }

        blocks.add(b);
        find(b.getRelative(BlockFace.UP), mat, blocks, max);
        find(b.getRelative(BlockFace.EAST), mat, blocks, max);
        find(b.getRelative(BlockFace.NORTH), mat, blocks, max);
        find(b.getRelative(BlockFace.SOUTH), mat, blocks, max);
        find(b.getRelative(BlockFace.WEST), mat, blocks, max);
        find(b.getRelative(BlockFace.DOWN), mat, blocks, max);
    }

    @Nullable
    private DyeColor getBlockColor(@Nonnull Block b) {
        if (STBUtil.isColorable(b.getType())) {
            String name = b.getType().name();
            String color = name.split("_")[0];
            if (color.equals("LIGHT")) {
                color += "_" + name.split("_")[1];
            }
            return DyeColor.valueOf(color);
        } else {
            return null;
        }
    }

    private int paintBlocks(@Nonnull Player p, Block... blocks) {
        int painted = 0;

        for (Block b : blocks) {
            if (!SensibleToolbox.getProtectionManager().hasPermission(p, b, Interaction.PLACE_BLOCK)) {
                continue;
            }

            Debugger.getInstance().debug(2, "painting! " + b + " " + getPaintLevel() + " " + getColor());

            if (b.getType() == Material.GLASS) {
                b.setType(Material.WHITE_STAINED_GLASS);
            } else if (b.getType() == Material.GLASS_PANE) {
                b.setType(Material.WHITE_STAINED_GLASS_PANE);
            } else {
                if (!STBUtil.isColorable(b.getType())) {
                    continue;
                }

                String name = b.getType().name();
                String oldCol = name.split("_")[0];
                if (oldCol.equals("LIGHT")) {
                    oldCol += "_" + name.split("_")[1];
                }
                name = name.replace(oldCol, getColor().name());
                b.setType(Material.valueOf(name));
            }

            painted++;
            setPaintLevel(getPaintLevel() - 1);

            if (getPaintLevel() <= 0) {
                break;
            }
        }
        return painted;
    }

    private void openArtworkMenu(@Nonnull Player p, @Nonnull EquipmentSlot hand, @Nonnull Painting painting) {
        Painting editingPainting = painting;

        Art[] other = getOtherArt(painting.getArt());
        InventoryGUI menu = GUIUtil.createGUI(p, this, 9, ChatColor.DARK_PURPLE + "Select Artwork");

        int i = 0;
        for (Art art : other) {
            menu.addGadget(new ButtonGadget(menu, i, new CustomItemStack(Material.PAINTING, art.name(), "", "&7Click to select this artwork"), new Runnable() {

                @Override
                public void run() {
                    editingPainting.setArt(art);
                    setPaintLevel(getPaintLevel() - art.getBlockWidth() * art.getBlockHeight());
                    updateHeldItemStack(p, hand);
                    p.playSound(editingPainting.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.0F, 1.5F);
                    p.closeInventory();
                }
            }));
            i++;
        }
        menu.show(p);
    }

    @Nonnull
    private static Art[] getOtherArt(@Nonnull Art art) {
        List<Art> l = new ArrayList<>();

        for (Art a : Art.values()) {
            if (a.getBlockWidth() == art.getBlockWidth() && a.getBlockHeight() == art.getBlockHeight() && a != art) {
                l.add(a);
            }
        }

        return l.toArray(new Art[0]);
    }
}
