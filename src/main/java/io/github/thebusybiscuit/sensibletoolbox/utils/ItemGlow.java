package io.github.thebusybiscuit.sensibletoolbox.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.guizhanss.guizhanlib.minecraft.utils.compatibility.EnchantmentX;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

/**
 * Use ProtocolLib to add a glow to unenchanted items. With thanks to Comphenix; see
 * https://gist.github.com/aadnk/4580551
 *
 * @author desht
 * @author Comphenix
 * @author TheBusyBiscuit
 */
public final class ItemGlow {

    private ItemGlow() {}

    private static boolean inited = false;

    // use this enchantment on most items
    private static final Enchantment GLOW_FLAG = EnchantmentX.INFINITY;
    // use this enchantment on bows, where Infinity actually means something
    private static final Enchantment GLOW_FLAG_2 = EnchantmentX.PROTECTION;
    private static final int GLOW_FLAG_LEVEL = 32;

    /**
     * Initialise the ItemGlow system.
     *
     * @param plugin
     *            the plugin instance
     */
    public static void init(@Nonnull Plugin plugin) {
        PacketAdapter adapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SET_SLOT) {

            @Override
            public void onPacketSending(PacketEvent e) {
                if (e.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                    addGlow(new ItemStack[] { e.getPacket().getItemModifier().read(0) });
                } else {
                    addGlow(e.getPacket().getItemArrayModifier().read(0));
                }
            }

        };

        ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
        inited = true;
    }

    /**
     * Set the glowing status of an item stack.
     *
     * @param s
     *            the item stack to modify
     * @param glowing
     *            true to make the item glow, false to stop it glowing
     */
    public static void setGlowing(@Nonnull ItemStack s, boolean glowing) {
        if (!inited) {
            throw new IllegalStateException("ItemGlow system has not been initialised.  Call ItemGlow.init(plugin) first.");
        }

        Enchantment flag = getFlag(s);
        ItemMeta m = s.getItemMeta();

        if (glowing) {
            // if the item already has a real enchantment, let's not overwrite it!
            if (!s.getItemMeta().hasEnchant(flag)) {
                s.setItemMeta(m);
                s.addUnsafeEnchantment(flag, GLOW_FLAG_LEVEL);
            }
        } else if (s.getEnchantmentLevel(flag) == GLOW_FLAG_LEVEL) {
            s.setItemMeta(m);
            s.removeEnchantment(flag);
        }
    }

    /**
     * Check if this item stack has been set to glow.
     *
     * @param s
     *            the item stack to check
     * @return true if the stack will glow; false otherwise
     */
    public static boolean hasGlow(@Nullable ItemStack s) {

        if (s == null) {
            return false;
        } else {
            return s.getEnchantmentLevel(getFlag(s)) == GLOW_FLAG_LEVEL;
        }
    }

    private static void addGlow(ItemStack[] stacks) {
        for (ItemStack s : stacks) {
            // Only update those stacks that have our flag enchantment
            if (s != null && s.getEnchantmentLevel(getFlag(s)) == 1) {
                NbtCompound compound = (NbtCompound) NbtFactory.fromItemTag(s);
                compound.put(NbtFactory.ofList("ench"));
            }
        }
    }

    @Nonnull
    private static Enchantment getFlag(@Nonnull ItemStack item) {
        return item.getType() == Material.BOW ? GLOW_FLAG_2 : GLOW_FLAG;
    }
}
