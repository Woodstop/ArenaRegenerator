package io.github.woodstop.arenaRegenerator.TabCompletion;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length == 1) {
            File jsonFile = new File(ArenaRegenerator.getInstance().getDataFolder(), "arenas.json");
            if (!jsonFile.exists()) return Collections.emptyList();

            try (FileReader reader = new FileReader(jsonFile)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return root.keySet().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .sorted()
                        .collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        return Collections.emptyList();
    }
}
