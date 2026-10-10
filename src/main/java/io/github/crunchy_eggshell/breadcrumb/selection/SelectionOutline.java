package io.github.crunchy_eggshell.breadcrumb.selection;

import io.github.crunchy_eggshell.breadcrumb.BreadCrumb;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class SelectionOutline implements Runnable {

    private final BreadCrumb plugin;
    private final SelectionManager selections;
    private final Wand wand;
    private BukkitTask task;
    private static final Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(140, 89, 45), 1.0f);

    public SelectionOutline(BreadCrumb plugin, SelectionManager selections, Wand wand) {
        this.plugin = plugin;
        this.selections = selections;
        this.wand = wand;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 0L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!wand.isWand(p.getInventory().getItemInMainHand())) continue;
            Selection selection = selections.get(p);
            if (!selection.isComplete() || !selection.sameWorld()) continue;
            if (!selection.pos1().getWorld().equals(p.getWorld())) continue;
            drawBox(p, selection);
        }
    }

    private void drawBox(Player player, Selection selection) {
        Location min = selection.min();
        Location max = selection.max();
        double minX = min.getX();
        double minY = min.getY();
        double minZ = min.getZ();
        double maxX = max.getX() + 1;
        double maxY = max.getY() + 1;
        double maxZ = max.getZ() + 1;

        Location b1 = new Location(selection.pos1().getWorld(), minX, minY, minZ);
        Location b2 = new Location(selection.pos1().getWorld(), maxX, minY, minZ);
        Location b3 = new Location(selection.pos1().getWorld(), maxX, minY, maxZ);
        Location b4 = new Location(selection.pos1().getWorld(), minX, minY, maxZ);

        Location t1 = new Location(selection.pos1().getWorld(), minX, maxY, minZ);
        Location t2 = new Location(selection.pos1().getWorld(), maxX, maxY, minZ);
        Location t3 = new Location(selection.pos1().getWorld(), maxX, maxY, maxZ);
        Location t4 = new Location(selection.pos1().getWorld(), minX, maxY, maxZ);

        drawLine(player, b1, b2);
        drawLine(player, b2, b3);
        drawLine(player, b3, b4);
        drawLine(player, b4, b1);
        drawLine(player, t1, t2);
        drawLine(player, t2, t3);
        drawLine(player, t3, t4);
        drawLine(player, t4, t1);
        drawLine(player, b1, t1);
        drawLine(player, b2, t2);
        drawLine(player, b3, t3);
        drawLine(player, b4, t4);
    }

    private void drawLine(Player player, Location from, Location to) {
        double length = from.distance(to);
        if (length == 0) return;
        double spacing = Math.max(0.5, length / 64);
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        for (double d = 0; d <= length; d += spacing) {
            Location point = from.clone().add(direction.clone().multiply(d));
            player.spawnParticle(Particle.DUST, point, 1, dust);
        }
    }
}
