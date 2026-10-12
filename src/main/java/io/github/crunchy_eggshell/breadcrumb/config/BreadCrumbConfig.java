package io.github.crunchy_eggshell.breadcrumb.config;


import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


public record BreadCrumbConfig(Material wandMaterial, Set<Material> naturalGround, Set<Material> replaceable, int flatnessTolerance,
                               int attemptsPerSub, int buffer, int groundDepth, boolean debug) {

    public static BreadCrumbConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        Material wand = Material.matchMaterial(config.getString("wand-item", "BREEZE_ROD"));
        if (wand == null || !wand.isItem() || wand.isAir()) {
            plugin.getLogger().warning("Invalid config value wand-item: " + config.getString("wand-item"));
            wand = Material.BREEZE_ROD;
        }
        Set<Material> naturalGround = loadBlockSet(config, "natural-ground", plugin);
        Set<Material> replaceable = loadBlockSet(config, "replaceable", plugin);
        if (replaceable.isEmpty()) {
            plugin.getLogger().warning("Invalid config list value replaceable: empty, this will result in structures rarely spawning, please add your wanted values to solve the issue.");
        }
        if (naturalGround.isEmpty()) {
            plugin.getLogger().severe("Invalid config list value natural-ground: empty, this will result in the plugin not working at all, please add your wanted values to continue using the plugin.");
        }
        int flatnessTolerance = clampAndLog(config.getInt("flatness-tolerance", 2), 0, 100, "flatness-tolerance", plugin);
        int attemptsPerSub = clampAndLog(config.getInt("attempts-per-sub", 50), 1, 100, "attempts-per-sub", plugin);
        int buffer = clampAndLog(config.getInt("buffer", 2), 0, 10, "buffer", plugin);
        int groundDepth = clampAndLog(config.getInt("ground-depth", 3), 0, 16, "ground-depth", plugin);
        boolean debug = config.getBoolean("debug", false);
        return new BreadCrumbConfig(wand, naturalGround, replaceable, flatnessTolerance, attemptsPerSub, buffer, groundDepth, debug);
    }

    public boolean isNaturalGround(Material material) {
        return naturalGround().contains(material);
    }

    public boolean isReplaceable(Material material) {
        return replaceable().contains(material);
    }

    private static int clampAndLog(int value, int min, int max, String valueName, JavaPlugin plugin) {
        int clampedValue = Math.max(min, Math.min(max, value));
        if (clampedValue != value) {
            plugin.getLogger().warning("Invalid config value " + valueName + ": " + value + ", value has been clamped to: " + clampedValue);
        }
        return clampedValue;
    }

    private static Set<Material> loadBlockSet(FileConfiguration config, String key, JavaPlugin plugin) {
        List<String> blockSetStrings = config.getStringList(key);
        EnumSet<Material> blockSetEnumSet = EnumSet.noneOf(Material.class);
        for (String s : blockSetStrings) {
            Material m = Material.matchMaterial(s);
            if (m == null) {
                plugin.getLogger().warning("Invalid config list value " + key + ": " + s);
                continue;
            }
            if (!m.isBlock()) {
                plugin.getLogger().warning("Invalid config list value " + key + ": " + s + ", material must be a block.");
                continue;
            }
            blockSetEnumSet.add(m);
        }
        return Collections.unmodifiableSet(blockSetEnumSet);
    }
}
