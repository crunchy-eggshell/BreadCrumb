package io.github.crunchy_eggshell.breadcrumb.selection;

import org.bukkit.Location;

public record Selection(Location pos1, Location pos2) {

    public static Selection empty() {
        return new Selection(null, null);
    }

    public Selection withPos1(Location location) {
        return new Selection(location.toBlockLocation(), pos2());
    }

    public Selection withPos2(Location location) {
        return new Selection(pos1(), location.toBlockLocation());
    }

    public boolean isComplete() {
        return pos1() != null && pos2() != null;
    }

    public boolean sameWorld() {
        return isComplete() && pos1().getWorld().equals(pos2().getWorld());
    }

    public Location min() {
        int x = Math.min(pos1().getBlockX(), pos2().getBlockX());
        int y = Math.min(pos1().getBlockY(), pos2().getBlockY());
        int z = Math.min(pos1().getBlockZ(), pos2().getBlockZ());
        return new Location(pos1.getWorld(), x, y, z);
    }

    public Location max() {
        int x = Math.max(pos1().getBlockX(), pos2().getBlockX());
        int y = Math.max(pos1().getBlockY(), pos2().getBlockY());
        int z = Math.max(pos1().getBlockZ(), pos2().getBlockZ());
        return new Location(pos1.getWorld(), x, y, z);
    }

    public int sizeX() {
        return Math.max(pos1().getBlockX(), pos2().getBlockX()) - Math.min(pos1().getBlockX(), pos2().getBlockX()) + 1;
    }

    public int sizeY() {
        return Math.max(pos1().getBlockY(), pos2().getBlockY()) - Math.min(pos1().getBlockY(), pos2().getBlockY()) + 1;
    }

    public int sizeZ() {
        return Math.max(pos1().getBlockZ(), pos2().getBlockZ()) - Math.min(pos1().getBlockZ(), pos2().getBlockZ()) + 1;
    }
}
