package io.github.woodstop.arenaRegenerator;

import io.github.woodstop.arenaRegenerator.Commands.*;
import io.github.woodstop.arenaRegenerator.Listeners.ArenaSignListener;
import io.github.woodstop.arenaRegenerator.TabCompletion.ArenaTabCompleter;
import io.github.woodstop.arenaRegenerator.TabCompletion.NoTabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArenaRegenerator extends JavaPlugin {


    private static ArenaRegenerator instance;
    private boolean worldEditLoaded = false;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getLogger().info("ArenaRegenerator enabled!");

        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Check for WorldEdit or FastAsyncWorldEdit
        Plugin fawePlugin = getServer().getPluginManager().getPlugin("FastAsyncWorldEdit");
        Plugin worldEditPlugin = getServer().getPluginManager().getPlugin("WorldEdit");

        // Flag to track if WorldEdit/FAWE is loaded
        if (fawePlugin != null && fawePlugin.isEnabled()) {
            getLogger().info("Detected FastAsyncWorldEdit. Using FAWE API.");
            worldEditLoaded = true;
        } else if (worldEditPlugin != null && worldEditPlugin.isEnabled()) {
            getLogger().info("Detected WorldEdit. Using WorldEdit API.");
            worldEditLoaded = true;
        } else {
            getLogger().severe("Neither FastAsyncWorldEdit nor WorldEdit was found! This plugin requires one of them to function.");
            getServer().getPluginManager().disablePlugin(this); // Disable your plugin if dependency missing
            return; // Stop further initialization
        }

        getCommand("savearena").setExecutor(new SaveArenaCommand());
        getCommand("savearena").setTabCompleter(new SaveArenaCommand());

        getCommand("regenarena").setExecutor(new RegenerateArenaCommand());
        getCommand("regenarena").setTabCompleter(new ArenaTabCompleter());

        getCommand("delarena").setExecutor(new DeleteArenaCommand());
        getCommand("delarena").setTabCompleter(new ArenaTabCompleter());

        getCommand("arenas").setExecutor(new ArenaListCommand());
        getCommand("arenas").setTabCompleter(new NoTabCompleter());

        getCommand("cleararena").setExecutor(new ClearArenaCommand());
        getCommand("cleararena").setTabCompleter(new ArenaTabCompleter());

        getCommand("arenainfo").setExecutor(new ArenaInfoCommand());
        getCommand("arenainfo").setTabCompleter(new ArenaTabCompleter());

        getCommand("selectarena").setExecutor(new SelectArenaCommand());
        getCommand("selectarena").setTabCompleter(new ArenaTabCompleter());

        getServer().getPluginManager().registerEvents(new ArenaSignListener(),this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // Cancel tasks
        Bukkit.getScheduler().cancelTasks(this);

        // Unregister listeners
        HandlerList.unregisterAll(this);

        // Save config if modified
        this.saveConfig();

    }

    public static ArenaRegenerator getInstance() {
        return instance;
    }
    public boolean isWorldEditLoaded() {
        return worldEditLoaded;
    }
}
