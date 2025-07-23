package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.JsonObject;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ArenaListCommand implements CommandExecutor {

    private final ArenaDataManager dataManager;

    public ArenaListCommand() {
        this.dataManager = new ArenaDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        // No arguments expected for list command
        if (args.length > 0) {
            sender.sendMessage("§cUsage: /arena list");
            return true;
        }

        try {
            JsonObject root = dataManager.loadArenasJson();
            if (root.size() == 0) {
                sender.sendMessage("§cNo arenas have been saved yet.");
                return true;
            }
            sender.sendMessage("§aSaved Arenas:");
            for (String key : root.keySet()) {
                sender.sendMessage("§7- " + key);
            }
        } catch (IOException e) {
            sender.sendMessage("§cError loading arena list: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}