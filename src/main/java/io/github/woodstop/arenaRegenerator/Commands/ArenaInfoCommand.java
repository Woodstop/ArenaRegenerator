package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.JsonObject;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

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

        } catch (IOException e) {
            sender.sendMessage("§cError reading arena info.");
            e.printStackTrace();
        }

        return true;
    }
}
