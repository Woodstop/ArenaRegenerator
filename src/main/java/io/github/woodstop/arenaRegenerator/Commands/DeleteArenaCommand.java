package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.JsonObject;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class DeleteArenaCommand implements CommandExecutor {

    private final ArenaDataManager dataManager;

    public DeleteArenaCommand() {
        this.dataManager = new ArenaDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /arena delete <name>");
            return true;
        }

        String arenaName = args[0];

        try {
            JsonObject root = dataManager.loadArenasJson();
            if (!root.has(arenaName)) {
                sender.sendMessage("§cArena '" + arenaName + "' not found.");
                return true;
            }

            // Remove from JSON
            root.remove(arenaName);
            dataManager.saveArenasJson(root);

            // Delete schematic file
            File schematicFile = dataManager.getSchematicFile(arenaName);
            if (schematicFile.exists()) {
                if (schematicFile.delete()) {
                    sender.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' and its schematic deleted.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Arena '" + arenaName + "' data deleted, but schematic file could not be deleted.");
                }
            } else {
                sender.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' data deleted. Schematic file was already missing.");
            }
        }
        catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "Error deleting arena: " + e.getMessage());
                e.printStackTrace();
            }
        return true;
    }
}
