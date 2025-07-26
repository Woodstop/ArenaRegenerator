package io.github.woodstop.arenaRegenerator.Listeners;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent; // Import PlayerQuitEvent

/**
 * Listener to handle player-related events for minigames.
 */
public class MinigamePlayerListener implements Listener {

    private final MinigameManager minigameManager;

    public MinigamePlayerListener(MinigameManager minigameManager) {
        this.minigameManager = minigameManager;
    }

    /**
     * Handles players leaving the server. If a player is in a minigame,
     * they will automatically leave it.
     * @param event The PlayerQuitEvent.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        //plugin.getLogger().info("[MinigamePlayerListener] Player " + player.getName() + " quit the server.");

        // Check if the player is in a minigame
        if (minigameManager.isPlayerInMinigame(player)) {
            minigameManager.getPlugin().getLogger().info("[MinigamePlayerListener] Player " + player.getName() + " was in a minigame. Forcing them to leave.");
            // Force the player to leave the minigame and restore their state
            minigameManager.leaveMinigame(player, true);
            // No need to send message to player, as they have already quit
        }
    }
}
