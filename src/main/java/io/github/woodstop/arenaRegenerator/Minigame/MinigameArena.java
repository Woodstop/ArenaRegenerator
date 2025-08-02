package io.github.woodstop.arenaRegenerator.Minigame;

import com.sk89q.worldedit.regions.Region;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.Managers.MinigameScoreboardManager;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
    private final List<ItemStack> itemsOnJoin;
    private final GameMode gameModeOnJoin;
    private final boolean allowItemDrops;
    private final boolean preventItemDurabilityLoss;

    private final Set<Material> breakableBlocks;
    private final Set<Material> placeableBlocks;
    private final List<String> winnerRewardCommands;

    private final boolean preventDamage;

    // Arena state
    private final List<UUID> playersInLobby; // Players waiting in lobby
    private final List<UUID> playersInGame;  // Players currently playing
    private final List<UUID> playersSpectating; // Players spectating
    private final Set<UUID> playersWhoParticipatedThisRound; // Players who participated this round
    private GameState currentState;
    private BukkitTask countdownTask;
    private BukkitTask gameTimerTask;
    private int currentCountdown;
    private int currentGameTime;
    private final Region arenaRegion;
    // Enum for game states
    public enum GameState {
        WAITING, COUNTDOWN, IN_GAME, ENDING
    }
    private long gameStartTick; // To store the server tick when the game officially started
    private final int BOUNDARY_CHECK_GRACE_PERIOD_TICKS = 40; // 2 seconds grace period (20 ticks per second)
    private final MinigameScoreboardManager scoreboardManager;

    public MinigameArena(ArenaRegenerator plugin, String arenaName, ConfigurationSection config, ArenaDataManager arenaDataManager) throws IOException {
        this.plugin = plugin;
        this.arenaName = arenaName;
        this.config = config;
        this.arenaDataManager = arenaDataManager;
        this.scoreboardManager = new MinigameScoreboardManager(plugin, arenaName);

        // Load settings from config
        this.minPlayers = config.getInt("min-players", 2);
        this.maxPlayers = config.getInt("max-players", 8);
        this.gameDurationSeconds = config.getInt("game-duration-seconds", 180);
        this.lobbyCountdownSeconds = config.getInt("lobby-countdown-seconds", 10);
        this.restorePlayerStateOnExit = config.getBoolean("restore-player-state-on-exit", true);

        this.lobbySpawn = arenaDataManager.loadSpawnLocation(arenaName, "lobby-spawn");
        this.exitSpawn = arenaDataManager.loadSpawnLocation(arenaName, "exit-spawn");
        this.spectatorSpawn = arenaDataManager.loadSpawnLocation(arenaName, "spectator-spawn");
        this.gameSpawnPoints = arenaDataManager.loadGameSpawnPoints(arenaName);

        // Log loaded spawn points for debugging
        // if (this.lobbySpawn == null) plugin.getLogger().warning("[MinigameArena] Arena '" + arenaName + "': Lobby spawn not set in arenas.json.");
        // if (this.exitSpawn == null) plugin.getLogger().warning("[MinigameArena] Arena '" + arenaName + "': Exit spawn not set in arenas.json.");
        // if (this.spectatorSpawn == null) plugin.getLogger().warning("[MinigameArena] Arena '" + arenaName + "': Spectator spawn not set in arenas.json.");
        // if (this.gameSpawnPoints.isEmpty()) plugin.getLogger().warning("[MinigameArena] Arena '" + arenaName + "': No game spawn points set in arenas.json.");

        this.clearInventoryOnJoin = config.getBoolean("clear-inventory-on-join", true);
        this.itemsOnJoin = loadItemsOnJoin();
        this.gameModeOnJoin = GameMode.valueOf(config.getString("game-mode-on-join", "ADVENTURE").toUpperCase());
        this.breakableBlocks = config.getStringList("breakable-blocks").stream()
                .map(s -> {
                    try {
                        return Material.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material name '" + s + "' in breakable-blocks for arena '" + arenaName + "'. Skipping.");
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        this.placeableBlocks = config.getStringList("placeable-blocks").stream()
                .map(s -> {
                    try {
                        return Material.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material name '" + s + "' in placeable-blocks for arena '" + arenaName + "'. Skipping.");
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        this.preventDamage = config.getBoolean("prevent-damage", true);
        this.allowItemDrops = config.getBoolean("item-drops", true);
        this.preventItemDurabilityLoss = config.getBoolean("prevent-item-durability-loss", true);
        this.winnerRewardCommands = loadWinnerRewards();

        this.playersInLobby = new ArrayList<>();
        this.playersInGame = new ArrayList<>();
        this.playersSpectating = new ArrayList<>();
        this.playersWhoParticipatedThisRound = new HashSet<>();
        this.currentState = GameState.WAITING;

        this.arenaRegion = arenaDataManager.getMinigamePlayableRegion(arenaName);
        if (this.arenaRegion == null) {
            throw new IOException("Failed to load WorldEdit region for arena '" + arenaName + "'. Ensure it's saved correctly and its world is loaded.");
        }
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
        if (lobbySpawn == null) { // NEW: Check if lobby spawn is set
            player.sendMessage(ChatColor.RED + "Lobby spawn not set for this arena. Cannot join.");
            plugin.getLogger().warning("Player " + player.getName() + " tried to join arena " + arenaName + " but lobby spawn is null.");
            return false;
        }

        playersInLobby.add(player.getUniqueId());
        playersWhoParticipatedThisRound.add(player.getUniqueId());
        player.teleport(lobbySpawn);
        applyPlayerSettingsOnJoin(player);
        player.sendMessage(ChatColor.GREEN + "Welcome to the lobby for " + arenaName + "!");
        broadcast(ChatColor.YELLOW + player.getName() + " joined the lobby (" + playersInLobby.size() + "/" + maxPlayers + ").");
        scoreboardManager.createScoreboard(player);
        updateScoreboardsForAllPlayers();

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
        scoreboardManager.removeScoreboard(player);
        broadcast(ChatColor.YELLOW + player.getName() + " left the arena.");


        // Manager will handle restoring state or teleporting to exit spawn
        // This method only handles removal from internal lists.
        checkEndCondition();
        updateScoreboardsForAllPlayers();
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

        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setFireTicks(0);
        player.setFallDistance(0);
        player.setExp(0);
        player.setLevel(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

        new BukkitRunnable() {
            @Override
            public void run() {
                // Re-check if player is still online before setting game mode
                if (player.isOnline()) {
                    player.setGameMode(gameModeOnJoin);
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
            }
        }.runTaskLater(plugin, 1L); // Run 1 tick later
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
        updateScoreboardsForAllPlayers();

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            currentCountdown--;
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
            updateScoreboardsForAllPlayers();
        }, 3L, 20L); // Every 1 second (20 ticks)
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
        updateScoreboardsForAllPlayers();
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

                // Give configured items
                for (ItemStack item : itemsOnJoin) {
                    p.getInventory().addItem(item.clone()); // Use clone to prevent modifying the original ItemStack
                }

            } else if (p != null) {
                p.sendMessage(ChatColor.RED + "No spawn points configured for this arena!");
                // Teleport to lobby spawn if game spawn points are missing
                p.teleport(lobbySpawn != null ? lobbySpawn : p.getWorld().getSpawnLocation());
            }
        }

        broadcast(ChatColor.GREEN + "The game is now in progress!");
        this.gameStartTick = plugin.getServer().getCurrentTick(); // Record the exact tick the game started
        startArenaTimer();
        // Start the continuous scoreboard update task now that the game is IN_GAME
        scoreboardManager.startScoreboardUpdateTask(this);
    }

    /**
     * Starts the game timer.
     */
    private void startArenaTimer() {
        currentGameTime = gameDurationSeconds;
        gameTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentGameTime <= 0) {
                endGame();
                return;
            }

            if (currentState == GameState.IN_GAME && (plugin.getServer().getCurrentTick() - gameStartTick) > BOUNDARY_CHECK_GRACE_PERIOD_TICKS) {
                for (UUID uuid : playersInGame) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && !isInsideArena(p)) {
                        p.sendMessage(ChatColor.RED + "You are now spectating!");
                        // Remove from in-game, add to spectating
                        playersInGame.remove(uuid);
                        playersSpectating.add(uuid);

                        // Teleport to spectator spawn and set gamemode
                        if (spectatorSpawn != null) {
                            p.teleport(spectatorSpawn);
                        } else {
                            // Fallback to a random game spawn point if spectator spawn is not set
                            Location randomGameSpawn = getRandomGameSpawnPoint();
                            if (randomGameSpawn != null) {
                                p.sendMessage(ChatColor.YELLOW + "No spectator spawn set. Teleporting to a game spawn point.");
                                p.teleport(randomGameSpawn);
                                plugin.getLogger().warning("No spectator spawn set for arena " + arenaName + ". Player " + p.getName() + " was teleported to a random game spawn.");
                            }
                        }
                        p.setGameMode(GameMode.SPECTATOR);
                        p.getInventory().clear(); // Clear inventory for spectators
                        p.getInventory().setArmorContents(null);
                    }
                }
                // After iterating through all players, re-check end condition if game hasn't already ended
                // This is important for scenarios like time running out, or the last player leaving voluntarily
                // or if multiple players leave bounds in quick succession but the game didn't end on the first one.
                checkEndCondition();
            }

            // Example: Broadcast time remaining
            if (currentGameTime % 60 == 0 || currentGameTime <= 10) { // Every minute or last 10 seconds
                broadcast(ChatColor.YELLOW + "Time remaining: " + currentGameTime + " seconds.");
            }
            currentGameTime--;
        }, 5L, 20L); // Every 1 second (20 ticks)
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

       cancelAllTasks();

        String winnerName = "No one";
        Player winner;
        if (playersInGame.size() == 1) {
            winner = Bukkit.getPlayer(playersInGame.get(0));
            if (winner != null) {
                winnerName = winner.getName();
                winner.sendMessage(ChatColor.GOLD + "Congratulations! You won the game in " + arenaName + "!");
            }
        } else {
            winner = null;
        }

        // Broadcasts winner to all players
        final String finalWinnerName = winnerName;

        List<UUID> playersToProcess = new ArrayList<>(playersWhoParticipatedThisRound); // Make a copy
        plugin.getLogger().info("[MinigameArena] Processing " + playersToProcess.size() + " players for end-game broadcast and state restoration.");

        // Process scoreboard removal immediately on this tick for all players
        for (UUID uuid : playersToProcess) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                scoreboardManager.removeScoreboard(p);
                plugin.getLogger().info("[MinigameArena] Scoreboard removed for " + p.getName() + " on current tick.");
            }
        }

        // Clear all internal player lists immediately on this tick
        playersInLobby.clear();
        playersInGame.clear();
        playersSpectating.clear();
        playersWhoParticipatedThisRound.clear(); // Clear for next round
        plugin.getLogger().info("[MinigameArena] All internal player lists cleared.");

        // Schedule other player processing (messages, leaveMinigame) for the next tick
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : playersToProcess) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) { // Only process if player is still online
                        p.sendMessage(ChatColor.GOLD + "Game in " + arenaName + " has ended! Winner: " + finalWinnerName);
                        plugin.getMinigameManager().leaveMinigame(p, restorePlayerStateOnExit);
                    } else {
                        plugin.getLogger().info("[MinigameArena] Player UUID " + uuid + " not online for delayed end-game processing.");
                    }
                }
                // Regenerate the arena
                resetArena();
                currentState = GameState.WAITING; // Reset state for next game
                // Give rewards to winner
                if (winner != null && winnerRewardCommands != null) {
                    for (String command : winnerRewardCommands) {
                        String parsedCommand = command.replace("%player%", winner.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
                    }
                    winner.sendMessage(ChatColor.GOLD + "You received rewards for winning!");
                }
            }
        }.runTaskLater(plugin, 30L); // Delay 1.5 seconds before teleporting them back
    }

    /**
     * Resets the arena by clearing blocks and regenerating the schematic.
     */
    public void resetArena() {
        plugin.getLogger().info("Resetting arena: " + arenaName);
        // Dispatch commands as console to use existing logic
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "arena clear " + arenaName);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "arena regen " + arenaName);
        playersWhoParticipatedThisRound.clear();
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

    /**
     * Checks if a player is currently inside the defined WorldEdit region of this arena.
     * @param player The player to check.
     * @return true if the player is inside the arena region, false otherwise.
     */
    private boolean isInsideArena(Player player) {
        if (arenaRegion == null || !arenaRegion.getWorld().getName().equals(player.getWorld().getName())) {
            // If arena region is not defined, or player is in a different world, consider them outside for safety
            return false;
        }
        // Check if the player's current block location is within the arena region
        return arenaRegion.contains(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
    }

    /**
     * Loads the list of ItemStacks to be given to players upon joining from the config.
     * @return A list of ItemStack objects.
     */
    private List<ItemStack> loadItemsOnJoin() {
        List<ItemStack> loadedItems = new ArrayList<>();
        List<String> itemStrings = config.getStringList("give-item-on-join");
        for (String itemString : itemStrings) {
            try {
                String[] parts = itemString.split(":");
                Material material = Material.valueOf(parts[0].toUpperCase());
                int amount = 1;
                if (parts.length > 1) {
                    amount = Integer.parseInt(parts[1]);
                }
                loadedItems.add(new ItemStack(material, amount));
                plugin.getLogger().info("[MinigameArena] Arena '" + arenaName + "': Configured to give item: " + itemString);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[MinigameArena] Arena '" + arenaName + "': Invalid item string in config 'give-item-on-join': " + itemString + ". Skipping this item.");
            }
        }
        return loadedItems;
    }

    /**
     * Loads the list of winner reward commands from the config.
     * @return A list of reward command strings.
     */
    private List<String> loadWinnerRewards() {
        List<String> rewardCommands = config.getStringList("winner-rewards.commands");

        if (rewardCommands.isEmpty()) {
            plugin.getLogger().info("[MinigameArena] No winner reward commands found in config.");
        }

        return rewardCommands;
    }

    /**
     * Helper method to get all players currently in this arena (lobby, game, or spectating).
     * This is needed by MinigameScoreboardManager.
     * @return A combined list of UUIDs of all players in the arena.
     */
    public List<UUID> getAllPlayersInArena() {
        List<UUID> allPlayers = new ArrayList<>();
        allPlayers.addAll(playersInLobby);
        allPlayers.addAll(playersInGame);
        allPlayers.addAll(playersSpectating);
        return allPlayers;
    }

    /**
     * Helper method to update scoreboards for all players in the arena.
     * This centralizes the call to MinigameScoreboardManager.updateScoreboard.
     */
    private void updateScoreboardsForAllPlayers() {
        scoreboardManager.updateScoreboard(
                currentState,
                currentCountdown,
                currentGameTime,
                playersInGame.size(),
                playersInLobby.size(),
                minPlayers,
                maxPlayers,
                getAllPlayersInArena()
        );
    }

    /**
     * Retrieves a random game spawn point from the configured list.
     * @return A random Location from gameSpawnPoints, or null if no game spawn points are configured.
     */
    private Location getRandomGameSpawnPoint() {
        if (gameSpawnPoints.isEmpty()) {
            return null;
        }
        List<Location> spawns = new ArrayList<>(gameSpawnPoints.values());
        return spawns.get(ThreadLocalRandom.current().nextInt(spawns.size()));
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

    /**
     * Returns a set of the names of all configured game spawn points for this arena.
     * @return A Set of String representing the names of game spawn points.
     */
    public Set<String> getGameSpawnPointNames() {
        return gameSpawnPoints.keySet();
    }

    public boolean isRestorePlayerStateOnExit() {
        return restorePlayerStateOnExit;
    }

    public Set<Material> getBreakableBlocks() {
        return Collections.unmodifiableSet(breakableBlocks); // Return unmodifiable set
    }

    public Set<Material> getPlaceableBlocks() {
        return Collections.unmodifiableSet(placeableBlocks); // Return unmodifiable set
    }

    public boolean getPreventDamage() {
        return preventDamage;
    }

    public boolean getAllowItemDrops() {
        return allowItemDrops;
    }

    /**
     * Returns whether item durability loss is prevented in this arena.
     * @return true if item durability loss is prevented, false otherwise.
     */
    public boolean shouldPreventItemDurabilityLoss() {
        return preventItemDurabilityLoss;
    }
}
