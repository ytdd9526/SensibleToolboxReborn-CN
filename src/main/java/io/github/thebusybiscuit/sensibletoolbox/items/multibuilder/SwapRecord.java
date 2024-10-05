package io.github.thebusybiscuit.sensibletoolbox.items.multibuilder;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

final class SwapRecord {

    private final Player p;
    private final Block block;
    private final Material source;
    private final Material target;
    private final int layersLeft;
    private final MultiBuilder builder;
    private final int slot;
    private final double chargeNeeded;
    private final BlockFace direction;


    @ParametersAreNonnullByDefault
    SwapRecord(Player p, Block block, Material source, Material target, int layersLeft, MultiBuilder builder, int slot, double chargeNeeded, BlockFace direction) {
        this.p = p;
        this.block = block;
        this.source = source;
        this.target = target;
        this.layersLeft = layersLeft;
        this.builder = builder;
        this.slot = slot;
        this.chargeNeeded = chargeNeeded;
        this.direction = direction;
    }

    @Nonnull
    public Player getPlayer() {
        return p;
    }

    @Nonnull
    public Block getBlock() {
        return block;
    }

    @Nonnull
    public MultiBuilder getMultiBuilder() {
        return builder;
    }

    @Nonnull
    public Material getSource() {
        return source;
    }

    @Nonnull
    public Material getTarget() {
        return target;
    }

    public int getRemainingLayers() {
        return layersLeft;
    }

    public int getSlot() {
        return slot;
    }

    public double getRequiredCharge() {
        return chargeNeeded;
    }

    public BlockFace getDirection() {
        return direction;
    }

}