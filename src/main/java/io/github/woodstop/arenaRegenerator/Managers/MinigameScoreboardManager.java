package io.github.woodstop.arenaRegenerator.Managers;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena; // Import MinigameArena for GameState enum
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages scoreboards for individual players within a minigame arena.
 */
public class MinigameScoreboardManager {

    private final ArenaRegenerator plugin;
    private final String arenaName; // The name of the arena this scoreboard manager belongs to
    private final ScoreboardManager scoreboardManager;
    private final Map<UUID, Scoreboard> playerScoreboards; // Each player gets their own scoreboard
    private BukkitTask scoreboardUpdateTask;

    public MinigameScoreboardManager(ArenaRegenerator plugin, String arenaName) {
        this.plugin = plugin;
        this.arenaName = arenaName;
        this.scoreboardManager = Bukkit.getScoreboardManager();
        this.playerScoreboards = new HashMap<>();
        plugin.getLogger().info("[MinigameScoreboardManager] Initialized for arena: " + arenaName);
    }

    /**
     * Creates and assigns a scoreboard to the given player.
     * @param player The player to assign the scoreboard to.
     */
    public void createScoreboard(Player player) {
        if (scoreboardManager == null) {
            plugin.getLogger().warning("[MinigameScoreboardManager] ScoreboardManager is null! Cannot create scoreboard for " + player.getName());
            return;
        }
        Scoreboard board = scoreboardManager.getNewScoreboard();
        Objective obj = board.registerNewObjective("minigame", "dummy", ChatColor.YELLOW + "" + ChatColor.BOLD + arenaName);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(board);
        playerScoreboards.put(player.getUniqueId(), board);
    }

    /**
     * Updates the scoreboard for all players currently associated with this arena.
     * This method receives all necessary data from MinigameArena.
     * @param currentState The current GameState of the arena.
     * @param currentCountdown The current countdown value (if in COUNTDOWN state).
     * @param currentGameTime The current game time remaining (if in IN_GAME state).
     * @param playersInGameCount The number of players currently in the game.
     * @param playersInLobbyCount The number of players currently in the lobby.
     * @param minPlayers The minimum players required for the game.
     * @param maxPlayers The maximum players allowed in the game.
     * @param allArenaPlayers A list of UUIDs of all players currently in the arena (lobby, game, spectating).
     */
    public void updateScoreboard(MinigameArena.GameState currentState, int currentCountdown, int currentGameTime,
                                 int playersInGameCount, int playersInLobbyCount, int minPlayers, int maxPlayers,
                                 List<UUID> allArenaPlayers) {

        for (UUID uuid : allArenaPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && playerScoreboards.containsKey(uuid)) {
                Scoreboard board = playerScoreboards.get(uuid);
                Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
                if (obj == null) { // Re-create objective if it somehow disappeared
                    obj = board.registerNewObjective("minigame", "dummy", ChatColor.YELLOW + "" + ChatColor.BOLD + arenaName);
                    obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                    p.setScoreboard(board); // Re-assign if needed
                }

                // Clear existing scores
                board.getEntries().forEach(board::resetScores);

                // Add new scores based on game state
                obj.getScore(ChatColor.GRAY + "--------------------").setScore(5); // Separator

                if (currentState == MinigameArena.GameState.WAITING) {
                    obj.getScore(ChatColor.WHITE + "Status: " + ChatColor.AQUA + "Waiting...").setScore(4);
                    obj.getScore(ChatColor.WHITE + "Players: " + ChatColor.YELLOW + playersInLobbyCount + "/" + maxPlayers).setScore(3);
                    obj.getScore(ChatColor.WHITE + "Min Players: " + ChatColor.YELLOW + minPlayers).setScore(2);
                } else if (currentState == MinigameArena.GameState.COUNTDOWN) {
                    obj.getScore(ChatColor.WHITE + "Status: " + ChatColor.GREEN + "Starting...").setScore(4);
                    obj.getScore(ChatColor.WHITE + "Starts in: " + ChatColor.YELLOW + currentCountdown + "s").setScore(3);
                    obj.getScore(ChatColor.WHITE + "Players: " + ChatColor.YELLOW + playersInLobbyCount + "/" + maxPlayers).setScore(2);
                } else if (currentState == MinigameArena.GameState.IN_GAME) {
                    obj.getScore(ChatColor.WHITE + "Status: " + ChatColor.RED + "In Game").setScore(4);
                    obj.getScore(ChatColor.WHITE + "Time Left: " + ChatColor.YELLOW + formatTime(currentGameTime)).setScore(3);
                    obj.getScore(ChatColor.WHITE + "Players Left: " + ChatColor.YELLOW + playersInGameCount).setScore(2);
                } else if (currentState == MinigameArena.GameState.ENDING) {
                    obj.getScore(ChatColor.WHITE + "Status: " + ChatColor.DARK_RED + "Ending...").setScore(4);
                    obj.getScore(ChatColor.WHITE + "Players Left: " + ChatColor.YELLOW + playersInGameCount).setScore(3);
                }
                obj.getScore(ChatColor.GRAY + "-------------------- ").setScore(1); // Another separator (with space to differentiate entry)
            }
        }
    }

    /**
     * Starts the repeating task to update scoreboards for all players in the arena.
     * This task will call the updateScoreboard method, which will then need to pull
     * the latest game state information from the MinigameArena.
     */
    public void startScoreboardUpdateTask(MinigameArena arena) {
        if (scoreboardUpdateTask == null || scoreboardUpdateTask.isCancelled()) {
            scoreboardUpdateTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // Pass all necessary current data from the arena to the update method
                    updateScoreboard(arena.getCurrentState(), arena.getCurrentCountdown(), arena.getCurrentGameTime(),
                            arena.getPlayersInGameCount(), arena.getPlayersInLobbyCount(),
                            arena.getMinPlayers(), arena.getMaxPlayers(),
                            arena.getAllPlayersInArena());
                }
            }.runTaskTimer(plugin, 3L, 20L); // Update every 1 second (20 ticks)
        }
    }

    /**
     * Cancels the repeating scoreboard update task.
     */
    public void cancelScoreboardUpdateTask() {
        if (scoreboardUpdateTask != null) {
            scoreboardUpdateTask.cancel();
            scoreboardUpdateTask = null;
        }
    }

    /**
     * Removes the scoreboard from a specific player.
     * @param player The player to remove the scoreboard from.
     */
    public void removeScoreboard(Player player) {
        if (playerScoreboards.containsKey(player.getUniqueId())) {
            try {
                // Set player's scoreboard back to an empty server scoreboard
                player.setScoreboard(scoreboardManager.getNewScoreboard());
                playerScoreboards.remove(player.getUniqueId());
            } catch (Exception e) {
                plugin.getLogger().severe("[MinigameScoreboardManager] Error removing scoreboard for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Formats time in seconds into a MM:SS string.
     * @param totalSeconds The total seconds.
     * @return Formatted time string.
     */
    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
