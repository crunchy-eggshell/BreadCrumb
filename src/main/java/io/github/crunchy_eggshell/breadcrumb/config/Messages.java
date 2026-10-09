package io.github.crunchy_eggshell.breadcrumb.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public record Messages(String prefix, Map<String, String> messages) {

    private static final MiniMessage mini = MiniMessage.miniMessage();

    public static Messages load(JavaPlugin plugin) throws IOException, InvalidConfigurationException {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        Map<String, String> messages = new HashMap<>();
        yaml.load(file);
        InputStream inputStream = plugin.getResource("messages.yml");
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            yaml.setDefaults(defaults);
            String prefix = yaml.getString("prefix", "");
            for(String s : defaults.getKeys(true)) {
                if (yaml.isConfigurationSection(s)) continue;
                messages.put(s, yaml.getString(s));
            }
            return new Messages(prefix, Map.copyOf(messages));
        }
    }

    public Component get(String key, TagResolver... placeholders) {
        String string = messages().get(key);
        if (string == null) {
            return Component.text(key, NamedTextColor.RED);
        }
        return mini.deserialize(prefix + string, placeholders);
    }

    public Component raw(String key, TagResolver... placeholders) {
        String string = messages().get(key);
        if (string == null) {
            return Component.text(key, NamedTextColor.RED);
        }
        return mini.deserialize(string, placeholders);
    }

    public void send(Audience audience, String key, TagResolver... placeholders) {
        audience.sendMessage(get(key, placeholders));
    }

}
