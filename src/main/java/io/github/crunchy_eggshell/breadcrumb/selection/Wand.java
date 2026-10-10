package io.github.crunchy_eggshell.breadcrumb.selection;

import io.github.crunchy_eggshell.breadcrumb.BreadCrumb;
import io.github.crunchy_eggshell.breadcrumb.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class Wand {

    private final NamespacedKey key;
    private final ConfigManager configManager;

    public Wand(BreadCrumb plugin, ConfigManager configManager) {
        this.key = new NamespacedKey(plugin, "wand");
        this.configManager = configManager;
    }

    public ItemStack create() {
        ItemStack wand = new ItemStack(configManager.config().wandMaterial());
        wand.editMeta(wandMeta -> {
            wandMeta.displayName(MiniMessage.miniMessage().deserialize("<!i><b><gradient:#8c592d:#b38940:#8c592d>BreadCrumb Wand</gradient>"));
            wandMeta.lore(List.of(
                    MiniMessage.miniMessage().deserialize("<i><b><u><green>Left-click to set pos1</green></u></b></i>"),
                    MiniMessage.miniMessage().deserialize("<i><b><u><green>Right-click to set pos2</green></u></b></i>")
            ));
            PersistentDataContainer pdc = wandMeta.getPersistentDataContainer();
            pdc.set(key, PersistentDataType.BYTE, (byte) 1);
        });
        return wand;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        return item.getPersistentDataContainer().has(key);
    }


}
