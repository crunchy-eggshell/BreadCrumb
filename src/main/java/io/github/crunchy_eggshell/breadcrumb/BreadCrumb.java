package io.github.crunchy_eggshell.breadcrumb;

import io.github.crunchy_eggshell.breadcrumb.command.CrumbCommand;
import io.github.crunchy_eggshell.breadcrumb.config.ConfigManager;
import io.github.crunchy_eggshell.breadcrumb.selection.SelectionManager;
import io.github.crunchy_eggshell.breadcrumb.selection.SelectionOutline;
import io.github.crunchy_eggshell.breadcrumb.selection.Wand;
import io.github.crunchy_eggshell.breadcrumb.selection.WandListener;
import io.github.crunchy_eggshell.breadcrumb.storage.Database;
import io.github.crunchy_eggshell.breadcrumb.structure.StructureStore;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executor;

public final class BreadCrumb extends JavaPlugin {

    private ConfigManager configManager;
    private Database database;
    private SelectionManager selectionManager;
    private Wand wand;
    private SelectionOutline outline;
    private CrumbCommand crumbCommand;
    private StructureStore store;
    private Executor mainThread;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        try {
            configManager.load();
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("Something went wrong with the config: " + e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        database = new Database(this);
        try {
            database.connect();
        } catch (SQLException e) {
            getLogger().severe("Something went wrong with the database: " + e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        store = new StructureStore(this, database);
        selectionManager = new SelectionManager();
        wand = new Wand(this, configManager);
        mainThread = (runnable -> Bukkit.getScheduler().runTask(this, runnable));
        store.loadAll()
                .thenRunAsync(() -> getLogger().info("Structures loaded: " + store.all().size()), mainThread)
                .exceptionally(error -> {
                    getLogger().severe("Structures failed to load: " + error);
                    return null;
                });
        Bukkit.getPluginManager().registerEvents(selectionManager, this);
        Bukkit.getPluginManager().registerEvents(new WandListener(wand, selectionManager, configManager), this);
        outline = new SelectionOutline(this, selectionManager, wand);
        outline.start();
        crumbCommand = new CrumbCommand(configManager, selectionManager, wand, mainThread, store, this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(crumbCommand.build(), "BreadCrumb main command.", List.of("crumb"));
        });
    }

    @Override
    public void onDisable() {
        if (outline != null) outline.stop();
        if (database != null) {
            try {
                database.close();
            } catch (SQLException e) {
                getLogger().severe("Something went wrong with the database: " + e);
            }
        }
    }
}
