package io.github.crunchy_eggshell.breadcrumb.config;

import io.github.crunchy_eggshell.breadcrumb.BreadCrumb;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class ConfigManager {

    private final BreadCrumb plugin;
    private volatile BreadCrumbConfig config;
    private volatile Messages messages;

    public ConfigManager(BreadCrumb plugin) {
        this.plugin = plugin;
    }

    public void load() throws IOException, InvalidConfigurationException {
        validateConfigFile();
        messages = Messages.load(plugin);
        config = BreadCrumbConfig.load(plugin);
    }

    public boolean reload() {
        try {
            validateConfigFile();
            Messages newMessages = Messages.load(plugin);
            BreadCrumbConfig newConfig = BreadCrumbConfig.load(plugin);
            messages = newMessages;
            config = newConfig;
            return true;
        }
        catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Something went wrong with the config files: " + e);
            return false;
        }
    }

    public BreadCrumbConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    private void validateConfigFile() throws IOException, InvalidConfigurationException {
        plugin.saveDefaultConfig();
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
    }
}
