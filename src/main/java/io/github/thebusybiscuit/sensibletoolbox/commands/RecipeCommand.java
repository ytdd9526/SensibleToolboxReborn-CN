package io.github.thebusybiscuit.sensibletoolbox.commands;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import io.github.thebusybiscuit.sensibletoolbox.api.SensibleToolbox;
import io.github.thebusybiscuit.sensibletoolbox.items.recipebook.RecipeBook;
import io.github.thebusybiscuit.sensibletoolbox.utils.STBUtil;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

public class RecipeCommand extends AbstractCommand {

    public RecipeCommand() {
        super("stb recipe", 0, 1);
        setUsage("/<command> recipe <filter-string>");
        setPermissionNode("stb.commands.recipe");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MiscUtil.errorMessage(sender, "This command can't be run from the console.");
            return true;
        }

        Player p = (Player) sender;
        Inventory inv = p.getInventory();
        RecipeBook book = null;
        int slot;

        for (slot = 0; slot < 35; slot++) {
            book = SensibleToolbox.getItemRegistry().fromItemStack(inv.getItem(slot), RecipeBook.class);
            if (book != null) {
                break;
            }
        }

        if (book == null) {
            MiscUtil.errorMessage(sender, "You must have a Recipe Book in your inventory to search for recipes!");
            return true;
        }

        String filter = args.length > 0 ? args[0] : "";
        book.setInventorySlot(slot);
        book.setRecipeNameFilter(filter);
        book.goToItemList();
        Block b = p.getTargetBlock((Set<Material>) null, 4);
        book.openBook(p, STBUtil.canFabricateWith(b) ? b : null);
        return true;
    }
}
