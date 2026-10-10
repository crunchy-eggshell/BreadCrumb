package io.github.crunchy_eggshell.breadcrumb.selection;

import io.github.crunchy_eggshell.breadcrumb.config.ConfigManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class WandListener implements Listener {

    private final Wand wand;
    private final SelectionManager selections;
    private final ConfigManager configManager;

    public WandListener(Wand wand, SelectionManager selections, ConfigManager configManager) {
        this.wand = wand;
        this.selections = selections;
        this.configManager = configManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (!wand.isWand(item)) return;
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("breadcrumb.wand")) {
            player.sendMessage(configManager.messages().get("general.no-permission"));
            return;
        }
        event.setCancelled(true);
        Location location = block.getLocation();
        if (action == Action.LEFT_CLICK_BLOCK) {
            selections.setPos1(player, location);
            player.sendMessage(configManager.messages().get("selection.pos1-set", Placeholder.unparsed("x", String.valueOf(block.getX())), Placeholder.unparsed("y", String.valueOf(block.getY())), Placeholder.unparsed("z", String.valueOf(block.getZ()))));
        }
        if (action == Action.RIGHT_CLICK_BLOCK) {
            selections.setPos2(player, location);
            player.sendMessage(configManager.messages().get("selection.pos2-set", Placeholder.unparsed("x", String.valueOf(block.getX())), Placeholder.unparsed("y", String.valueOf(block.getY())), Placeholder.unparsed("z", String.valueOf(block.getZ()))));
        }

    }
}
