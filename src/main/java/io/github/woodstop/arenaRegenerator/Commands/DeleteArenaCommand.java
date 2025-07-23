package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DeleteArenaCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /delarena <name>");
            return true;
        }

        String arenaName = args[0];
        File dataFolder = ArenaRegenerator.getInstance().getDataFolder();
        File schematicFile = new File(dataFolder, arenaName + ".schem");
        File jsonFile = new File(dataFolder, "arenas.json");

        boolean deletedSomething = false;

        // Delete schematic file
        if (schematicFile.exists()) {
            if (schematicFile.delete()) {
                deletedSomething = true;
            }
        }

        // Remove entry from JSON
        if (jsonFile.exists()) {
            try {
                JsonObject root = JsonParser.parseReader(new FileReader(jsonFile)).getAsJsonObject();
                if (root.has(arenaName)) {
                    root.remove(arenaName);
                    try (FileWriter writer = new FileWriter(jsonFile)) {
                        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(root));
                    }
                    deletedSomething = true;
                }
            } catch (IOException e) {
                sender.sendMessage("§cError updating arenas.json");
                e.printStackTrace();
                return true;
            }
        }

        if (deletedSomething) {
            sender.sendMessage("§aArena '" + arenaName + "' deleted.");
        } else {
            sender.sendMessage("§cArena '" + arenaName + "' not found.");
        }

        return true;
    }
}
