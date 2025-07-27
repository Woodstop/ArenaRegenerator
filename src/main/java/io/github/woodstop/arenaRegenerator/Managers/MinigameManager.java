package io.github.woodstop.arenaRegenerator.Managers;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import io.github.woodstop.arenaRegenerator.util.PlayerRestoreData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages all active minigame arenas and player states within them.
 */
public class MinigameManager {

    private final ArenaRegenerator plugin;
    private final ArenaDataManager arenaDataManager;
    // Map of arenaName -> MinigameArena instance
    private final Map<String, MinigameArena> activeMinigames;
    // Map of Player UUID -> PlayerRestoreData for players currently in a minigame
    private final Map<UUID, PlayerRestoreData> playerRestoreDataMap;
    // Map of Player UUID -> Arena Name for tracking which arena a player is currently in
    private final Map<UUID, String> playerCurrentArenaMap;

    public MinigameManager(ArenaRegenerator plugin, ArenaDataManager arenaDataManager) {
        this.plugin = plugin;
        this.arenaDataManager = arenaDataManager;
        this.activeMinigames = new HashMap<>();
        this.playerRestoreDataMap = new HashMap<>();
        this.playerCurrentArenaMap = new HashMap<>();
        loadConfiguredMinigames();
    }

    /**
     * Loads minigame configurations from the plugin's config and initializes MinigameArena instances.
     */
    private void loadConfiguredMinigames() {
        // Clear any existing minigame instances before reloading
        activeMinigames.values().forEach(MinigameArena::cancelAllTasks); // Cancel tasks for existing arenas
        activeMinigames.clear();

        Set<String> arenaNames = plugin.getMinigameArenaNames();
        if (arenaNames.isEmpty()) {
            plugin.getLogger().warning("No minigame arenas found in configuration or all are disabled.");
        }

        for (String arenaName : arenaNames) {
            ConfigurationSection arenaConfig = plugin.getMinigameConfig(arenaName);
            if (arenaConfig != null && arenaConfig.getBoolean("enabled", false)) {
                try {
                    MinigameArena minigameArena = new MinigameArena(plugin, arenaName, arenaConfig, arenaDataManager);
                    activeMinigames.put(arenaName, minigameArena);
                    plugin.getLogger().info("Minigame arena '" + arenaName + "' loaded and ready.");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load minigame arena '" + arenaName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            } else if (arenaConfig != null) {
                plugin.getLogger().info("Minigame arena '" + arenaName + "' found but is disabled (enabled: false).");
            } else {
                plugin.getLogger().warning("Minigame arena '" + arenaName + "' defined in plugin's internal map but config section not found.");
            }
        }
    }

    /**
     * Attempts to join a player to a minigame arena.
     *
     * @param player    The player to join.
     * @param arenaName The name of the arena to join.
     */
    public void joinMinigame(Player player, String arenaName) {
        if (playerRestoreDataMap.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already in a minigame!");
            return;
        }

        MinigameArena arena = activeMinigames.get(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Minigame arena '" + arenaName + "' not found or not enabled.");
            return;
        }

        // Save player's current state before joining
        playerRestoreDataMap.put(player.getUniqueId(), new PlayerRestoreData(player));

        // Delegate the actual join logic to the MinigameArena instance
        if (arena.addPlayer(player)) {
            playerCurrentArenaMap.put(player.getUniqueId(), arenaName); // Track which arena the player is in
            player.sendMessage(ChatColor.GREEN + "You joined minigame arena: " + ChatColor.WHITE + arenaName);
        } else {
            // If addPlayer failed, remove restore data as player didn't fully join
            playerRestoreDataMap.remove(player.getUniqueId());
        }
    }

    /**
     * Attempts to remove a player from a minigame arena and restore their state.
     * @param player The player to remove.
     * @param restoreState Whether to restore player's state or teleport to exit spawn.
     */
    public void leaveMinigame(Player player, boolean restoreState) {
        String arenaName = playerCurrentArenaMap.remove(player.getUniqueId()); // Get arena name from map and remove
        if (arenaName == null) {
            player.sendMessage(ChatColor.RED + "You are not currently in a minigame.");
            return;
        }

        MinigameArena arena = activeMinigames.get(arenaName);
        if (arena != null) {
            arena.removePlayer(player);
        } else {
            plugin.getLogger().warning("Arena " + arenaName + " not found in activeMinigames when " + player.getName() + " tried to leave.");
        }

        PlayerRestoreData restoreData = playerRestoreDataMap.remove(player.getUniqueId());
        Location exitSpawn = (arena != null) ? arena.getExitSpawn() : null;

        if (restoreState && restoreData != null) {
            // Case 1: Restore player state is true
            if (exitSpawn != null) {
                // If exit spawn exists, teleport them there, overriding the restored location
                restoreData.restoreWithoutLocation(player);
                player.teleport(exitSpawn);
                player.sendMessage(ChatColor.GREEN + "You have been teleported to the exit spawn.");
            } else { // If no exit spawn, restore the player's original location
                restoreData.restore(player);
                player.sendMessage(ChatColor.GREEN + "Your state has been fully restored.");
            }
        } else {
            // Case 2: Restore player state is false (or restoreData was null unexpectedly)
            if (exitSpawn != null) {
                player.teleport(exitSpawn);
                player.sendMessage(ChatColor.GREEN + "You have been teleported to the exit spawn.");
            } else {
                // Fallback if no exit spawn and no restore data (or restoreState is false)
                player.teleport(player.getWorld().getSpawnLocation()); // Teleport to current world's spawn
                player.sendMessage(ChatColor.YELLOW + "You left the minigame. No specific exit point or restore data found. Teleported to world spawn.");
                plugin.getLogger().warning("[MinigameManager] No exit spawn or restore data for player " + player.getName() + " after leaving arena " + arenaName + ". Teleported to world spawn.");
            }
        }
    }

    /**
     * Checks if a player is currently in any active minigame.
     * @param player The player to check.
     * @return true if the player is in a minigame, false otherwise.
     */
    public boolean isPlayerInMinigame(Player player) {
        return playerCurrentArenaMap.containsKey(player.getUniqueId());
    }

    /**
     * Gets the MinigameArena instance by name.
     * @param arenaName The name of the arena.
     * @return The MinigameArena instance, or null if not found or not enabled.
     */
    public MinigameArena getMinigameArena(String arenaName) {
        return activeMinigames.get(arenaName);
    }

    /**
     * Returns a set of names of all currently configured and enabled minigame arenas.
     * @return A set of minigame arena names.
     */
    public Set<String> getConfiguredMinigameNames() {
        return activeMinigames.keySet();
    }

    /**
     * NEW: Gets the name of the arena a player is currently in.
     * @param player The player to check.
     * @return The name of the arena, or null if the player is not in an arena.
     */
    public String getPlayerArenaName(Player player) {
        return playerCurrentArenaMap.get(player.getUniqueId());
    }

    /**
     * Returns the main plugin instance.
     * @return The ArenaRegenerator plugin instance.
     */
    public ArenaRegenerator getPlugin() {
        return plugin;
    }

    /**
     * Gets the MinigameArena a player is currently in.
     * This method correctly retrieves the arena using playerCurrentArenaMap and activeMinigames.
     * @param player The player.
     * @return The MinigameArena, or null if the player is not in one.
     */
    public MinigameArena getPlayerCurrentMinigame(Player player) {
        String arenaName = playerCurrentArenaMap.get(player.getUniqueId());
        if (arenaName != null) {
            return activeMinigames.get(arenaName); // Use the case-sensitive name to retrieve the arena object
        }
        return null;
    }


    /**
     * Shuts down the MinigameManager, canceling all active game tasks
     * and preparing for re-initialization or plugin disable.
     */
    public void shutdown() {
        // Cancel all tasks for all active minigames
        activeMinigames.values().forEach(MinigameArena::cancelAllTasks);
        activeMinigames.clear(); // Clear the map of active minigames

        // Create a copy of playerRestoreDataMap entries before clearing maps
        Map<UUID, PlayerRestoreData> playersToRestore = new HashMap<>(playerRestoreDataMap);

        // Clear tracking maps immediately after copying data for restoration
        playerRestoreDataMap.clear();
        playerCurrentArenaMap.clear();

        // Now, restore players using the copied data
        for (Map.Entry<UUID, PlayerRestoreData> entry : playersToRestore.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerRestoreData restoreData = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (restoreData != null) {
                    restoreData.restore(player);
                    player.sendMessage(ChatColor.YELLOW + "Minigame ended due to plugin reload/disable. Your state has been restored.");
                } else {
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.sendMessage(ChatColor.YELLOW + "Minigame ended due to plugin reload/disable. You have been teleported to spawn.");
                }
            }
        }
        plugin.getLogger().info("MinigameManager shut down.");
    }
}
