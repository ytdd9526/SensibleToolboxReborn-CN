package io.github.thebusybiscuit.sensibletoolbox.core.storage;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;

class UpdateRecord {

    private final DatabaseOperation op;
    private final UUID worldID;
    private final int x;
    private final int y;
    private final int z;
    private String type;
    private String data;

    @Nonnull
    public static UpdateRecord finishingRecord() {
        return new UpdateRecord(DatabaseOperation.FINISH, null);
    }

    @Nonnull
    public static UpdateRecord commitRecord() {
        return new UpdateRecord(DatabaseOperation.COMMIT, null);
    }

    protected UpdateRecord(@Nonnull DatabaseOperation op, @Nullable Location l) {
        this.op = op;

        if (l != null) {
            this.worldID = l.getWorld().getUID();
            this.x = l.getBlockX();
            this.y = l.getBlockY();
            this.z = l.getBlockZ();
        } else {
            this.worldID = null;
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpdateRecord that = (UpdateRecord) o;

        if (x != that.x) {
            return false;
        }
        if (y != that.y) {
            return false;
        }
        if (z != that.z) {
            return false;
        }

        return worldID != null ? worldID.equals(that.worldID) : that.worldID == null;
    }

    @Override
    public int hashCode() {
        int result = worldID != null ? worldID.hashCode() : 0;
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Nonnull
    public DatabaseOperation getOp() {
        return op;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public UUID getWorldID() {
        return worldID;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        switch (op) {
            case FINISH:
            case COMMIT:
                return op.toString();
            default:
                return String.format("%s %s,%d,%d,%d %s", op.toString(), worldID, x, y, z, type);
        }
    }

    @Nonnull
    public Location getLocation() {
        return new Location(Bukkit.getWorld(worldID), x, y, z);
    }
}
