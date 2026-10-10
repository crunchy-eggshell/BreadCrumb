package io.github.crunchy_eggshell.breadcrumb.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.crunchy_eggshell.breadcrumb.config.ConfigManager;
import io.github.crunchy_eggshell.breadcrumb.selection.SelectionManager;
import io.github.crunchy_eggshell.breadcrumb.selection.Wand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class CrumbCommand {

    private final ConfigManager configManager;
    private final SelectionManager selections;
    private final Wand wand;

    public CrumbCommand(ConfigManager configManager, SelectionManager selections, Wand wand) {
        this.configManager = configManager;
        this.selections = selections;
        this.wand = wand;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("breadcrumb")
                .then(Commands.literal("wand").requires(source -> {
                    return source.getSender().hasPermission("breadcrumb.wand");
                }).executes(this::wand))
                .then(Commands.literal("pos1").requires(source -> {
                    return source.getSender().hasPermission("breadcrumb.pos");
                }).executes(this::pos1))
                .then(Commands.literal("pos2").requires(source -> {
                    return source.getSender().hasPermission("breadcrumb.pos");
                }).executes(this::pos2))
                .build();
    }

    private int wand(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.messages().get("general.players-only"));
            return Command.SINGLE_SUCCESS;
        }
        Map<Integer, ItemStack> items = player.getInventory().addItem(wand.create());
        for (ItemStack i : items.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), i);
        }
        sender.sendMessage(configManager.messages().get("selection.wand-given"));
        return Command.SINGLE_SUCCESS;
    }

    private int pos1(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.messages().get("general.players-only"));
            return Command.SINGLE_SUCCESS;
        }
        Block block = player.getLocation().getBlock();
        selections.setPos1(player, player.getLocation());
        player.sendMessage(configManager.messages().get("selection.pos1-set", Placeholder.unparsed("x", String.valueOf(block.getX())), Placeholder.unparsed("y", String.valueOf(block.getY())), Placeholder.unparsed("z", String.valueOf(block.getZ()))));
        return Command.SINGLE_SUCCESS;
    }

    private int pos2(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.messages().get("general.players-only"));
            return Command.SINGLE_SUCCESS;
        }
        Block block = player.getLocation().getBlock();
        selections.setPos2(player, player.getLocation());
        player.sendMessage(configManager.messages().get("selection.pos2-set", Placeholder.unparsed("x", String.valueOf(block.getX())), Placeholder.unparsed("y", String.valueOf(block.getY())), Placeholder.unparsed("z", String.valueOf(block.getZ()))));
        return Command.SINGLE_SUCCESS;
    }
}
