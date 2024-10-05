package io.github.thebusybiscuit.sensibletoolbox.listeners;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import io.github.thebusybiscuit.sensibletoolbox.blocks.SoundMuffler;
import me.desht.dhutils.Debugger;

/**
 * This {@link Listener} handles sound packets for the {@link SoundMuffler}.
 * 
 * @author desht
 * @author TheBusyBiscuit
 * 
 * @see SoundMuffler
 *
 */
public class SoundMufflerListener extends PacketAdapter implements Listener {

    private static final PacketType SOUND_PACKET = PacketType.Play.Server.NAMED_SOUND_EFFECT;

    private final Set<SoundMuffler> mufflers = new HashSet<>();

    public SoundMufflerListener(@Nonnull Plugin plugin) {
        super(plugin, ListenerPriority.NORMAL, SOUND_PACKET);
    }

    @Override
    public void onPacketSending(PacketEvent e) {
        // Fixes #72 - Check if Player is temporary
        if (!e.isPlayerTemporary() && e.getPacketType() == SOUND_PACKET) {
            Player p = e.getPlayer();

            int x = e.getPacket().getIntegers().read(0) >> 3;
            int y = e.getPacket().getIntegers().read(1) >> 3;
            int z = e.getPacket().getIntegers().read(2) >> 3;
            Location l = new Location(p.getWorld(), x, y, z);

            for (SoundMuffler sm : mufflers) {
                if (isInRange(sm, l)) {
                    if (sm.getVolume() == 0) {
                        // Completely mute the sound
                        e.setCancelled(true);
                    } else {
                        // Reduce the sound volume
                        e.getPacket().getFloat().write(0, (float) sm.getVolume() / 100.0F);
                    }
                }
            }
        }
    }

    private boolean isInRange(@Nonnull SoundMuffler sm, @Nonnull Location l) {
        int distance = SoundMuffler.DISTANCE * SoundMuffler.DISTANCE;
        return l.getWorld().equals(sm.getLocation().getWorld()) && l.distanceSquared(sm.getLocation()) < distance;
    }

    public void registerMuffler(@Nonnull SoundMuffler m) {
        Debugger.getInstance().debug("Registered sound muffler @ " + m.getLocation());
        mufflers.add(m);
    }

    public void unregisterMuffler(@Nonnull SoundMuffler m) {
        Debugger.getInstance().debug("Unregistered sound muffler @ " + m.getLocation());
        mufflers.remove(m);
    }

    public void clear() {
        mufflers.clear();
    }

    public void start() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }
}
