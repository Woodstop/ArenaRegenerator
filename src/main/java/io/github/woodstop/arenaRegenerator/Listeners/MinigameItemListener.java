package io.github.woodstop.arenaRegenerator.Listeners;

import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Listener to handle item-related events within minigames, such as durability loss.
 */
public class MinigameItemListener implements Listener {

    private final MinigameManager minigameManager;

    public MinigameItemListener(MinigameManager minigameManager) {
        this.minigameManager = minigameManager;
    }

    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        MinigameArena arena = minigameManager.getPlayerCurrentMinigame(player);

        if (arena == null) {
            return; // Player is not in a minigame
        }

        // If the arena is configured to prevent item durability loss, cancel the event
        if (arena.shouldPreventItemDurabilityLoss()) {
            event.setCancelled(true);
        }
    }
}
