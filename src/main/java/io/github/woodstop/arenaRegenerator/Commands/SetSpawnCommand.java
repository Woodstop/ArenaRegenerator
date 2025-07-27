package io.github.woodstop.arenaRegenerator.Commands;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class SetSpawnCommand implements CommandExecutor {

    private final ArenaRegenerator plugin;
    private final ArenaDataManager dataManager;

    public SetSpawnCommand() {
        this.plugin = ArenaRegenerator.getInstance();
        this.dataManager = plugin.getArenaDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            player.sendMessage(ChatColor.RED + "Usage: /arena setspawn <arenaName> <lobby|exit|spectator|game> [spawnName]");
            player.sendMessage(ChatColor.RED + "Note: [spawnName] is only for 'game' type.");
            return true;
        }

        String arenaName = args[0];
        String spawnType = args[1].toLowerCase(); // lobby, exit, spectator, or game
        String gameSpawnName = (args.length == 3) ? args[2] : null;

        try {
            // First, check if the arena schematic data exists in arenas.json
            if (!dataManager.arenaExists(arenaName)) {
                player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found in data file. Save the arena schematic first using /savearena.");
                return true;
            }

            Location playerLoc = player.getLocation();
            String fullSpawnPath; // This will be the key used in arenas.json

            switch (spawnType) {
                case "lobby":
                case "exit":
                case "spectator":
                    if (gameSpawnName != null) { // Ensure no extra name for these types
                        player.sendMessage(ChatColor.RED + "Error: Do not provide a spawnName for '" + spawnType + "' type.");
                        return true;
                    }
                    fullSpawnPath = spawnType + "-spawn"; // e.g., "lobby-spawn"
                    break;
                case "game":
                    if (gameSpawnName == null || gameSpawnName.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "Usage: /setspawn <arenaName> game <spawnName>");
                        return true;
                    }
                    fullSpawnPath = "game-spawn-points." + gameSpawnName; // e.g., "game-spawn-points.spawn1"
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Invalid spawn type. Use 'lobby', 'exit', 'spectator', or 'game'.");
                    return true;
            }

            // Use ArenaDataManager to save the spawn location to arenas.json
            dataManager.saveSpawnLocation(arenaName, fullSpawnPath, player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Spawn point '" + spawnType + (gameSpawnName != null ? " " + gameSpawnName : "") + "' set for arena '" + arenaName + "' at your current location!");

            // Reload the plugin to ensure MinigameManager picks up the new spawn points
            plugin.reloadPlugin();

        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Error saving spawn point: " + e.getMessage());
            plugin.getLogger().severe("IO error saving spawn point for arena '" + arenaName + "': " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "An unexpected error occurred: " + e.getMessage());
            plugin.getLogger().severe("Unexpected error setting spawn point for arena '" + arenaName + "': " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}
