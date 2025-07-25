package io.github.woodstop.arenaRegenerator.Commands;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DelSpawnCommand implements CommandExecutor {

    private final ArenaRegenerator plugin;
    private final ArenaDataManager dataManager;

    public DelSpawnCommand() {
        this.plugin = ArenaRegenerator.getInstance();
        this.dataManager = plugin.getArenaDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena delspawn <arenaName> <lobby|exit|spectator|game> [spawnName]");
            sender.sendMessage(ChatColor.RED + "Note: [spawnName] is only for 'game' type.");
            return true;
        }

        String arenaName = args[0];
        String spawnType = args[1].toLowerCase();
        String gameSpawnName = (args.length == 3) ? args[2] : null;

        try {
            if (!dataManager.arenaExists(arenaName)) {
                sender.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found.");
                return true;
            }

            String fullSpawnPath;
            switch (spawnType) {
                case "lobby":
                case "exit":
                case "spectator":
                    if (gameSpawnName != null) {
                        sender.sendMessage(ChatColor.RED + "Error: Do not provide a spawnName for '" + spawnType + "' type.");
                        return true;
                    }
                    fullSpawnPath = spawnType + "-spawn";
                    break;
                case "game":
                    if (gameSpawnName == null || gameSpawnName.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "Usage: /delspawn <arenaName> game <spawnName>");
                        return true;
                    }
                    fullSpawnPath = "game-spawn-points." + gameSpawnName;
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Invalid spawn type. Use 'lobby', 'exit', 'spectator', or 'game'.");
                    return true;
            }

            if (dataManager.deleteSpawnLocation(arenaName, fullSpawnPath)) {
                sender.sendMessage(ChatColor.GREEN + "Spawn point '" + spawnType + (gameSpawnName != null ? " " + gameSpawnName : "") + "' deleted for arena '" + arenaName + "'.");
                // Reload the plugin to ensure MinigameManager reflects the change
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.YELLOW + "Minigame configurations reloaded to apply spawn point changes.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Spawn point '" + spawnType + (gameSpawnName != null ? " " + gameSpawnName : "") + "' not found for arena '" + arenaName + "'.");
            }

        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Error deleting spawn point: " + e.getMessage());
            plugin.getLogger().severe("IO error deleting spawn point for arena '" + arenaName + "': " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An unexpected error occurred: " + e.getMessage());
            plugin.getLogger().severe("Unexpected error deleting spawn point for arena '" + arenaName + "': " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}
