package io.github.woodstop.arenaRegenerator.Listeners;

import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

/**
 * Listener to handle block break and place events within minigames,
 * enforcing rules based on allowed breakable/placeable blocks.
 */
public class MinigameBlockListener implements Listener {

    private final MinigameManager minigameManager; // Now directly holds the manager instance
    private final Plugin plugin;

    // Constructor now takes MinigameManager directly
    public MinigameBlockListener(MinigameManager minigameManager) {
        this.minigameManager = minigameManager;
        this.plugin = minigameManager.getPlugin();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (minigameManager.isPlayerInMinigame(player)) {
            String arenaName = minigameManager.getPlayerArenaName(player);
            MinigameArena arena = minigameManager.getMinigameArena(arenaName);

            if (arena != null) {
                //plugin.getLogger().info("[MinigameBlockListener] Player " + player.getName() + " attempting to break " + event.getBlock().getType() + " in arena " + arenaName + ". Current state: " + arena.getCurrentState());
                if (arena.getCurrentState() == MinigameArena.GameState.IN_GAME) {
                    // Check if the block is in the allowed breakable blocks list
                    if (!arena.getBreakableBlocks().contains(event.getBlock().getType())) {
                        event.setCancelled(true);
                        // plugin.getLogger().info("[MinigameBlockListener] Block " + event.getBlock().getType() + " is NOT breakable for " + arenaName + ". Allowed: " + arena.getBreakableBlocks());
                    }// else {
                        // plugin.getLogger().info("[MinigameBlockListener] Block " + event.getBlock().getType() + " IS breakable for " + arenaName + ".");
                    //}
                } else {
                    // If in lobby/countdown/ending, prevent breaking any blocks
                    event.setCancelled(true);
                  // plugin.getLogger().info("[MinigameBlockListener] Block break cancelled: Game not IN_GAME for " + arenaName + ".");
                }
            } else {
                plugin.getLogger().warning("[MinigameBlockListener] Could not get MinigameArena for " + arenaName + " for player " + player.getName() + " during block break.");
            }

            if (!arena.getAllowItemDrops()) {
                event.setDropItems(false); // Prevent items from dropping
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (minigameManager.isPlayerInMinigame(player)) {
            String arenaName = minigameManager.getPlayerArenaName(player);
            MinigameArena arena = minigameManager.getMinigameArena(arenaName);

            if (arena != null) {
                //plugin.getLogger().info("[MinigameBlockListener] Player " + player.getName() + " attempting to place " + event.getBlockPlaced().getType() + " in arena " + arenaName + ". Current state: " + arena.getCurrentState());

                if (arena.getCurrentState() == MinigameArena.GameState.IN_GAME) {
                    // Check if the block is in the allowed placeable blocks list
                    if (!arena.getPlaceableBlocks().contains(event.getBlockPlaced().getType())) {
                        event.setCancelled(true);
                        //player.sendMessage(ChatColor.RED + "You cannot place this block in the arena!");
                        //plugin.getLogger().info("[MinigameBlockListener] Block " + event.getBlockPlaced().getType() + " is NOT placeable for " + arenaName + ". Allowed: " + arena.getPlaceableBlocks());
                    } else {
                        //plugin.getLogger().info("[MinigameBlockListener] Block " + event.getBlockPlaced().getType() + " IS placeable for " + arenaName + ".");
                    }
                } else {
                    // If in lobby/countdown/ending, prevent placing any blocks
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot place blocks while waiting for the game to start or end.");
                   plugin.getLogger().info("[MinigameBlockListener] Block place cancelled: Game not IN_GAME for " + arenaName + ".");
                }
            } else {
                plugin.getLogger().warning("[MinigameBlockListener] Could not get MinigameArena for " + arenaName + " for player " + player.getName() + " during block place.");
            }
        }
    }
}
