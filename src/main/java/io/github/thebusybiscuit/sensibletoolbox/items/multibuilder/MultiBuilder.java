package io.github.thebusybiscuit.sensibletoolbox.items.multibuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.sensibletoolbox.SensibleToolboxPlugin;
import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.api.energy.Chargeable;
import io.github.thebusybiscuit.sensibletoolbox.api.items.BaseSTBItem;
import io.github.thebusybiscuit.sensibletoolbox.items.components.IntegratedCircuit;
import io.github.thebusybiscuit.sensibletoolbox.items.energycells.TenKEnergyCell;
import io.github.thebusybiscuit.sensibletoolbox.utils.STBUtil;
import io.github.thebusybiscuit.sensibletoolbox.utils.UnicodeSymbol;
import io.github.thebusybiscuit.sensibletoolbox.utils.VanillaInventoryUtils;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.cost.ItemCost;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

public class MultiBuilder extends BaseSTBItem implements Chargeable {

    public static final int MAX_BUILD_BLOCKS = 4;
    public static final int MAX_TOTAL_BLOCKS = 16;
    public static final int DEF_SCU_PER_OPERATION = 40;
    private static final Map<UUID, LinkedBlockingQueue<SwapRecord>> swapQueues = new HashMap<>();
    private BuildingMode mode;
    private double charge;
    private Material material;
    private boolean isBuilding = false;

    public MultiBuilder() {
        super();
        mode = BuildingMode.BUILD;
        charge = 0;
    }

    public MultiBuilder(ConfigurationSection conf) {
        super(conf);
        mode = BuildingMode.valueOf(conf.getString("mode"));
        charge = conf.getDouble("charge");
        String s = conf.getString("material");
        material = s.isEmpty() ? null : Material.matchMaterial(s);
    }

    public BuildingMode getMode() {
        return mode;
    }

    public void setMode(BuildingMode mode) {
        this.mode = mode;
    }

    public double getCharge() {
        return charge;
    }

    public void setCharge(double charge) {
        this.charge = charge;
    }

    public int getMaxCharge() {
        return 10000;
    }

    @Override
    public int getChargeRate() {
        return 500;
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration map = super.freeze();
        map.set("mode", mode.toString());
        map.set("charge", charge);
        map.set("material", material == null ? "" : material.name());
        return map;
    }

    @Override
    public String getItemName() {
        return "Multibuilder";
    }

    @Override
    public String[] getLore() {
        return switch (getMode()) {
            case BUILD ->
                new String[]{"L-click block: " + ChatColor.WHITE + "preview", "R-click block: " + ChatColor.WHITE + "build", UnicodeSymbol.ARROW_UP.toUnicode() + " + R-click block: " + ChatColor.WHITE + "build one", ChatColor.YELLOW + UnicodeSymbol.ARROW_UP.toUnicode() + " + R-click air: Exchange mode"};
            case EXCHANGE ->
                new String[]{"L-click block: " + ChatColor.WHITE + "set target block", "R-click block: " + ChatColor.WHITE + "swap many blocks", UnicodeSymbol.ARROW_UP.toUnicode() + " + R-click block: " + ChatColor.WHITE + "swap one block", ChatColor.YELLOW + UnicodeSymbol.ARROW_UP.toUnicode() + " + R-click air: Build mode"};
        };
    }

    @Override
    public String[] getExtraLore() {
        return new String[]{STBUtil.getChargeString(this)};
    }

    @Override
    public Recipe getMainRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(getKey(), toItemStack());
        TenKEnergyCell cell = new TenKEnergyCell();
        cell.setCharge(0.0);
        IntegratedCircuit sc = new IntegratedCircuit();
        registerCustomIngredients(cell, sc);
        recipe.shape(" DP", "CED", "I  ");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('P', Material.DIAMOND_AXE);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('E', cell.getMaterial());
        recipe.setIngredient('C', sc.getMaterial());
        return recipe;
    }

    @Override
    public Material getMaterial() {
        return Material.GOLDEN_AXE;
    }

    @Override
    public String getDisplaySuffix() {
        switch (getMode()) {
            case BUILD:
                return "Build";
            case EXCHANGE:
                String s = material == null ? "" : ItemUtils.getItemName(new ItemStack(material));
                return "Exchange " + s;
            default:
                return null;
        }
    }

    @Override
    public void onInteractItem(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR && event.getPlayer().isSneaking()) {
            if (getMode() == BuildingMode.BUILD) {
                setMode(BuildingMode.EXCHANGE);
            } else if (getMode() == BuildingMode.EXCHANGE) {
                setMode(BuildingMode.BUILD);
            }
        }

        updateHeldItemStack(event.getPlayer(), EquipmentSlot.HAND);

        switch (getMode()) {
            case BUILD -> handleBuildMode(event);
            case EXCHANGE -> handleExchangeMode(event);
            default -> {
            }
        }
    }

    @Override
    public void onItemHeld(PlayerItemHeldEvent event) {
        int delta = event.getNewSlot() - event.getPreviousSlot();

        if (delta == 0) {
            return;
        } else if (delta >= 6) {
            delta -= 9;
        } else if (delta <= -6) {
            delta += 9;
        }

        delta = (delta > 0) ? 1 : -1;
        int o = getMode().ordinal() + delta;

        if (o < 0) {
            o = BuildingMode.values().length - 1;
        } else if (o >= BuildingMode.values().length) {
            o = 0;
        }

        setMode(BuildingMode.values()[o]);
        updateHeldItemStack(event.getPlayer(), EquipmentSlot.HAND);
    }

    @EventHandler
    private void handleExchangeMode(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            material = clicked.getType();
            updateHeldItemStack(player, event.getHand());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && material != null) {
            event.setCancelled(true);

            int amount = howMuchDoesPlayerHave(player, material);
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "You do not have any " + ChatColor.WHITE + material.name() + ChatColor.RED + " to exchange!");
                return;
            }

            if (player.isSneaking()) {
                startSwap(player, event.getItem(), this, clicked, material, 0);
            } else if (material != null) {
                int sharpness = event.getItem().getEnchantmentLevel(Enchantment.SHARPNESS);
                int layers = MAX_BUILD_BLOCKS + sharpness * 3;

                int exchangedCount = Math.min(layers, MAX_TOTAL_BLOCKS);

                startSwap(player, event.getItem(), this, clicked, material, exchangedCount - 1);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void startSwap(Player player,
                           ItemStack item,
                           MultiBuilder builder,
                           Block origin,
                           Material target,
                           int maxBlocks
    ) {
        LinkedBlockingQueue<SwapRecord> queue = swapQueues.get(player.getWorld().getUID());

        if (queue == null) {
            queue = new LinkedBlockingQueue<>();
            swapQueues.put(player.getWorld().getUID(), queue);
        }

        if (queue.isEmpty()) {
            new QueueSwapper(queue).runTaskTimer(SensibleToolbox.getInstance(), 1L, 1L);
        }

        if (maxBlocks < 0) {
            maxBlocks = 0;
        }

        int chargePerOp = getItemConfig().getInt("scu_per_op", DEF_SCU_PER_OPERATION);
        double chargeNeeded = chargePerOp * Math.pow(0.8, item.getEnchantmentLevel(Enchantment.EFFICIENCY));

        if (player.getGameMode() == GameMode.CREATIVE) {
            chargeNeeded = 0;
        }

        Debugger.getInstance().debug("Starting swap: Player=" + player.getName() + ", MaxBlocks=" + maxBlocks);

        queue.add(new SwapRecord(
            player,
            origin,
            origin.getType(),
            target,
            maxBlocks,
            builder,
            -1,
            chargeNeeded,
            player.getFacing()
        ));
    }

    private int howMuchDoesPlayerHave(Player p, Material mat) {
        int amount = 0;

        for (ItemStack stack : p.getInventory()) {
            if (stack != null && !stack.hasItemMeta() && stack.getType() == mat) {
                amount += stack.getAmount();
            }
            if (p.getGameMode() == GameMode.CREATIVE) {
                amount = 64;
            }
        }

        return amount;
    }

    protected boolean canReplace(Player player, Block b) {
        // Check for non-replaceable block types.
        // STB Blocks
        if (SensibleToolbox.getBlockAt(b.getLocation(), true) != null) {
            return false;
            // Vanilla inventories
        } else if (VanillaInventoryUtils.isVanillaInventory(b)) {
            return false;
            // Slimefun Blocks
        } else if (SensibleToolboxPlugin.getInstance().isSlimefunEnabled() && BlockStorage.hasBlockInfo(b)) {
            return false;
            // Unbreakable Blocks
        } else if (b.getType().getHardness() >= 3600000) {
            return false;
        } else {
            // Block is replaceable, return permission to break
            return SensibleToolbox.getProtectionManager().hasPermission(player, b, Interaction.BREAK_BLOCK);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void handleBuildMode(PlayerInteractEvent event) {

        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();

        if (isBuilding) {
            return;
        }

        isBuilding = true;

        try {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Set<Block> blocks = getBuildCandidates(
                    player,
                    event.getItem(),
                    event.getClickedBlock(),
                    event.getBlockFace()
                );

                if (!blocks.isEmpty()) {
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        doBuild(
                            player,
                            event.getHand(),
                            event.getItem(),
                            event.getClickedBlock(),
                            blocks,
                            event.getBlockFace()
                        );
                    } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        showBuildPreview(player, blocks);
                    }
                }
                event.setCancelled(true);
            }
        } finally {
            isBuilding = false; // Release the lock
        }
    }

    private void showBuildPreview(Player player, Set<Block> blocks) {
        Bukkit.getScheduler().runTask(getProviderPlugin(), () -> {
            for (Block b : blocks) {
                player.sendBlockChange(b.getLocation(), Material.WHITE_STAINED_GLASS.createBlockData());
            }
        });

        Bukkit.getScheduler().runTaskLater(getProviderPlugin(), () -> {
            for (Block b : blocks) {
                player.sendBlockChange(b.getLocation(), b.getBlockData());
            }
        }, 20L);
    }

    private void doBuild(Player player,
                         EquipmentSlot hand,
                         ItemStack item,
                         Block source,
                         Set<Block> actualBlocks,
                         BlockFace face
    ) {
        int chargePerOp = getItemConfig().getInt("scu_per_op", DEF_SCU_PER_OPERATION);
        double chargeNeeded = chargePerOp * actualBlocks.size() * Math.pow(
            0.8,
            item.getEnchantmentLevel(Enchantment.EFFICIENCY)
        );

        if (player.getGameMode() != GameMode.CREATIVE) {
            if (getCharge() < chargeNeeded) {
                player.sendMessage(ChatColor.RED + "Not enough charge to build!");
                return;
            }

            setCharge(getCharge() - chargeNeeded);
            ItemCost cost = new ItemCost(source.getType(), actualBlocks.size());
            cost.apply(player);
        }

        final int[] cappedBlocks = {Math.min(actualBlocks.size(), MAX_TOTAL_BLOCKS)};

        Bukkit.getScheduler().runTaskLater(getProviderPlugin(), () -> {
            for (Block b : actualBlocks) {
                b.setType(source.getType(), true);
                if (cappedBlocks[0] > 0) {
                    b.setType(source.getType(), true);
                    cappedBlocks[0]--;
                }
            }

            String direction = STBUtil.getDirectionString(face);
            player.sendMessage(ChatColor.YELLOW + "Built " + ChatColor.WHITE + actualBlocks.size() + ChatColor.YELLOW + " blocks " + direction);
        }, 2L);

        updateHeldItemStack(player, hand);
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0F, 1.0F);
    }

    @Nonnull
    private Set<Block> getBuildCandidates(Player player, ItemStack item, Block clickedBlock, BlockFace blockFace) {
        int sharpness = item.getEnchantmentLevel(Enchantment.SHARPNESS);
        int baseBlocks = MAX_BUILD_BLOCKS;

        if (sharpness > 0) {
            baseBlocks += Math.min(sharpness * 3, 12);
        }

        double chargePerOp = getItemConfig().getInt("scu_per_op", DEF_SCU_PER_OPERATION) * Math.pow(
            0.8,
            item.getEnchantmentLevel(Enchantment.EFFICIENCY)
        );
        int ch = (int) (getCharge() / chargePerOp);

        if (player.getGameMode() == GameMode.CREATIVE) {
            ch = 10000;
        }

        if (ch == 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 0.5F);
            return Collections.emptySet();
        }

        int max = Math.min(baseBlocks, ch);
        Material clickedType = clickedBlock.getType();
        max = Math.min(max, howMuchDoesPlayerHave(player, clickedType));

        return lineBuild(player, clickedBlock.getRelative(blockFace), blockFace, getBuildFaces(blockFace), max);
    }

    @Nonnull
    private Set<Block> lineBuild(Player player, Block origin, BlockFace face, BuildFace buildFace, int max) {
        Set<Block> result = new HashSet<>();
        Block nextBlock = origin;

        Location playerLocation = player.getLocation();
        double playerX = playerLocation.getX();
        double playerY = playerLocation.getY();
        double playerZ = playerLocation.getZ();

        double playerHitboxHeight = 1.8;
        double playerHitboxWidth = 0.6;

        while (result.size() < max && nextBlock.isEmpty() && canReplace(player, nextBlock)) {
            Location blockLocation = nextBlock.getLocation().add(0.5, 0.5, 0.5);
            double blockX = blockLocation.getX();
            double blockY = blockLocation.getY();
            double blockZ = blockLocation.getZ();

            boolean intersectsPlayer = (Math.abs(playerX - blockX) < playerHitboxWidth)
                && (Math.abs(playerY - blockY) < playerHitboxHeight)
                && (Math.abs(playerZ - blockZ) < playerHitboxWidth);

            if (intersectsPlayer) {
                break;
            }

            result.add(nextBlock);
            nextBlock = nextBlock.getRelative(face);
        }

        return result;
    }

    @Nonnull
    private BuildFace getBuildFaces(@Nonnull BlockFace face) {
        return switch (face) {
            case NORTH, SOUTH -> BuildFace.NORTH_SOUTH;
            case EAST, WEST -> BuildFace.EAST_WEST;
            case UP, DOWN -> BuildFace.UP_DOWN;
            default -> throw new IllegalArgumentException("invalid face: " + face);
        };
    }

}
