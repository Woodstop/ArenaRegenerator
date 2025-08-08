package io.github.woodstop.arenaRegenerator.Listeners;

import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Listener to handle damage events within minigames, enforcing the preventDamage rule.
 */
public class MinigameDamageListener implements Listener {

    private final MinigameManager minigameManager; // Now directly holds the manager instance

    // Constructor now takes MinigameManager directly
    public MinigameDamageListener(MinigameManager minigameManager) {
        this.minigameManager = minigameManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Initial logging for all damage-by-entity events
        //   minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] EntityDamageByEntityEvent triggered. Damager: " + event.getDamager().getName() + ", Damaged: " + event.getEntity().getName());

        // Ensure both damager and damaged are players
        if (!(event.getDamager() instanceof Player damager)) {
            //   minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Damager is not a player. Skipping.");
            return;
        }
        if (!(event.getEntity() instanceof Player damaged)) {
            //   minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Damaged entity is not a player. Skipping.");
            return;
        }

        // Check if both players are in a minigame and the same minigame
        boolean damagerInMinigame = minigameManager.isPlayerInMinigame(damager);
        boolean damagedInMinigame = minigameManager.isPlayerInMinigame(damaged);

        //minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Players involved: Damager=" + damager.getName() + " (InMinigame: " + damagerInMinigame + "), Damaged=" + damaged.getName() + " (InMinigame: " + damagedInMinigame + ")");


        if (damagerInMinigame && damagedInMinigame) {
            String damagerArenaName = minigameManager.getPlayerArenaName(damager);
            String damagedArenaName = minigameManager.getPlayerArenaName(damaged);

            // minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Damager Arena: " + damagerArenaName + ", Damaged Arena: " + damagedArenaName);

            // Only apply rules if they are in the same minigame
            if (damagerArenaName != null && damagerArenaName.equals(damagedArenaName)) {
                MinigameArena arena = minigameManager.getMinigameArena(damagerArenaName);

                // If in WAITING or COUNTDOWN state, always prevent damage
                if (arena.getCurrentState() == MinigameArena.GameState.WAITING ||
                        arena.getCurrentState() == MinigameArena.GameState.COUNTDOWN) {
                    event.setCancelled(true);
                }
                // If in IN_GAME state, respect the preventDamage config
                else if (arena.getCurrentState() == MinigameArena.GameState.IN_GAME && arena.getPreventDamage()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Also listen for general EntityDamageEvent to prevent fall damage, environmental damage, etc.
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return; // Only care about players
        }

        // Check if player is in a minigame
        if (!minigameManager.isPlayerInMinigame(player)) {
            return; // Not in a minigame
        }

        String arenaName = minigameManager.getPlayerArenaName(player);
        MinigameArena arena = minigameManager.getMinigameArena(arenaName);

        // If in WAITING or COUNTDOWN state, always prevent damage
        if (arena.getCurrentState() == MinigameArena.GameState.WAITING ||
                arena.getCurrentState() == MinigameArena.GameState.COUNTDOWN) {
            event.setCancelled(true);
        }
        // If in IN_GAME state, respect the preventDamage config
        else if (arena.getCurrentState() == MinigameArena.GameState.IN_GAME && arena.getPreventDamage()) {
            event.setCancelled(true);
        }
    }
}
