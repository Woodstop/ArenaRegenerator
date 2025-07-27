package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.JsonObject;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ArenaInfoCommand implements CommandExecutor {

    private final ArenaDataManager dataManager;

    public ArenaInfoCommand() {
        this.dataManager = new ArenaDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /arena info <arenaName>");
            return true;
        }

        String arenaName = args[0];

        try {
            JsonObject data = dataManager.getArenaData(arenaName);
            if (data == null) {
                sender.sendMessage("§cArena '" + arenaName + "' not found.");
                return true;
            }

            int x = data.get("x").getAsInt();
            int y = data.get("y").getAsInt();
            int z = data.get("z").getAsInt();
            String world = data.get("world").getAsString();

            File schematicFile = dataManager.getSchematicFile(arenaName);
            boolean fileExists = schematicFile.exists();

            sender.sendMessage("§aArena Info: §f" + arenaName);
            sender.sendMessage("§7World: §f" + world);
            sender.sendMessage("§7Origin: §f(" + x + ", " + y + ", " + z + ")");
            sender.sendMessage("§7Schematic File: §f" + schematicFile.getName() + (fileExists ? " §a✓" : " §c✗ Not found"));

            sender.sendMessage(ChatColor.GOLD + "------- Spawn Points -------");
            Location lobbySpawn = dataManager.loadSpawnLocation(arenaName, "lobby-spawn");
            if (lobbySpawn != null) sender.sendMessage(ChatColor.YELLOW + "Lobby: " + ChatColor.WHITE + formatLocation(lobbySpawn));
            else sender.sendMessage(ChatColor.YELLOW + "Lobby: " + ChatColor.GRAY + "Not set");

            Location exitSpawn = dataManager.loadSpawnLocation(arenaName, "exit-spawn");
            if (exitSpawn != null) sender.sendMessage(ChatColor.YELLOW + "Exit: " + ChatColor.WHITE + formatLocation(exitSpawn));
            else sender.sendMessage(ChatColor.YELLOW + "Exit: " + ChatColor.GRAY + "Not set");

            Location spectatorSpawn = dataManager.loadSpawnLocation(arenaName, "spectator-spawn");
            if (spectatorSpawn != null) sender.sendMessage(ChatColor.YELLOW + "Spectator: " + ChatColor.WHITE + formatLocation(spectatorSpawn));
            else sender.sendMessage(ChatColor.YELLOW + "Spectator: " + ChatColor.GRAY + "Not set");

            Map<String, Location> gameSpawns = dataManager.loadGameSpawnPoints(arenaName);
            if (!gameSpawns.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Game Spawns:");
                gameSpawns.forEach((name, loc) -> sender.sendMessage(ChatColor.WHITE + "  - " + name + ": " + formatLocation(loc)));
            } else {
                sender.sendMessage(ChatColor.YELLOW + "No game spawns set.");
            }
            sender.sendMessage(ChatColor.GOLD + "---------------------------");

        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Error accessing arena info: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    // Helper method to format Location for display
    private String formatLocation(Location loc) {
        return String.format("X: %.1f, Y: %.1f, Z: %.1f, Yaw: %.1f, Pitch: %.1f",
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }
}
