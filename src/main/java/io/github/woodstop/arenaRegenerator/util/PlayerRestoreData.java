package io.github.woodstop.arenaRegenerator.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Stores a player's relevant data (location, inventory, stats)
 * to restore them to their original state after a minigame.
 */
public class PlayerRestoreData {
    private final Location location;
    private final ItemStack[] inventory;
    private final ItemStack[] armor; // Store armor separately
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int totalExperience; // Store total experience
    private final GameMode gameMode;
    private final boolean flying; // Whether player was flying
    private final float flySpeed; // Player's fly speed
    private final float walkSpeed; // Player's walk speed

    public PlayerRestoreData(Player player) {
        this.location = player.getLocation();
        this.inventory = player.getInventory().getContents();
        this.armor = player.getInventory().getArmorContents(); // Get armor contents
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.totalExperience = player.getTotalExperience(); // Get total experience
        this.gameMode = player.getGameMode();
        this.flying = player.isFlying();
        this.flySpeed = player.getFlySpeed();
        this.walkSpeed = player.getWalkSpeed();
    }

    /**
     * Restores the player's state to the saved data.
     * @param player The player to restore.
     */
    public void restore(Player player) {
        player.teleport(location);
        player.getInventory().clear(); // Clear current inventory before restoring
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor); // Restore armor
        player.setHealth(health);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setTotalExperience(totalExperience); // Restore total experience
        player.setGameMode(gameMode);
        player.setFlying(flying);
        player.setFlySpeed(flySpeed);
        player.setWalkSpeed(walkSpeed);

        // Ensure player is not invulnerable or similar states from game
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setAllowFlight(player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || flying); // Restore flight permission
        player.setExp(0); // Clear current level progress bar if needed
        player.setLevel(0); // Clear current level if needed
        // Consider removing potion effects if they were added by the minigame
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }
}
