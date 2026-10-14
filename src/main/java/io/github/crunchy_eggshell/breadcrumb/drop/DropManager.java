package io.github.crunchy_eggshell.breadcrumb.drop;

import io.github.crunchy_eggshell.breadcrumb.BreadCrumb;
import io.github.crunchy_eggshell.breadcrumb.config.ConfigManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class DropManager {

    private final BreadCrumb plugin;
    private final DropRepository repository;
    private final ConfigManager configManager;
    private final Executor mainThread;
    private final Map<String, Drop> drops;
    private BukkitTask expiryTask;

    public DropManager(Executor mainThread, ConfigManager configManager, DropRepository repository, BreadCrumb plugin) {
        this.mainThread = mainThread;
        this.configManager = configManager;
        this.repository = repository;
        this.plugin = plugin;
        this.drops = new ConcurrentHashMap<>();
    }

    public CompletableFuture<Void> loadAll() {
        return repository.loadAll()
                .thenAcceptAsync(list -> {
                    for (Drop d : list) {
                        drops.put(d.name(), d);
                    }
                    for (Drop d : List.copyOf(drops.values())) {
                        if (d.isExpired(System.currentTimeMillis())) removeDrop(d.name()).exceptionally(error -> {
                            plugin.getLogger().warning("Something went wrong with the drop, " + d.name() + ": " + error);
                            return null;
                        });
                    }
                }, mainThread);
    }

    public CompletableFuture<Void> register(Drop drop) {
        if (drop.subs().isEmpty()) return CompletableFuture.completedFuture(null);
        drops.put(drop.name(), drop);
        return repository.save(drop);
    }

    public Collection<Drop> all() {
        return drops.values();
    }

    public Drop get(String name) {
        return drops.get(name);
    }

    public Sub findSub(String subName) {
        for (Drop d : drops.values()) {
            for (Sub s : d.subs()) {
                if (s.name().equals(subName)) return s;
            }
        }
        return null;
    }

    public CompletableFuture<Void> removeDrop(String name) {
        Drop removed = drops.remove(name);
        if (removed == null) return CompletableFuture.completedFuture(null);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Sub s : removed.subs()) {
            futures.add(clearSub(s));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenCompose(ignored -> {
                    return repository.deleteDrop(name);
                });
    }

    public CompletableFuture<Void> removeSub(String subName) {
        Sub sub = findSub(subName);
        if (sub == null) return CompletableFuture.completedFuture(null);
        Drop drop = drops.get(sub.dropName());
        List<Sub> subs = new ArrayList<>();
        for (Sub s : drop.subs()) {
            if (!s.name().equals(subName)) subs.add(s);
        }
        if (subs.isEmpty()) {
            return removeDrop(drop.name());
        }
        Drop newDrop = new Drop(drop.name(), drop.structureName(), drop.createdAt(), drop.expiresAt(), List.copyOf(subs));
        drops.replace(drop.name(), drop, newDrop);
        return clearSub(sub)
                .thenCompose(ignored -> {
                   return repository.deleteSub(subName);
                });

    }

    private CompletableFuture<Void> clearSub(Sub sub) {
        World world = Bukkit.getWorld(sub.worldId());
        if (world == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("World for sub, " + sub.name() + " is not loaded"));
        }
        int minChunkX = Math.floorDiv(sub.x(), 16);
        int maxChunkX = Math.floorDiv(sub.x() + sub.sizeX() - 1, 16);
        int minChunkZ = Math.floorDiv(sub.z(), 16);
        int maxChunkZ = Math.floorDiv(sub.z() + sub.sizeZ() - 1, 16);
        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                futures.add(world.getChunkAtAsync(chunkX, chunkZ));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRunAsync(() -> {
                    for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                            world.getChunkAt(chunkX, chunkZ).getEntities();
                        }
                    }
                    for (UUID e : sub.entityIds()) {
                        Entity entity = Bukkit.getEntity(e);
                        if (entity != null) entity.remove();
                    }
                    for (int x = sub.x(); x < sub.x() + sub.sizeX(); x++) {
                        for (int y = sub.y(); y < sub.y() + sub.sizeY(); y++) {
                            for (int z = sub.z(); z < sub.z() + sub.sizeZ(); z++) {
                                world.getBlockAt(x, y, z).setType(Material.AIR, false);
                            }
                        }
                    }
                }, mainThread);
    }

    public void startExpiryTask() {
        expiryTask =  plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Drop d : List.copyOf(drops.values())) {
                if (d.isExpired(now)) {
                    removeDrop(d.name()).exceptionally(error -> {
                        plugin.getLogger().warning("Something went wrong with the drop, " + d.name() + ": " + error);
                        return null;
                    });
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        if (p.hasPermission("breadcrumb.drops")) {
                            p.sendMessage(configManager.messages().get("drop.expired", Placeholder.unparsed("drop", d.name())));
                        }
                    }
                }
            }
        }, 20, 20);
    }

    public void stopExpiryTask() {
        if (expiryTask != null) expiryTask.cancel();
    }
}
