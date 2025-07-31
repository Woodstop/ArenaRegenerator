package io.github.woodstop.arenaRegenerator;

import io.github.woodstop.arenaRegenerator.Commands.*;
import io.github.woodstop.arenaRegenerator.Listeners.ArenaSignListener;
import io.github.woodstop.arenaRegenerator.Listeners.MinigameBlockListener;
import io.github.woodstop.arenaRegenerator.Listeners.MinigameDamageListener;
import io.github.woodstop.arenaRegenerator.Listeners.MinigameItemListener;
import io.github.woodstop.arenaRegenerator.Listeners.MinigamePlayerListener;
import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ArenaRegenerator extends JavaPlugin {


    private static ArenaRegenerator instance;
    private boolean worldEditLoaded = false;
    private ArenaDataManager arenaDataManager;
    private Map<String, ConfigurationSection> minigameConfigs = new HashMap<>();
    private MinigameManager minigameManager;
    private int signUseCooldownSeconds;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        // Save default config if not present
        saveDefaultConfig();
        getLogger().info("ArenaRegenerator enabled!");

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

        loadMinigameConfigs();
        try {
            this.minigameManager = new MinigameManager(this, arenaDataManager);
        } catch (Exception e) {
            getLogger().severe("[ArenaRegenerator] Failed to initialize MinigameManager: " + e.getMessage());
            getLogger().severe("[ArenaRegenerator] Minigame features will be unavailable.");
            this.minigameManager = null;
            // It's critical to disable the plugin or handle this gracefully if minigame features are essential
            // For now, we'll allow it to continue but minigame features won't work.
        }

        // Register the main ArenaCommand and its TabCompleter
        ArenaCommand arenaCommandExecutor = new ArenaCommand();
        getCommand("arena").setExecutor(arenaCommandExecutor);
        getCommand("arena").setTabCompleter(arenaCommandExecutor);

        getServer().getPluginManager().registerEvents(new ArenaSignListener(minigameManager), this); // ArenaSignListener does not directly depend on MinigameManager
        if (minigameManager != null) {
            getServer().getPluginManager().registerEvents(new MinigamePlayerListener(minigameManager), this);
            getServer().getPluginManager().registerEvents(new MinigameBlockListener(minigameManager), this);
            getServer().getPluginManager().registerEvents(new MinigameDamageListener(minigameManager), this);
            getServer().getPluginManager().registerEvents(new MinigameItemListener(minigameManager), this);
        } else {
            getLogger().warning("[ArenaRegenerator] MinigameManager is null, skipping registration of minigame-specific listeners.");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // Cancel tasks
        Bukkit.getScheduler().cancelTasks(this);

        // Unregister listeners
        HandlerList.unregisterAll(this);
        if (minigameManager != null) { // Ensure manager exists before shutting down
            minigameManager.shutdown();
        }

        saveConfig();
        getLogger().info("[ArenaRegenerator] Plugin disabled!");

    }

    /**
     * Loads all minigame configurations from the config.yml.
     */
    private void loadMinigameConfigs() {
        getLogger().info("[ArenaRegenerator] Loading minigame configurations from config.yml...");
        minigameConfigs.clear(); // Clear previous configs on reload
        ConfigurationSection minigamesSection = getConfig().getConfigurationSection("minigames");
        if (minigamesSection != null) {
            Set<String> keys = minigamesSection.getKeys(false);
            if (keys.isEmpty()) {
                getLogger().warning("[ArenaRegenerator] 'minigames' section found, but it contains no arena configurations.");
            } else {
                getLogger().info("[ArenaRegenerator] Found " + keys.size() + " minigame arena configurations: " + keys);
            }

            for (String arenaName : keys) {
                ConfigurationSection arenaConfig = minigamesSection.getConfigurationSection(arenaName);
                if (arenaConfig != null) {
                    minigameConfigs.put(arenaName, arenaConfig);
                    // Detailed logging for each arena is now in MinigameManager's loadConfiguredMinigames
                }
            }
        } else {
            getLogger().warning("[ArenaRegenerator] No 'minigames' section found in config.yml. Minigame features might not be fully available.");
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
     * Returns the configured cooldown in seconds for sign usage.
     * @return The cooldown in seconds, or -1 if disabled.
     */
    public int getSignUseCooldownSeconds() {
        return signUseCooldownSeconds;
    }

    /**
     * Returns the instance of the MinigameManager.
     * @return The MinigameManager instance.
     */
    public MinigameManager getMinigameManager() {
        return minigameManager;
    }

    public ArenaDataManager getArenaDataManager() {
        return arenaDataManager;
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
        getLogger().info("[ArenaRegenerator] Reloading plugin configuration...");
        // Reload the config file from disk
        reloadConfig();
        getLogger().info("[ArenaRegenerator] Configuration file reloaded from disk.");
        this.signUseCooldownSeconds = getConfig().getInt("sign-use-cooldown-seconds", -1);
        // Reload minigame configurations into ArenaRegenerator's internal map
        loadMinigameConfigs();

        HandlerList.unregisterAll(this);

        // Recreate manager
        try {
            this.minigameManager = new MinigameManager(this, arenaDataManager);
        } catch (Exception e) {
            getLogger().severe("[ArenaRegenerator] Failed to initialize MinigameManager: " + e.getMessage());
            this.minigameManager = null;
        }

        // Re-register listeners
        getServer().getPluginManager().registerEvents(new ArenaSignListener(minigameManager), this);
        if (minigameManager != null) {
            getServer().getPluginManager().registerEvents(new MinigameBlockListener(minigameManager), this);
            getServer().getPluginManager().registerEvents(new MinigameDamageListener(minigameManager), this);
        } else {
            getLogger().warning("[ArenaRegenerator] MinigameManager is null, skipping registration of minigame-specific listeners.");
        }
    }
}
