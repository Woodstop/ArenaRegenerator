package io.github.woodstop.arenaRegenerator.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;

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
    private final GameMode gameMode;
    private final boolean flying; // Whether player was flying
    private final boolean allowFlight;
    private final float flySpeed; // Player's fly speed
    private final float walkSpeed; // Player's walk speed
    private final int level;
    private final float exp;
    private final float exhaustion;
    private final Collection<PotionEffect> potionEffects;


    public PlayerRestoreData(Player player) {
        this.location = player.getLocation();
        this.inventory = player.getInventory().getContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.exhaustion = player.getExhaustion();
        this.saturation = player.getSaturation();
        this.gameMode = player.getGameMode();
        this.flying = player.isFlying();
        this.flySpeed = player.getFlySpeed();
        this.walkSpeed = player.getWalkSpeed();
        this.level = player.getLevel();
        this.exp = player.getExp();
        this.allowFlight = player.getAllowFlight();
        this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
    }

    /**
     * Restores the player's state to the saved data.
     * @param player The player to restore.
     */
    public void restore(Player player) {
        player.teleport(location);
        player.getInventory().clear(); // Clear current inventory before restoring
        // Ensure player is not invulnerable or similar states from game
        player.setFireTicks(0);
        player.setFallDistance(0);
        // Consider removing potion effects if they were added by the minigame
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));


        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor); // Restore armor
        player.setHealth(health);
        player.setFoodLevel(foodLevel);
        player.setExhaustion(this.exhaustion);
        player.setSaturation(saturation);
        player.setLevel(this.level);
        player.setExp(this.exp);
        player.setGameMode(gameMode);
        player.setFlying(flying);
        player.setFlySpeed(flySpeed);
        player.setWalkSpeed(walkSpeed);
        player.setAllowFlight(this.allowFlight);

        // Add previous potion effects
        for (PotionEffect effect : potionEffects) {
            player.addPotionEffect(effect);
        }
    }

    /**
     * Restores the player's state, but explicitly DOES NOT restore their location.
     * Use this when you want to teleport the player to a specific exit point instead.
     * @param player The player to restore.
     */
    public void restoreWithoutLocation(Player player) {
        player.getInventory().clear(); // Clear current inventory before restoring
        // Ensure player is not invulnerable or similar states from game
        player.setFireTicks(0);
        player.setFallDistance(0);
        // Consider removing potion effects if they were added by the minigame
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));


        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor); // Restore armor
        player.setHealth(health);
        player.setFoodLevel(foodLevel);
        player.setExhaustion(this.exhaustion);
        player.setSaturation(saturation);
        player.setLevel(this.level);
        player.setExp(this.exp);
        player.setGameMode(gameMode);
        player.setFlying(flying);
        player.setFlySpeed(flySpeed);
        player.setWalkSpeed(walkSpeed);
        player.setAllowFlight(this.allowFlight);

        // Add previous potion effects
        for (PotionEffect effect : potionEffects) {
            player.addPotionEffect(effect);
        }
    }
}
