package io.github.crunchy_eggshell.breadcrumb.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.crunchy_eggshell.breadcrumb.BreadCrumb;
import io.github.crunchy_eggshell.breadcrumb.config.ConfigManager;
import io.github.crunchy_eggshell.breadcrumb.selection.Selection;
import io.github.crunchy_eggshell.breadcrumb.selection.SelectionManager;
import io.github.crunchy_eggshell.breadcrumb.selection.Wand;
import io.github.crunchy_eggshell.breadcrumb.structure.StoredStructure;
import io.github.crunchy_eggshell.breadcrumb.structure.StructureStore;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;

public final class CrumbCommand {

    private final ConfigManager configManager;
    private final SelectionManager selections;
    private final Wand wand;
    private final Executor mainThread;
    private final StructureStore store;
    private final BreadCrumb plugin;

    public CrumbCommand(ConfigManager configManager, SelectionManager selections, Wand wand, Executor mainThread, StructureStore store, BreadCrumb plugin) {
        this.configManager = configManager;
        this.selections = selections;
        this.wand = wand;
        this.mainThread = mainThread;
        this.store = store;
        this.plugin = plugin;
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
                .then(Commands.literal("save").requires(source -> {
                    return source.getSender().hasPermission("breadcrumb.save");
                }).then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::save)
                .then(Commands.argument("description", StringArgumentType.greedyString())
                        .executes(this::save))))
                .then(Commands.literal("delete").requires(source -> {
                    return source.getSender().hasPermission("breadcrumb.delete");
                }).then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::delete)
                        .suggests((context, builder) -> {
                            for (StoredStructure s : store.all()) {
                                builder.suggest(s.name());
                            }
                            return builder.buildFuture();
                        })))
                .then(Commands.literal("list").requires(source -> {
                    return source.getSender().hasPermission("breadcrumb.list");
                }).executes(this::list))
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

    private int save(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.messages().get("general.players-only"));
            return Command.SINGLE_SUCCESS;
        }
        Selection selection = selections.get(player);
        if (!selection.isComplete()) {
            player.sendMessage(configManager.messages().get("selection.incomplete"));
            return Command.SINGLE_SUCCESS;
        }
        if (!selection.sameWorld()) {
            player.sendMessage(configManager.messages().get("selection.different-worlds"));
            return Command.SINGLE_SUCCESS;
        }
        String name = StringArgumentType.getString(context, "name");
        if (!name.matches("[a-zA-Z0-9_-]{1,32}")) {
            player.sendMessage(configManager.messages().get("structure.invalid-name"));
            return Command.SINGLE_SUCCESS;
        }
        if (store.exists(name)) {
            player.sendMessage(configManager.messages().get("structure.already-exists", Placeholder.unparsed("name", name)));
            return Command.SINGLE_SUCCESS;
        }
        String description;
        try {
            description = StringArgumentType.getString(context, "description");
        } catch (IllegalArgumentException e) {
            description = "";
        }
        store.save(name, description, null, selection).thenAcceptAsync(saved -> player.sendMessage(configManager.messages().get("structure.saved", Placeholder.unparsed("name", saved.name()), Placeholder.unparsed("size", String.valueOf(selection.sizeX()) + "x" + String.valueOf(selection.sizeY()) + "x" + String.valueOf(selection.sizeZ())))), mainThread)
                .exceptionallyAsync(error -> {
                   player.sendMessage(configManager.messages().get("structure.save-failed"));
                   plugin.getLogger().warning("Something went wrong: " + error);
                   return null;
                }, mainThread);
        return Command.SINGLE_SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Collection<StoredStructure> structures = store.all();
        if (structures.isEmpty()) {
            sender.sendMessage(configManager.messages().get("structure.list-empty"));
            return Command.SINGLE_SUCCESS;
        }
        sender.sendMessage(configManager.messages().get("structure.list-header", Placeholder.unparsed("count", String.valueOf(structures.size()))));
        for (StoredStructure s : structures) {
            sender.sendMessage(configManager.messages().raw("structure.list-entry", Placeholder.unparsed("name", s.name()), Placeholder.unparsed("size", String.valueOf(s.sizeX()) + "x" + String.valueOf(s.sizeY()) + "x" + String.valueOf(s.sizeZ())), Placeholder.unparsed("description", s.description())));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int delete(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, "name");
        if (!store.exists(name)) {
            sender.sendMessage(configManager.messages().get("structure.not-found", Placeholder.unparsed("name", name)));
            return Command.SINGLE_SUCCESS;
        }
        store.delete(name).thenRunAsync(() -> sender.sendMessage(configManager.messages().get("structure.deleted", Placeholder.unparsed("name", name))), mainThread)
                .exceptionallyAsync(error -> {
                    if (sender instanceof Player) {
                        sender.sendMessage(configManager.messages().get("structure.delete-failed"));
                        plugin.getLogger().warning("Something went wrong: " + error);
                    }else plugin.getLogger().warning("Something went wrong: " + error);
                    return null;
                }, mainThread);
        return Command.SINGLE_SUCCESS;
    }
}
