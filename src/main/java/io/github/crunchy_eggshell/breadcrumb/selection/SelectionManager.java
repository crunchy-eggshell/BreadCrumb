package io.github.crunchy_eggshell.breadcrumb.selection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SelectionManager implements Listener {

    private final Map<UUID, Selection> selections = new HashMap<>();

    public Selection setPos1(Player player, Location location) {
        Selection selection = selections.getOrDefault(player.getUniqueId(), Selection.empty());
        Selection newSelection = selection.withPos1(location);
        selections.put(player.getUniqueId(), newSelection);
        return newSelection;
    }

    public Selection setPos2(Player player, Location location) {
        Selection selection = selections.getOrDefault(player.getUniqueId(), Selection.empty());
        Selection newSelection = selection.withPos2(location);
        selections.put(player.getUniqueId(), newSelection);
        return newSelection;
    }

    public Selection get(Player player) {
        return selections.getOrDefault(player.getUniqueId(), Selection.empty());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selections.remove(event.getPlayer().getUniqueId());
    }

}
