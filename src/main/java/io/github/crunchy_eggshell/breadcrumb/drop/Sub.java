package io.github.crunchy_eggshell.breadcrumb.drop;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public record Sub(String name, String dropName, UUID worldId, int x, int y, int z, int sizeX, int sizeY, int sizeZ, List<UUID> entityIds) {

    public Location corner() {
        return new Location(Bukkit.getWorld(worldId()), x(), y(), z());
    }

    public Location center() {
        return new Location(Bukkit.getWorld(worldId), x() + (double) sizeX / 2, (double) y() + sizeY(), z() + (double) sizeZ / 2);
    }

}
