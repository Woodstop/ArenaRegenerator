package io.github.woodstop.arenaRegenerator;

import io.github.woodstop.arenaRegenerator.Commands.*;
import io.github.woodstop.arenaRegenerator.Listeners.ArenaSignListener;
import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class ArenaRegenerator extends JavaPlugin {


    private static ArenaRegenerator instance;
    private boolean worldEditLoaded = false;
    private ArenaDataManager arenaDataManager;
    private Map<String, ConfigurationSection> minigameConfigs = new HashMap<>();
    private MinigameManager minigameManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getLogger().info("ArenaRegenerator enabled!");
        reloadPlugin();

        // Ensure data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        this.arenaDataManager = new ArenaDataManager();

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

        this.minigameManager = new MinigameManager(this, arenaDataManager);

        // Register the main ArenaCommand and its TabCompleter
        ArenaCommand arenaCommandExecutor = new ArenaCommand();
        getCommand("arena").setExecutor(arenaCommandExecutor);
        getCommand("arena").setTabCompleter(arenaCommandExecutor);

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

    /**
     * Loads all minigame configurations from the config.yml.
     */
    private void loadMinigameConfigs() {
        minigameConfigs.clear();
        ConfigurationSection minigamesSection = getConfig().getConfigurationSection("minigames");
        if (minigamesSection != null) {
            for (String arenaName : minigamesSection.getKeys(false)) { // false means only direct children
                ConfigurationSection arenaConfig = minigamesSection.getConfigurationSection(arenaName);
                if (arenaConfig != null) {
                    minigameConfigs.put(arenaName, arenaConfig);
                    getLogger().info("Loaded minigame configuration for arena: " + arenaName);
                }
            }
        } else {
            getLogger().warning("No 'minigames' section found in config.yml. Minigame features will not be available.");
        }
    }

    /**
     * Retrieves the configuration section for a specific minigame arena.
     * @param arenaName The name of the minigame arena.
     * @return The ConfigurationSection for the arena, or null if not found.
     */
    public ConfigurationSection getMinigameConfig(String arenaName) {
        return minigameConfigs.get(arenaName);
    }

    /**
     * Returns a set of all configured minigame arena names.
     * @return A set of minigame arena names.
     */
    public java.util.Set<String> getMinigameArenaNames() {
        return minigameConfigs.keySet();
    }

    /**
     * Returns the instance of the MinigameManager.
     * @return The MinigameManager instance.
     */
    public MinigameManager getMinigameManager() {
        return minigameManager;
    }

    public static ArenaRegenerator getInstance() {
        return instance;
    }
    public boolean isWorldEditLoaded() {
        return worldEditLoaded;
    }

    /**
     * Reloads the plugin's configuration and re-initializes managers.
     * This method can be called by a command.
     */
    public void reloadPlugin() {
        // Reload the config file from disk
        reloadConfig();
        getLogger().info("Configuration reloaded.");

        // Reload minigame configurations
        loadMinigameConfigs();

        // Re-initialize MinigameManager to pick up new configs
        // This will cancel existing tasks and re-load arenas
        if (minigameManager != null) {
            minigameManager.shutdown(); // Gracefully shut down current manager (cancel tasks, etc.)
        }
        this.minigameManager = new MinigameManager(this, arenaDataManager);
        getLogger().info("MinigameManager re-initialized.");
    }
}
