package io.github.thebusybiscuit.sensibletoolbox.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.filters.Filter;

import me.desht.dhutils.Debugger;

/**
 * Utility methods to interact with vanilla inventories.
 *
 * @author desht
 * @author TheBusyBiscuit
 */
public final class VanillaInventoryUtils {

    private VanillaInventoryUtils() {}

    public static boolean isVanillaInventory(Block b) {
        return getVanillaInventory(b).isPresent();
    }

    /**
     * Get the vanilla inventory for the given block.
     *
     * @param target
     *            the block containing the target inventory
     * @return the block's inventory, or null if the block does not have one
     */
    public static Optional<Inventory> getVanillaInventory(@Nonnull Block target) {
        BlockState state = target.getState();

        if (state instanceof InventoryHolder) {
            return Optional.of(((InventoryHolder) state).getInventory());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Attempt to insert items from the given buffer into the given block,
     * which should be a vanilla inventory holder. Items successfully
     * inserted will be removed from the buffer stack.
     *
     * @param target
     *            the block to insert into
     * @param source
     *            the item stack to take items from
     * @param amount
     *            the number of items from the buffer to insert
     * @param side
     *            the side on which insertion is occurring
     *            (some blocks care about this, e.g. furnace)
     * @param inserterId
     *            UUID of the player doing the insertion
     *            (may be null or the UUID of an offline player)
     * @return the number of items actually inserted
     */
    public static int vanillaInsertion(Block target, ItemStack source, int amount, BlockFace side, boolean sorting, UUID inserterId) {
        if (source == null || source.getAmount() == 0) {
            return 0;
        }

        if (inserterId == null || !SensibleToolbox.getProtectionManager().hasPermission(Bukkit.getOfflinePlayer(inserterId), target, Interaction.INTERACT_BLOCK)) {
            return 0;
        }

        Optional<Inventory> targetInv = getVanillaInventory(target);


        if (targetInv.isEmpty()) {
            return 0;
        } else {
            return vanillaInsertion(targetInv.get(), source, amount, side, sorting);
        }
    }

    /**
     * Attempt to insert items from the given buffer into the given inventory.
     * Items successfully inserted will be removed from the buffer stack.
     *
     * @param targetInv
     *            the inventory to insert into
     * @param source
     *            the item stack to take items from
     * @param amount
     *            the number of items from the buffer to insert
     * @param side
     *            the side on which insertion is occurring (some blocks care about this, e.g. furnace)
     * @return the number of items actually inserted
     */
    public static int vanillaInsertion(Inventory targetInv, ItemStack source, int amount, BlockFace side, boolean sorting) {
        if (targetInv != null) {
            if (sorting && !sortingOK(source, targetInv)) {
                return 0;
            }

            ItemStack s = source.clone();
            s.setAmount(Math.min(amount, s.getAmount()));
            Debugger.getInstance().debug(2, "inserting " + s + " into " + targetInv.getHolder());
            Map<Integer, ItemStack> excess;

            switch (targetInv.getType()) {
                case FURNACE:
                    if (side == BlockFace.DOWN) {
                        // no insertion from below
                        return 0;
                    }
                    excess = addToFurnace((FurnaceInventory) targetInv, s, side);
                    break;
                case BREWING:
                    if (side == BlockFace.DOWN) {
                        // no insertion from below
                        return 0;
                    }
                    excess = addToBrewingStand((BrewerInventory) targetInv, s, side);
                    break;
                default:
                    if (amount != targetInv.getMaxStackSize()) {
                        excess = targetInv.addItem(s);
                    } else {
                        excess = targetInv.addItem(source);
                    }
                    break;
            }

            if (!excess.isEmpty()) {
                int insertedAmount = s.getAmount() - excess.values().stream().mapToInt(ItemStack::getAmount).sum();
                source.setAmount(source.getAmount() - insertedAmount);
                return insertedAmount;
            } else {
                source.setAmount(source.getAmount() - s.getAmount());
                return s.getAmount();
            }
        }
        return 0;
    }

    /**
     * Attempt to pull items from an inventory into a receiving buffer.
     *
     * @param target
     *            the block containing the target inventory
     * @param amount
     *            the desired number of items
     * @param buffer
     *            an item stack into which to insert
     *            the transferred items
     * @param filter
     *            a filter to whitelist/blacklist items
     * @param pullerId
     *            UUID of the player doing the pulling
     *            (may be null or the UUID of an offline player)
     *
     * @return the items pulled, or null if nothing was pulled
     */
    @Nullable
    public static ItemStack pullFromInventory(Block target, int amount, ItemStack buffer, Filter filter, @Nullable UUID pullerId) {
        if (pullerId == null || !SensibleToolbox.getProtectionManager().hasPermission(Bukkit.getOfflinePlayer(pullerId), target, Interaction.INTERACT_BLOCK)) {
            return null;
        }

        Optional<Inventory> targetInv = getVanillaInventory(target);

        if (!targetInv.isPresent()) {
            return null;
        } else {
            return pullFromInventory(targetInv.get(), amount, buffer, filter);
        }
    }

    /**
     * Attempt to pull items from an inventory into a receiving buffer.
     *
     * @param targetInv
     *            the target inventory
     * @param amount
     *            the desired number of items
     * @param buffer
     *            an item stack into which to insert
     *            the transferred items
     * @param filter
     *            a filter to whitelist/blacklist items
     * @return the items pulled, or null if nothing was pulled
     */
    public static ItemStack pullFromInventory(Inventory targetInv, int amount, ItemStack buffer, Filter filter) {
        if (targetInv == null) {
            return null;
        }
        IntRange range = getExtractionSlots(targetInv);
        for (int slot = range.getMinimumInt(); slot <= range.getMaximumInt(); slot++) {
            ItemStack s = targetInv.getItem(slot);

            if (s != null) {
                if ((filter == null || filter.shouldPass(s)) && (buffer == null || s.isSimilar(buffer))) {
                    Debugger.getInstance().debug(2, "pulling " + s + " from " + targetInv.getHolder());
                    int toTake = Math.min(amount, s.getAmount());

                    if (buffer != null) {
                        toTake = Math.min(toTake, buffer.getType().getMaxStackSize() - buffer.getAmount());
                    }

                    if (toTake > 0) {
                        if (buffer == null) {
                            buffer = s.clone();
                            buffer.setAmount(toTake);
                        } else {
                            buffer.setAmount(buffer.getAmount() + toTake);
                        }

                        s.setAmount(s.getAmount() - toTake);
                        targetInv.setItem(slot, s.getAmount() > 0 ? s : null);
                        return buffer;
                    }
                }
            }
        }

        return null;
    }

    private static IntRange getExtractionSlots(Inventory inv) {
        switch (inv.getType()) {
            case FURNACE:
                return new IntRange(2);
            case BREWING:
                return new IntRange(0, 2);
            default:
                return new IntRange(0, inv.getSize() - 1);
        }
    }

    private static Map<Integer, ItemStack> addToBrewingStand(BrewerInventory targetInv, ItemStack s, BlockFace side) {
        Map<Integer, ItemStack> res = new HashMap<>();
        ItemStack excess = null;

        if (side == BlockFace.UP) {
            // ingredient slot
            if (!STBUtil.isPotionIngredient(s.getType())) {
                excess = s;
            } else {
                excess = putStack(targetInv, 3, s);
            }
        } else {
            // water/potion slots
            if (s.getType() != Material.GLASS_BOTTLE && s.getType() != Material.POTION) {
                excess = s;
            } else {
                for (int slot = 0; slot <= 2; slot++) {
                    excess = putStack(targetInv, slot, s);

                    if (excess == null) {
                        // all fitted
                        break;
                    } else {
                        // some or none fitted, continue with other slots
                        s.setAmount(excess.getAmount());
                    }
                }
            }
        }

        if (excess != null) {
            res.put(0, excess);
        }
        return res;
    }

    private static Map<Integer, ItemStack> addToFurnace(FurnaceInventory targetInv, ItemStack s, BlockFace side) {
        Map<Integer, ItemStack> res = new HashMap<>();

        // 0 == Smelting | 1 == Fuel
        int slot = side == BlockFace.UP ? 0 : 1;

        ItemStack excess = putStack(targetInv, slot, s);

        if (excess != null) {
            res.put(slot, excess);
        }

        return res;
    }

    /**
     * Attempt to put the given item stack in the given slot. Some or all items
     * may not fit if there's already something in the slot.
     *
     * @param inv
     *            the inventory
     * @param slot
     *            the slot to insert into
     * @param s
     *            the items to insert
     * @return the items left over, or null if nothing was left over (all inserted)
     */
    private static ItemStack putStack(Inventory inv, int slot, ItemStack s) {
        ItemStack current = inv.getItem(slot);

        if (current == null) {
            inv.setItem(slot, s);
            return null;
        } else if (current.isSimilar(s)) {
            int toAdd = Math.min(s.getAmount(), current.getType().getMaxStackSize() - current.getAmount());
            current.setAmount(current.getAmount() + toAdd);
            inv.setItem(slot, current);

            if (toAdd < s.getAmount()) {
                ItemStack leftover = s.clone();
                leftover.setAmount(s.getAmount() - toAdd);
                return leftover;
            } else {
                return null;
            }
        } else {
            return s;
        }
    }

    private static boolean sortingOK(ItemStack candidate, Inventory inv) {
        boolean isEmpty = true;

        for (ItemStack s : inv) {
            if (candidate.isSimilar(s)) {
                return true;
            } else if (s != null) {
                isEmpty = false;
            }
        }

        return isEmpty;
    }
}
