package io.github.woodstop.arenaRegenerator.Managers;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import io.github.woodstop.arenaRegenerator.util.PlayerRestoreData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    // Map of Player -> PlayerRestoreData for players currently in a minigame
    private final Map<UUID, PlayerRestoreData> playerRestoreDataMap;

    public MinigameManager(ArenaRegenerator plugin, ArenaDataManager arenaDataManager) {
        this.plugin = plugin;
        this.arenaDataManager = arenaDataManager;
        this.activeMinigames = new HashMap<>();
        this.playerRestoreDataMap = new HashMap<>();
        loadConfiguredMinigames();
    }

    /**
     * Loads minigame configurations from the plugin's config and initializes MinigameArena instances.
     */
    private void loadConfiguredMinigames() {
        Set<String> arenaNames = plugin.getMinigameArenaNames();
        for (String arenaName : arenaNames) {
            ConfigurationSection arenaConfig = plugin.getMinigameConfig(arenaName);
            if (arenaConfig != null && arenaConfig.getBoolean("enabled", false)) {
                try {
                    // Initialize MinigameArena but don't activate it yet
                    MinigameArena minigameArena = new MinigameArena(plugin, arenaName, arenaConfig, arenaDataManager);
                    activeMinigames.put(arenaName, minigameArena);
                    plugin.getLogger().info("Minigame arena '" + arenaName + "' loaded and ready.");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load minigame arena '" + arenaName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Attempts to join a player to a minigame arena.
     * @param player The player to join.
     * @param arenaName The name of the arena to join.
     * @return true if the player successfully joined, false otherwise.
     */
    public boolean joinMinigame(Player player, String arenaName) {
        if (playerRestoreDataMap.containsKey(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a minigame!");
            return false;
        }

        MinigameArena arena = activeMinigames.get(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Minigame arena '" + arenaName + "' not found or not enabled.");
            return false;
        }

        // Save player's current state before joining
        playerRestoreDataMap.put(player.getUniqueId(), new PlayerRestoreData(player));

        // Delegate the actual join logic to the MinigameArena instance
        if (arena.addPlayer(player)) {
            player.sendMessage(ChatColor.GREEN + "You joined minigame arena: " + ChatColor.WHITE + arenaName);
            return true;
        } else {
            // If addPlayer failed, remove restore data as player didn't fully join
            playerRestoreDataMap.remove(player);
            return false;
        }
    }

    /**
     * Attempts to remove a player from a minigame arena and restore their state.
     * @param player The player to remove.
     * @param arenaName The name of the arena the player is in.
     * @param restoreState Whether to restore player's state or teleport to exit spawn.
     */
    public void leaveMinigame(Player player, String arenaName, boolean restoreState) {
        MinigameArena arena = activeMinigames.get(arenaName);
        if (arena != null) {
            arena.removePlayer(player);
        }

        PlayerRestoreData restoreData = playerRestoreDataMap.remove(player);
        if (restoreData != null && restoreState) {
            restoreData.restore(player);
            player.sendMessage(ChatColor.GREEN + "Your state has been restored!");
        } else if (arena != null && arena.getExitSpawn() != null) {
            player.teleport(arena.getExitSpawn());
            player.sendMessage(ChatColor.GREEN + "You have been teleported to the exit spawn.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "You left the minigame. No specific exit point or restore data found.");
        }
    }

    /**
     * Checks if a player is currently in any active minigame.
     * @param player The player to check.
     * @return true if the player is in a minigame, false otherwise.
     */
    public boolean isPlayerInMinigame(Player player) {
        return playerRestoreDataMap.containsKey(player);
    }

    /**
     * Gets the MinigameArena instance by name.
     * @param arenaName The name of the arena.
     * @return The MinigameArena instance, or null if not found.
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
     * Shuts down the MinigameManager, canceling all active game tasks
     * and preparing for re-initialization or plugin disable.
     */
    public void shutdown() {
        // Cancel all tasks for all active minigames
        activeMinigames.values().forEach(MinigameArena::cancelAllTasks);
        activeMinigames.clear(); // Clear the map of active minigames

        // Force all players out and restore their state if they are still in a minigame
        // This is important for /reload or plugin disable to prevent players getting stuck
        for (UUID playerId : playerRestoreDataMap.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Use the restore data directly, as the arena might be gone
                PlayerRestoreData restoreData = playerRestoreDataMap.get(playerId);
                if (restoreData != null) {
                    restoreData.restore(player);
                    player.sendMessage(ChatColor.YELLOW + "Minigame ended due to plugin reload/disable. Your state has been restored.");
                } else {
                    // Fallback teleport if restore data is somehow missing
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.sendMessage(ChatColor.YELLOW + "Minigame ended due to plugin reload/disable. You have been teleported to spawn.");
                }
            }
        }
        playerRestoreDataMap.clear(); // Clear all restore data
        plugin.getLogger().info("MinigameManager shut down.");
    }
}
