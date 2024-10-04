package io.github.thebusybiscuit.sensibletoolbox.items;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import me.desht.dhutils.MiscUtil;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.GUIUtil;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.InventoryGUI;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.SlotType;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBItem;
import io.github.thebusybiscuit.sensibletoolbox.utils.STBUtil;

import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.CuboidDirection;


public abstract class CombineHoe extends BaseSTBItem {


    private Material seedType;
    private int seedAmount;
    private InventoryGUI gui;
    private EquipmentSlot equipmentSlot;

    @Nonnull
    public static String getInventoryTitle() {
        return ChatColor.DARK_GREEN + "Seed Bag";
    }

    protected CombineHoe() {
        super();
        seedType = null;
        seedAmount = 0;
    }

    protected CombineHoe(ConfigurationSection conf) {
        super(conf);
        setSeedAmount(conf.getInt("amount"));
        setSeedType(Material.getMaterial(conf.getString("seeds")));
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        conf.set("amount", getSeedAmount());
        conf.set("seeds", getSeedType() == null ? "" : getSeedType().toString());
        return conf;
    }

    public Material getSeedType() {
        return seedType;
    }

    public void setSeedType(Material seedType) {
        this.seedType = seedType;
    }

    public int getSeedAmount() {
        return seedAmount;
    }

    public void setSeedAmount(int seedAmount) {
        this.seedAmount = seedAmount;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public void setEquipmentSlot(EquipmentSlot equipmentSlot) {
        this.equipmentSlot = equipmentSlot;
    }

    @Override
    public boolean isEnchantable() {
        return false;
    }

    @Override
    public String[] getLore() {
        int n = getWorkRadius() * 2 + 1;
        String s = n + "x" + n;
        String t = n + "x" + n + "x" + n;
        return new String[] { "Right-click dirt/grass:" + ChatColor.WHITE + " till " + s + " area", "Right-click soil:" + ChatColor.WHITE + " sow " + s + " area", "Right-click air:" + ChatColor.WHITE + " open seed bag", "Left-click plants:" + ChatColor.WHITE + " harvest " + s + " area", "Left-click leaves:" + ChatColor.WHITE + " break " + t + " area", };
    }

    @Override
    public String[] getExtraLore() {
        if (getSeedType() != null && getSeedAmount() > 0) {
            String s = ItemUtils.getItemName(new ItemStack(getSeedType()));
            return new String[] { ChatColor.WHITE + "Seed bag: " + ChatColor.GOLD + getSeedAmount() + " x " + s };
        } else {
            return new String[0];
        }
    }

    @Override
    public boolean hasGlow() {
        return false;
    }

    @Override
    public void onInteractItem(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        setEquipmentSlot(e.getHand());

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (b.getType() == Material.FARMLAND) {
                    plantSeeds(e.getPlayer(), b);
                    e.setCancelled(true);
                    return;
                } else if (b.getType() == Material.DIRT || b.getType() == Material.GRASS_BLOCK) {
                    tillSoil(e.getPlayer(), e.getItem(), e.getHand(), b);
                    e.setCancelled(true);
                    return;
            }
        }


        if (e.getAction() == Action.RIGHT_CLICK_AIR) {
            if (e.getClickedBlock() == null || !e.getClickedBlock().getType().isInteractable()) {
                gui = GUIUtil.createGUI(e.getPlayer(), this, 9, getInventoryTitle());

                for (int i = 0; i < gui.getInventory().getSize(); i++) {
                    gui.setSlotType(i, SlotType.ITEM);
                }

                populateSeedBag(gui);
                gui.show(e.getPlayer());
            }
        }
    }

    @Override
    public void onBreakBlockWithItem(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (p.isSneaking()) {
            return;
        }

        Block b = e.getBlock();
            if (Tag.LEAVES.isTagged(b.getType())) {
                if (!p.isSneaking()) {
                    harvestLayer(p, b);
                }
                ItemUtils.damageItem(p.getInventory().getItemInMainHand(), false);
            } else if (STBUtil.isPlant(b.getType())) {
                harvestLayer(p, b);
                ItemUtils.damageItem(p.getInventory().getItemInMainHand(), false);
            }
        }


    private boolean verifyUnique(Inventory inv, ItemStack s, int exclude) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != exclude && inv.getItem(i) != null && inv.getItem(i).getType() != s.getType()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onSlotClick(HumanEntity p, int slot, ClickType click, ItemStack inSlot, ItemStack onCursor) {
        return onCursor.getType() == Material.AIR || STBUtil.getCropType(onCursor.getType()) != null && verifyUnique(gui.getInventory(), onCursor, slot);
    }

    @Override
    public boolean onPlayerInventoryClick(HumanEntity p, int slot, ClickType click, ItemStack inSlot, ItemStack onCursor) {
        return true;
    }

    @Override
    public int onShiftClickInsert(HumanEntity p, int slot, ItemStack toInsert) {
        if (STBUtil.getCropType(toInsert.getType()) == null) {
            return 0;
        } else if (!verifyUnique(gui.getInventory(), toInsert, slot)) {
            return 0;
        } else {
            Map<Integer, ItemStack> excess = gui.getInventory().addItem(toInsert);
            int inserted = toInsert.getAmount();

            for (ItemStack s : excess.values()) {
                inserted -= s.getAmount();
            }

            return inserted;
        }
    }

    @Override
    public boolean onShiftClickExtract(HumanEntity p, int slot, ItemStack toExtract) {
        return true;
    }

    @Override
    public boolean onClickOutside(HumanEntity p) {
        return false;
    }

    @Override
    public void onGUIClosed(HumanEntity p) {
        Material seeds = null;
        int count = 0;
        String err = null;

        for (int i = 0; i < gui.getInventory().getSize(); i++) {
            ItemStack s = gui.getInventory().getItem(i);

            if (s != null) {
                if (seeds != null && seeds != s.getType()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), s);
                    err = "Mixed items in the seed bag?";
                } else if (STBUtil.getCropType(s.getType()) == null) {
                    p.getWorld().dropItemNaturally(p.getLocation(), s);
                    err = "Non-seed items in the seed bag?";
                } else {
                    seeds = s.getType();
                    count += s.getAmount();
                }
            }
        }

        if (err != null) {
            STBUtil.complain((Player) p, err);
        }

        setSeedAmount(count);
        setSeedType(seeds);
        updateHeldItemStack((Player) p, getEquipmentSlot());
    }

    private void populateSeedBag(InventoryGUI gui) {
        Inventory inv = gui.getInventory();

        if (getSeedType() != null && getSeedAmount() > 0) {
            int nFullStacks = getSeedAmount() / getSeedType().getMaxStackSize();
            int remainder = getSeedAmount() % getSeedType().getMaxStackSize();

            for (int i = 0; i < nFullStacks && i < inv.getSize(); i++) {
                inv.setItem(i, new ItemStack(getSeedType(), getSeedType().getMaxStackSize()));
            }

            if (remainder > 0 && nFullStacks < inv.getSize()) {
                inv.setItem(nFullStacks, new ItemStack(getSeedType(), remainder));
            }
        }
    }

    private void plantSeeds(Player p, Block b) {
        Cuboid cuboid = new Cuboid(b.getLocation());
        cuboid = cuboid.outset(CuboidDirection.HORIZONTAL, getWorkRadius());

        if (getSeedType() == null || getSeedAmount() == 0) {
            return;
        }

        int amountLeft = getSeedAmount();
        for (Block block : cuboid) {

            Block above = block.getRelative(BlockFace.UP);

            if (!SensibleToolbox.getProtectionManager().hasPermission(p, above, Interaction.PLACE_BLOCK)) {
                continue;
            }

            if (block.getType() == Material.FARMLAND && above.isEmpty()) {
                // candidate for sowing
                above.setType(STBUtil.getCropType(getSeedType()));
                amountLeft--;

                if (amountLeft == 0) {
                    break;
                }
            }
        }

        if (amountLeft < getSeedAmount()) {
            setSeedAmount(amountLeft);
            updateHeldItemStack(p, getEquipmentSlot());
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.0F);
        }
    }

    @ParametersAreNonnullByDefault
    private void harvestLayer(Player p, Block b) {
        Cuboid cuboid = new Cuboid(b.getLocation());
        cuboid = cuboid.outset(CuboidDirection.BOTH, getWorkRadius());

        for (Block block : cuboid) {
            if (!block.equals(b) && (STBUtil.isPlant(block.getType()) || Tag.LEAVES.isTagged(block.getType()))) {
                if (SensibleToolbox.getProtectionManager().hasPermission(p, b, Interaction.BREAK_BLOCK)) {
                    block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
                    block.breakNaturally();
                    BlockStorage.clearBlockInfo(block);
                }
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void tillSoil(Player p, ItemStack s, EquipmentSlot hand, Block b) {
        int count = 0;
        int damage = ((Damageable) s.getItemMeta()).getDamage();

        Cuboid cuboid = new Cuboid(b.getLocation());
        cuboid = cuboid.outset(CuboidDirection.HORIZONTAL, getWorkRadius());

        for (Block b1 : cuboid) {

            if (!SensibleToolbox.getProtectionManager().hasPermission(p, b1, Interaction.BREAK_BLOCK)) {
                MiscUtil.errorMessage(p, "You do not have permission to till soil here.");
                continue;
            }

            Block above = b1.getRelative(BlockFace.UP);

            if ((b1.getType() == Material.DIRT || b1.getType() == Material.GRASS_BLOCK) && !above.getType().isSolid() && !above.isLiquid()) {
                b1.setType(Material.FARMLAND);
                count++;

                if (!above.isEmpty()) {
                    above.breakNaturally();
                }

                if (damage + count >= s.getType().getMaxDurability()) {
                    break;
                }
            }

            if (p.isSneaking()) {
                break;
            }
        }

        if (count > 0) {
            p.playSound(b.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0F, 1.0F);
            ItemUtils.damageItem(s, count, false);
        }
    }

    public int getWorkRadius() {
        return 1;
    }
}
