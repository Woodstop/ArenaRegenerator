package io.github.woodstop.arenaRegenerator.Listeners;

import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena;
import org.bukkit.ChatColor;
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

        minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Players involved: Damager=" + damager.getName() + " (InMinigame: " + damagerInMinigame + "), Damaged=" + damaged.getName() + " (InMinigame: " + damagedInMinigame + ")");


        if (damagerInMinigame && damagedInMinigame) {
            String damagerArenaName = minigameManager.getPlayerArenaName(damager);
            String damagedArenaName = minigameManager.getPlayerArenaName(damaged);

           // minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Damager Arena: " + damagerArenaName + ", Damaged Arena: " + damagedArenaName);

            // Only apply rules if they are in the same minigame
            if (damagerArenaName != null && damagerArenaName.equals(damagedArenaName)) {
                MinigameArena arena = minigameManager.getMinigameArena(damagerArenaName);
                if (arena != null) {
                   // minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Damage attempt in arena " + arena.getArenaName() + ": Damager=" + damager.getName() + ", Damaged=" + damaged.getName() + ", PreventDamage=" + arena.getPreventDamage());

                    // If damage is prevented for this arena, cancel the event and notify both players
                    if (arena.getPreventDamage()) {
                        event.setCancelled(true);
                       // minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] PvP cancelled in " + arena.getArenaName() + ". Both players notified.");
                    } //else {
                       // minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] PvP allowed in " + arena.getArenaName() + ". Event not cancelled.");
                  //  }
                } else {
                    minigameManager.getPlugin().getLogger().warning("[MinigameDamageListener] Could not get MinigameArena for " + damagerArenaName + " during damage event.");
                }
            } else {
                // Players are in minigames but different ones, or one is in a minigame and the other isn't.
                // For now, allow damage between different arenas/non-arena players.
                // You might want more complex rules here depending on your game design.
                minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Damage between players in different minigames or one not in minigame. Damager in game: " + damagerInMinigame + ", Damaged in game: " + damagedInMinigame);
            }
        } else if (damagerInMinigame || damagedInMinigame) {
            // One player is in a minigame, the other is not.
            // By default, allow damage, but you could implement rules here.
            minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] Damage between minigame player and non-minigame player. Damager in game: " + damagerInMinigame + ", Damaged in game: " + damagedInMinigame);
        }
        // If neither is in a minigame, let the event pass (handled by other plugins or default Bukkit)
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

        if (arena != null && arena.getCurrentState() == MinigameArena.GameState.IN_GAME) {
            // If preventDamage is true, cancel all damage to players in this arena
            // This covers fall damage, environmental damage, etc.
            if (arena.getPreventDamage()) {
                event.setCancelled(true);
                // No message needed for self-inflicted/environmental damage usually
                minigameManager.getPlugin().getLogger().info("[MinigameDamageListener] General damage cancelled for " + player.getName() + " in arena " + arenaName + " (PreventDamage=true).");
            }
        }
    }
}
