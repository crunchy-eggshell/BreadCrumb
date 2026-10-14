package io.github.crunchy_eggshell.breadcrumb.placement;

import io.github.crunchy_eggshell.breadcrumb.drop.Drop;
import io.github.crunchy_eggshell.breadcrumb.drop.Sub;
import io.github.crunchy_eggshell.breadcrumb.structure.StoredStructure;
import io.github.crunchy_eggshell.breadcrumb.structure.StructureStore;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

public final class Spawner {

    private final SpotFinder spotFinder;
    private final StructureStore store;
    private final Executor mainThread;

    public Spawner(SpotFinder spotFinder, StructureStore store, Executor mainThread) {
        this.spotFinder = spotFinder;
        this.store = store;
        this.mainThread = mainThread;
    }

    public CompletableFuture<SpawnResult> spawn(StoredStructure structure, int amount, int radius, Location center, Long durationMillis) {
        List<Sub> placed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        return store.reserveDropNumber(structure.name())
                .thenCompose(number -> {
                    String dropName = structure.name() + "-" + number;
                    return placeSub(dropName, 1, structure, amount, radius, center, placed, failed);
                })
                .thenApply(dropName -> {
                    long now = System.currentTimeMillis();
                    Long expiresAt = null;
                    if (durationMillis != null) expiresAt = now + durationMillis;
                    Drop drop = new Drop(dropName, structure.name(), now, expiresAt, List.copyOf(placed));
                    return new SpawnResult(drop, List.copyOf(failed));
                });
    }

    private CompletableFuture<String> placeSub(String dropName, int index, StoredStructure structure, int amount, int radius, Location center, List<Sub> placed, List<String> failed) {
        if (index > amount) return CompletableFuture.completedFuture(dropName);
        String subName = dropName + "-sub-" + index;
        return spotFinder.find(center.getWorld(), center, radius, structure)
                .thenApplyAsync(location -> {
                    if (location == null) {
                        failed.add(subName);
                        return null;
                    }
                    BoundingBox boundingBox = new BoundingBox(location.x(), location.y(), location.z(), location.x() + structure.sizeX(), location.y() + structure.sizeY(), location.z() + structure.sizeZ());
                    Collection<Entity> entities = location.getWorld().getNearbyEntities(boundingBox);
                    structure.structure().place(location, true, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, ThreadLocalRandom.current());
                    List<UUID> entitiesUUIDs = new ArrayList<>();
                    for (Entity e : location.getWorld().getNearbyEntities(boundingBox)) {
                        if (entities.contains(e)) continue;
                        if (e instanceof Player) continue;
                        entitiesUUIDs.add(e.getUniqueId());
                    }
                    Sub sub = new Sub(subName, dropName, location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), structure.sizeX(), structure.sizeY(), structure.sizeZ(), List.copyOf(entitiesUUIDs));
                    placed.add(sub);
                    return null;
                }, mainThread)
                .thenCompose(ignored -> {
                    return placeSub(dropName, index + 1, structure, amount, radius, center, placed, failed);
                });
    }
}
