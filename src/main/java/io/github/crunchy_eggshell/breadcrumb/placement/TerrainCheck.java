package io.github.crunchy_eggshell.breadcrumb.placement;

import io.github.crunchy_eggshell.breadcrumb.config.BreadCrumbConfig;
import io.github.crunchy_eggshell.breadcrumb.config.ConfigManager;
import io.github.crunchy_eggshell.breadcrumb.structure.StoredStructure;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class TerrainCheck {

    private final ConfigManager configManager;

    public TerrainCheck(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public CheckResult check(World world, int x, int z, StoredStructure structure) {
        BreadCrumbConfig config = configManager.config();
        int buffer = config.buffer();
        int groundDepth = config.groundDepth();
        int flatnessTolerance = structure.effectiveTolerance(config);
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int cx = x; cx < x + structure.sizeX(); cx++) {
            for (int cz = z; cz < z + structure.sizeZ(); cz++) {
                Block highestBlock = world.getHighestBlockAt(cx, cz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                if (!config.isNaturalGround(highestBlock.getType())) {
                    return CheckResult.fail(FailReason.NOT_NATURAL_GROUND, highestBlock.getType().name());
                }
                for (int d = 1; d <= groundDepth; d++) {
                    if (!highestBlock.getRelative(BlockFace.DOWN, d).isSolid()) {
                        return CheckResult.fail(FailReason.FLOATING_GROUND, String.valueOf(d));
                    }
                }
                lowest = Math.min(lowest, highestBlock.getY());
                highest = Math.max(highest, highestBlock.getY());
            }
        }
        if ((highest - lowest) > flatnessTolerance) {
            return CheckResult.fail(FailReason.NOT_FLAT, String.valueOf(highest - lowest));
        }
        int baseY = highest + 1;
        int topY = baseY + structure.sizeY() - 1 + buffer;
        if (topY > world.getMaxHeight() - 1) {
            return CheckResult.fail(FailReason.TOO_HIGH, String.valueOf(topY));
        }
        for (int cx = x - buffer; cx < x + structure.sizeX() + buffer; cx++) {
            for (int cz = z - buffer; cz < z + structure.sizeZ() + buffer; cz++) {
                Block topBlock = world.getHighestBlockAt(cx, cz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                boolean inBuffer = false;
                if (cx < x || cx > x + structure.sizeX() - 1 || cz < z || cz > z + structure.sizeZ() - 1) inBuffer = true;
                if (inBuffer && !config.isNaturalGround(topBlock.getType())) {
                    return CheckResult.fail(FailReason.BUFFER_BUILT, topBlock.getType().name());
                }
                for (int y = topBlock.getY() + 1; y <= topY; y++) {
                    Block block = world.getBlockAt(cx, y, cz);
                    if (!block.getType().isAir() && !config.isReplaceable(block.getType())) {
                        return CheckResult.fail(FailReason.SPACE_BLOCKED, block.getType().name());
                    }
                }
            }
        }
        return CheckResult.success(new Location(world, x, baseY, z));
    }
}
