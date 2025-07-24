package io.github.woodstop.arenaRegenerator.Minigame;

import com.sk89q.worldedit.regions.Region;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a single instance of a minigame arena, managing its state, players, and game flow.
 */
public class MinigameArena {

    private final ArenaRegenerator plugin;
    private final String arenaName;
    private final ArenaDataManager arenaDataManager;
    private final ConfigurationSection config;

    // Game configuration settings
    private final int minPlayers;
    private final int maxPlayers;
    private final int gameDurationSeconds;
    private final int lobbyCountdownSeconds;
    private final boolean restorePlayerStateOnExit;
    private final Location lobbySpawn;
    private final Map<String, Location> gameSpawnPoints; // Named spawn points
    private final Location exitSpawn;
    private final Location spectatorSpawn;
    private final boolean clearInventoryOnJoin;
    private final GameMode gameModeOnJoin;
    private final boolean preventBlockBreak;
    private final boolean preventBlockPlace;
    private final boolean preventDamage;

    // Arena state
    private final List<UUID> playersInLobby; // Players waiting in lobby
    private final List<UUID> playersInGame;  // Players currently playing
    private final List<UUID> playersSpectating; // Players spectating
    private GameState currentState;
    private BukkitTask countdownTask;
    private BukkitTask gameTimerTask;
    private int currentCountdown;
    private int currentGameTime;

    // Enum for game states
    public enum GameState {
        WAITING, COUNTDOWN, IN_GAME, ENDING
    }

    public MinigameArena(ArenaRegenerator plugin, String arenaName, ConfigurationSection config, ArenaDataManager arenaDataManager) {
        this.plugin = plugin;
        this.arenaName = arenaName;
        this.config = config;
        this.arenaDataManager = arenaDataManager;

        // Load settings from config
        this.minPlayers = config.getInt("min-players", 2);
        this.maxPlayers = config.getInt("max-players", 8);
        this.gameDurationSeconds = config.getInt("game-duration-seconds", 180);
        this.lobbyCountdownSeconds = config.getInt("lobby-countdown-seconds", 10);
        this.restorePlayerStateOnExit = config.getBoolean("restore-player-state-on-exit", true);

        this.lobbySpawn = parseLocation(config.getConfigurationSection("lobby-spawn"));
        this.exitSpawn = parseLocation(config.getConfigurationSection("exit-spawn"));
        this.spectatorSpawn = parseLocation(config.getConfigurationSection("spectator-spawn"));

        this.gameSpawnPoints = new HashMap<>();
        ConfigurationSection spawnPointsSection = config.getConfigurationSection("game-spawn-points");
        if (spawnPointsSection != null) {
            for (String spawnName : spawnPointsSection.getKeys(false)) {
                Location spawnLoc = parseLocation(spawnPointsSection.getConfigurationSection(spawnName));
                if (spawnLoc != null) {
                    this.gameSpawnPoints.put(spawnName, spawnLoc);
                } else {
                    plugin.getLogger().warning("Invalid game spawn point '" + spawnName + "' for arena '" + arenaName + "' in config.yml.");
                }
            }
        }

        this.clearInventoryOnJoin = config.getBoolean("clear-inventory-on-join", true);
        this.gameModeOnJoin = GameMode.valueOf(config.getString("game-mode-on-join", "ADVENTURE").toUpperCase());
        this.preventBlockBreak = config.getBoolean("prevent-block-break", true);
        this.preventBlockPlace = config.getBoolean("prevent-block-place", true);
        this.preventDamage = config.getBoolean("prevent-damage", false);

        this.playersInLobby = new ArrayList<>();
        this.playersInGame = new ArrayList<>();
        this.playersSpectating = new ArrayList<>();
        this.currentState = GameState.WAITING;
    }

    /**
     * Helper to parse a Location from a ConfigurationSection.
     */
    private Location parseLocation(ConfigurationSection section) {
        if (section == null) return null;
        String worldName = section.getString("world");
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' for arena '" + arenaName + "' is not loaded or does not exist!");
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Adds a player to the arena lobby.
     * @param player The player to add.
     * @return true if player was added, false otherwise (e.g., arena full).
     */
    public boolean addPlayer(Player player) {
        if (currentState == GameState.IN_GAME || playersInLobby.size() >= maxPlayers) {
            player.sendMessage(ChatColor.RED + "This arena is currently in-game or full.");
            return false;
        }
        if (playersInLobby.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You are already in the lobby for this arena.");
            return false;
        }

        playersInLobby.add(player.getUniqueId());
        player.teleport(lobbySpawn);
        applyPlayerSettingsOnJoin(player);
        player.sendMessage(ChatColor.GREEN + "Welcome to the lobby for " + arenaName + "!");
        broadcast(ChatColor.YELLOW + player.getName() + " joined the lobby (" + playersInLobby.size() + "/" + maxPlayers + ").");

        checkStartCondition();
        return true;
    }

    /**
     * Removes a player from the arena (lobby, game, or spectator).
     * @param player The player to remove.
     */
    public void removePlayer(Player player) {
        playersInLobby.remove(player.getUniqueId());
        playersInGame.remove(player.getUniqueId());
        playersSpectating.remove(player.getUniqueId());
        broadcast(ChatColor.YELLOW + player.getName() + " left the arena.");

        // Manager will handle restoring state or teleporting to exit spawn
        // This method only handles removal from internal lists.
        checkEndCondition();
    }

    /**
     * Handles applying settings to a player when they join the arena.
     * @param player The player.
     */
    private void applyPlayerSettingsOnJoin(Player player) {
        if (clearInventoryOnJoin) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null); // Clear armor
        }
        player.setGameMode(gameModeOnJoin);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setExp(0);
        player.setLevel(0);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    /**
     * Checks if the game can start (enough players).
     */
    private void checkStartCondition() {
        if (currentState == GameState.WAITING && playersInLobby.size() >= minPlayers) {
            startCountdown();
        }
    }

    /**
     * Starts the game countdown.
     */
    private void startCountdown() {
        currentState = GameState.COUNTDOWN;
        currentCountdown = lobbyCountdownSeconds;
        broadcast(ChatColor.GREEN + "Game starting in " + currentCountdown + " seconds!");

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentCountdown <= 0) {
                startGame();
                countdownTask.cancel();
                return;
            }
            if (playersInLobby.size() < minPlayers) {
                broadcast(ChatColor.RED + "Not enough players! Countdown cancelled.");
                cancelCountdown();
                return;
            }

            if (currentCountdown <= 5 || currentCountdown % 5 == 0) {
                broadcast(ChatColor.YELLOW + "Game starts in " + currentCountdown + " seconds...");
            }
            currentCountdown--;
        }, 0L, 20L); // Every 1 second (20 ticks)
    }

    /**
     * Cancels the game countdown.
     */
    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        currentState = GameState.WAITING;
        broadcast(ChatColor.YELLOW + "Countdown cancelled. Waiting for players...");
    }

    /**
     * Starts the actual game.
     */
    private void startGame() {
        currentState = GameState.IN_GAME;
        playersInGame.addAll(playersInLobby); // Move players from lobby to game
        playersInLobby.clear();

        // Teleport players to game spawn points
        List<Location> availableSpawns = new ArrayList<>(gameSpawnPoints.values());
        Collections.shuffle(availableSpawns, ThreadLocalRandom.current()); // Randomize spawn order

        for (int i = 0; i < playersInGame.size(); i++) {
            Player p = Bukkit.getPlayer(playersInGame.get(i));
            if (p != null && !availableSpawns.isEmpty()) {
                p.teleport(availableSpawns.get(i % availableSpawns.size())); // Cycle through spawns
                p.sendMessage(ChatColor.GREEN + "The game has started!");
            } else if (p != null) {
                p.sendMessage(ChatColor.RED + "No spawn points configured for this arena!");
                // Teleport to lobby spawn if game spawn points are missing
                p.teleport(lobbySpawn != null ? lobbySpawn : p.getWorld().getSpawnLocation());
            }
        }

        broadcast(ChatColor.GREEN + "The game is now in progress!");
        startArenaTimer();
    }

    /**
     * Starts the game timer.
     */
    private void startArenaTimer() {
        currentGameTime = gameDurationSeconds;
        gameTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentGameTime <= 0) {
                endGame();
                gameTimerTask.cancel();
                return;
            }

            // Example: Broadcast time remaining
            if (currentGameTime % 60 == 0 || currentGameTime <= 10) { // Every minute or last 10 seconds
                broadcast(ChatColor.YELLOW + "Time remaining: " + currentGameTime + " seconds.");
            }
            currentGameTime--;
        }, 0L, 20L); // Every 1 second (20 ticks)
    }

    /**
     * Checks if the game should end (e.g., last man standing, time ran out).
     */
    private void checkEndCondition() {
        if (currentState == GameState.IN_GAME && playersInGame.size() <= 1) {
            endGame();
        }
    }

    /**
     * Ends the game, teleports players out, and resets the arena.
     */
    public void endGame() {
        if (currentState == GameState.ENDING) return; // Prevent double ending
        currentState = GameState.ENDING;

        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        String winnerName = "No one";
        if (playersInGame.size() == 1) {
            Player winner = Bukkit.getPlayer(playersInGame.get(0));
            if (winner != null) {
                winnerName = winner.getName();
                winner.sendMessage(ChatColor.GOLD + "Congratulations! You won the game in " + arenaName + "!");
            }
        }
        broadcast(ChatColor.GOLD + "Game in " + arenaName + " has ended! Winner: " + winnerName);

        // Teleport all remaining players out and restore state
        List<UUID> allPlayers = new ArrayList<>();
        allPlayers.addAll(playersInGame);
        allPlayers.addAll(playersInLobby); // Players still in lobby if game ended early
        allPlayers.addAll(playersSpectating);

        for (UUID uuid : allPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                // Delegate to MinigameManager to handle restoration/teleport
                plugin.getMinigameManager().leaveMinigame(p, arenaName, restorePlayerStateOnExit);
            }
        }
        playersInLobby.clear();
        playersInGame.clear();
        playersSpectating.clear();

        // Regenerate the arena
        resetArena();
        currentState = GameState.WAITING; // Reset state for next game
    }

    /**
     * Resets the arena by clearing blocks and regenerating the schematic.
     */
    public void resetArena() {
        plugin.getLogger().info("Resetting arena: " + arenaName);
        // Dispatch commands as console to use existing logic
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "arena clear " + arenaName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "arena regen " + arenaName);
    }

    /**
     * Sends a message to all players currently in this arena (lobby, game, or spectating).
     * @param message The message to send.
     */
    private void broadcast(String message) {
        List<UUID> allPlayers = new ArrayList<>();
        allPlayers.addAll(playersInLobby);
        allPlayers.addAll(playersInGame);
        allPlayers.addAll(playersSpectating);

        for (UUID uuid : allPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(message);
            }
        }
    }

    /**
     * Cancels a given BukkitTask if it's not null and not already cancelled.
     * @param task The BukkitTask to cancel.
     */
    public void cancelTask(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Cancels all active tasks associated with this minigame arena.
     * Called during shutdown or reload.
     */
    public void cancelAllTasks() {
        cancelTask(countdownTask);
        cancelTask(gameTimerTask);
        countdownTask = null;
        gameTimerTask = null;
    }

    // --- Getters for MinigameManager and other classes to use ---
    public String getArenaName() {
        return arenaName;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public int getPlayersInLobbyCount() {
        return playersInLobby.size();
    }

    public int getPlayersInGameCount() {
        return playersInGame.size();
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getCurrentCountdown() {
        return currentCountdown;
    }

    public int getCurrentGameTime() {
        return currentGameTime;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public Location getExitSpawn() {
        return exitSpawn;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public List<Location> getGameSpawnPoints() {
        return new ArrayList<>(gameSpawnPoints.values());
    }

    public boolean isRestorePlayerStateOnExit() {
        return restorePlayerStateOnExit;
    }
}
