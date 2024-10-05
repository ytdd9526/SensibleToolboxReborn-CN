package io.github.thebusybiscuit.sensibletoolbox.blocks.machines;

import java.util.Arrays;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.google.common.base.Objects;

import io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin;
import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.InventoryGUI;
import io.github.thebusybiscuit.sensibletoolbox.api.gui.SlotType;
import io.github.thebusybiscuit.sensibletoolbox.core.IDTracker;
import io.github.thebusybiscuit.sensibletoolbox.core.energy.SCURelayConnection;
import io.github.thebusybiscuit.sensibletoolbox.items.components.SubspaceTransponder;
import io.github.thebusybiscuit.sensibletoolbox.items.components.UnlinkedSCURelay;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.text.LogUtils;

public class SCURelay extends BatteryBox {

    private static final int TRANSPONDER_LABEL_SLOT = 43;
    private static final int TRANSPONDER_SLOT = 44;
    private UUID worldID = null;
    private int relayId = 0;
    private boolean hasTransponder;

    public SCURelay() {
        super();
    }

    public SCURelay(ConfigurationSection conf) {
        super(conf);
        relayId = conf.getInt("relayId");
        hasTransponder = conf.getBoolean("transponder", false);
        IDTracker<SCURelayConnection> tracker = getTracker();

        if (!tracker.contains(relayId)) {
            SCURelayConnection relayData = new SCURelayConnection();
            relayData.setChargeLevel(super.getCharge());
            tracker.add(relayId, relayData);
        }
    }

    @Nonnull
    private IDTracker<SCURelayConnection> getTracker() {
        return ((SensibleToolboxPlugin) getProviderPlugin()).getScuRelayIDTracker();
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        conf.set("relayId", relayId);
        conf.set("transponder", hasTransponder);
        return conf;
    }

    @Override
    public Material getMaterial() {
        return Material.CYAN_STAINED_GLASS;
    }

    @Override
    public String getItemName() {
        return "SCU Relay";
    }

    @Override
    public String getDisplaySuffix() {
        return relayId > 0 ? "#" + relayId : null;
    }

    @Override
    public String[] getExtraLore() {
        String[] lore = super.getExtraLore();

        if (relayId == 0) {
            return lore;
        }

        String[] res = Arrays.copyOf(lore, lore.length + 4);
        res[lore.length] = "Comes in pairs: both partners";
        res[lore.length + 1] = "always have the same SCU level.";
        res[lore.length + 2] = "Displayed charge may be out of date";
        res[lore.length + 3] = "L-Click to refresh";
        return res;
    }

    @Override
    public Recipe getMainRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(getKey(), toItemStack(2));
        UnlinkedSCURelay usr = new UnlinkedSCURelay();
        registerCustomIngredients(usr);
        recipe.addIngredient(usr.getMaterial());
        recipe.addIngredient(usr.getMaterial());
        return recipe;
    }

    @Override
    public int getMaxCharge() {
        // takes two 50k battery boxes to make, so...
        return 100000;
    }

    @Override
    public int getChargeRate() {
        if (!isRedstoneActive() || relayId == 0) {
            return 0;
        }

        SCURelayConnection connection = getTracker().get(relayId);

        if (connection == null) {
            return 0;
        }

        SCURelay block1 = connection.getFirst();
        SCURelay block2 = connection.getSecond();

        if (block1 == null || block2 == null) {
            return 0;
        } else if (!Objects.equal(block1.worldID, block2.worldID) && (!block1.hasTransponder || !block2.hasTransponder)) {
            return 0;
        } else {
            return 500;
        }
    }

    @Override
    public double getCharge() {
        SCURelayConnection relayData = getTracker().get(relayId);
        return relayData == null ? 0 : relayData.getChargeLevel();
    }

    @Override
    public void setCharge(double charge) {
        SCURelayConnection connection = getTracker().get(relayId);

        if (connection != null) {
            SCURelay block1 = connection.getFirst();
            SCURelay block2 = connection.getSecond();
            connection.setChargeLevel(charge);

            if (block1 != null) {
                block1.notifyCharge();
            }

            if (block2 != null) {
                block2.notifyCharge();
            }
        }
    }

    private void notifyCharge() {
        super.setCharge(getCharge());
    }

    @Override
    public void onInteractItem(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            updateHeldItemStack(event.getPlayer(), event.getHand());
            event.setCancelled(true);
        } else {
            super.onInteractItem(event);
        }
    }

    @Override
    public int[] getInputSlots() {
        return new int[] { TRANSPONDER_SLOT };
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] { TRANSPONDER_SLOT };
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
    public boolean onSlotClick(HumanEntity player, int slot, ClickType click, ItemStack inSlot, ItemStack onCursor) {
        boolean res = super.onSlotClick(player, slot, click, inSlot, onCursor);

        if (res) {
            rescanTransponder();
        }

        return res;
    }

    @Override
    public int onShiftClickInsert(HumanEntity player, int slot, ItemStack toInsert) {
        int inserted = super.onShiftClickInsert(player, slot, toInsert);

        if (inserted > 0) {
            rescanTransponder();
        }

        return inserted;
    }

    @Override
    public boolean onShiftClickExtract(HumanEntity player, int slot, ItemStack toExtract) {
        boolean res = super.onShiftClickExtract(player, slot, toExtract);

        if (res) {
            rescanTransponder();
        }

        return res;
    }

    @Override
    public void onGUIOpened(HumanEntity player) {
        drawTransponder(getGUI());
    }

    private void drawTransponder(InventoryGUI gui) {
        if (hasTransponder) {
            gui.setItem(TRANSPONDER_SLOT, new SubspaceTransponder().toItemStack());
        } else {
            gui.setItem(TRANSPONDER_SLOT, null);
        }
    }

    @Override
    public boolean acceptsItemType(ItemStack item) {
        return SensibleToolbox.getItemRegistry().isSTBItem(item, SubspaceTransponder.class);
    }

    @Override
    public int insertItems(ItemStack toInsert, BlockFace side, boolean sorting, UUID uuid) {
        int n = super.insertItems(toInsert, side, sorting, uuid);

        if (n > 0) {
            rescanTransponder();
        }

        return n;
    }

    @Override
    public ItemStack extractItems(BlockFace face, ItemStack receiver, int amount, UUID uuid) {
        ItemStack s = super.extractItems(face, receiver, amount, uuid);

        if (s != null) {
            rescanTransponder();
        }

        return s;
    }

    private void rescanTransponder() {
        // defer this since we need to ensure the inventory slot is actually updated
        Bukkit.getScheduler().runTask(getProviderPlugin(), () -> {
            SubspaceTransponder str = SensibleToolbox.getItemRegistry().fromItemStack(getGUI().getItem(TRANSPONDER_SLOT), SubspaceTransponder.class);
            hasTransponder = str != null;
        });
    }

    @Override
    protected InventoryGUI createGUI() {
        InventoryGUI gui = super.createGUI();

        gui.addLabel("Subspace Transponder", TRANSPONDER_LABEL_SLOT, null, "Insert a Subspace Transponder", "here if the relay partner will", "be on a different world");
        gui.setSlotType(TRANSPONDER_SLOT, SlotType.ITEM);

        drawTransponder(gui);

        return gui;
    }

    @Override
    protected boolean shouldPaintSlotSurrounds() {
        return false;
    }

    private void updateInfoLabel(@Nonnull SCURelayConnection connection) {
        String locStr = "(unknown)";
        SCURelay block1 = connection.getFirst();
        SCURelay block2 = connection.getSecond();

        if (this.equals(block1)) {
            locStr = block2 == null ? "(not placed)" : MiscUtil.formatLocation(block2.getLocation());
        } else if (this.equals(block2)) {
            locStr = block1 == null ? "(not placed)" : MiscUtil.formatLocation(block1.getLocation());
        }

        getGUI().addLabel("SCU Relay : #" + relayId, 0, null, ChatColor.DARK_AQUA + "Partner Location: " + locStr, "Relay will only accept/supply power", "when both partners are placed");
    }

    @Override
    public int getInventoryGUISize() {
        return 45;
    }

    @Override
    public boolean onCrafted() {
        relayId = getTracker().add(new SCURelayConnection());
        return true;
    }

    @Override
    protected String[] getSignLabel(BlockFace face) {
        String[] label = super.getSignLabel(face);
        label[1] = ChatColor.DARK_RED + "ID #" + relayId;
        return label;
    }

    @Override
    public void onBlockRegistered(Location l, boolean isPlacing) {
        super.onBlockRegistered(l, isPlacing);
        SCURelayConnection connection = getTracker().get(relayId);
        SCURelay block1 = connection.getFirst();
        SCURelay block2 = connection.getSecond();

        if (block1 == null) {
            connection.setFirstBlock(this);
        } else if (block2 == null) {
            connection.setSecondBlock(this);
        } else {
            // This shouldn't happen!
            LogUtils.warning("trying to register more than 2 SCU relays of ID " + relayId);
        }

        updateInfoLabels(connection);
        worldID = l.getWorld().getUID();
    }

    @Override
    public void onBlockUnregistered(Location l) {
        getGUI().setItem(TRANSPONDER_SLOT, null);

        SCURelayConnection connection = getTracker().get(relayId);

        if (connection != null) {
            SCURelay block1 = connection.getFirst();
            SCURelay block2 = connection.getSecond();

            if (this.equals(block1)) {
                connection.setFirstBlock(null);
            } else if (this.equals(block2)) {
                connection.setSecondBlock(null);
            } else {
                // shouldn't happen!
                LogUtils.warning("relay loc for ID " + relayId + " doesn't match placed relays");
            }

            updateInfoLabels(connection);
        } else {
            // shouldn't happen!
            LogUtils.warning("can't find any register SCU relay of ID " + relayId);
        }

        worldID = null;

        super.onBlockUnregistered(l);
    }

    private void updateInfoLabels(@Nonnull SCURelayConnection connection) {
        SCURelay block1 = connection.getFirst();
        SCURelay block2 = connection.getSecond();

        if (block1 != null) {
            block1.updateInfoLabel(connection);
        }

        if (block2 != null) {
            block2.updateInfoLabel(connection);
        }
    }
}
