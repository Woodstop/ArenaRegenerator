package io.github.woodstop.arenaRegenerator.Listeners;

import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
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

    // Check if the player has fallen into the void when in the lobby
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        MinigameArena arena = minigameManager.getPlayerCurrentMinigame(player);

        if (arena == null) {
            return; // Player is not in a minigame
        }

        // Check if player is in a lobby state (WAITING or COUNTDOWN)
        if (arena.getCurrentState() == MinigameArena.GameState.WAITING ||
                arena.getCurrentState() == MinigameArena.GameState.COUNTDOWN) {

            Location lobbySpawn = arena.getLobbySpawn();
            // Calculate the dynamic void threshold: 15 blocks below lobby spawn Y
            double voidThresholdY = lobbySpawn.getY() - 15;

            // Check if player has fallen into the void
            if (player.getLocation().getY() < voidThresholdY) {
                player.teleport(lobbySpawn);
            }
        }
    }
}
