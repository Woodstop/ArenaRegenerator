package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ArenaInfoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /arenainfo <arenaName>");
            return true;
        }

        String arenaName = args[0];
        File dataFolder = ArenaRegenerator.getInstance().getDataFolder();
        File jsonFile = new File(dataFolder, "arenas.json");

        if (!jsonFile.exists()) {
            sender.sendMessage("§cNo arenas saved yet.");
            return true;
        }

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has(arenaName)) {
                sender.sendMessage("§cArena '" + arenaName + "' not found.");
                return true;
            }

            JsonObject data = root.getAsJsonObject(arenaName);
            int x = data.get("x").getAsInt();
            int y = data.get("y").getAsInt();
            int z = data.get("z").getAsInt();
            String world = data.get("world").getAsString();

            File schematicFile = new File(dataFolder, arenaName + ".schem");
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
