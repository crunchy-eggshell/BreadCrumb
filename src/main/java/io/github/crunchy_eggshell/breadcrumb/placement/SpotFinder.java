package io.github.crunchy_eggshell.breadcrumb.placement;

import io.github.crunchy_eggshell.breadcrumb.BreadCrumb;
import io.github.crunchy_eggshell.breadcrumb.config.ConfigManager;
import io.github.crunchy_eggshell.breadcrumb.structure.StoredStructure;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

public final class SpotFinder {

    private final TerrainCheck terrainCheck;
    private final ConfigManager configManager;
    private final Executor mainThread;
    private final BreadCrumb plugin;

    public SpotFinder(TerrainCheck terrainCheck, ConfigManager configManager, Executor mainThread, BreadCrumb plugin) {
        this.terrainCheck = terrainCheck;
        this.configManager = configManager;
        this.mainThread = mainThread;
        this.plugin = plugin;
    }

    public CompletableFuture<Location> find(World world, Location center, int radius, StoredStructure structure) {
        return attempt(world, center, radius, structure, configManager.config().attemptsPerSub());
    }

    private CompletableFuture<Location> attempt(World world, Location center, int radius, StoredStructure structure, int remaining) {
        if (remaining == 0) return CompletableFuture.completedFuture(null);
        double angleRoll = ThreadLocalRandom.current().nextDouble();
        double distanceRoll = ThreadLocalRandom.current().nextDouble();
        double angle = angleRoll * 2 * Math.PI;
        double distance = radius * Math.sqrt(distanceRoll);
        int x = (int) Math.round(center.getX() + distance * Math.cos(angle));
        int z = (int) Math.round(center.getZ() + distance * Math.sin(angle));
        int cornerX = x - structure.sizeX() / 2;
        int cornerZ = z - structure.sizeZ() / 2;
        int buffer = configManager.config().buffer();
        int minChunkX = Math.floorDiv(cornerX - buffer, 16);
        int maxChunkX = Math.floorDiv(cornerX + structure.sizeX() - 1 + buffer, 16);
        int minChunkZ = Math.floorDiv(cornerZ - buffer, 16);
        int maxChunkZ = Math.floorDiv(cornerZ + structure.sizeZ() - 1 + buffer, 16);
        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                futures.add(world.getChunkAtAsync(chunkX, chunkZ));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApplyAsync(ignored -> {
                    return terrainCheck.check(world, cornerX, cornerZ, structure);
                }, mainThread)
                .thenCompose(result -> {
                    if (result.passed()) return CompletableFuture.completedFuture(result.location());

                    if (configManager.config().debug()) {
                        plugin.getLogger().info("Spot " + cornerX + ", " + cornerZ + ", failed because: " + result.reason().description() + " : " + result.detail());
                    }
                    return attempt(world, center, radius, structure, remaining - 1);
                });
    }
}
