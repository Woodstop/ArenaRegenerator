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

public class ArenaListCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        // Open the JSON file
        File jsonFile = new File(ArenaRegenerator.getInstance().getDataFolder(), "arenas.json");

        // If the JSON file does not exist, print message
        if (!jsonFile.exists()) {
            sender.sendMessage("§cNo arenas have been saved yet.");
            return true;
        }
            JsonObject root;

            // If the JSON file exists but is empty, print message
            try (FileReader reader = new FileReader(jsonFile)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                sender.sendMessage("§cFailed to read arenas file.");
                e.printStackTrace();
                return true;
            }

            if (root.size() == 0) {
                sender.sendMessage("§cNo arenas have been saved yet.");
                return true;
            }

            sender.sendMessage("§aSaved Arenas:");
            for (String key : root.keySet()) {
                sender.sendMessage("§7- " + key);
        }
        return true;
    }

}