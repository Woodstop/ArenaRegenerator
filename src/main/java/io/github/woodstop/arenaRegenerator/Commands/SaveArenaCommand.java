package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;

import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;

import com.sk89q.worldedit.WorldEdit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collections;
import java.util.List;

public class SaveArenaCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /savearena <arenaName>");
            return true;
        }

        String arenaName = args[0];

        try {
            // Adapt Bukkit player to WorldEdit player
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);

            SessionManager manager = WorldEdit.getInstance().getSessionManager();

            // Get their WorldEdit session
            LocalSession session = manager.get(wePlayer);

            // Get the selection region
            Region region = session.getSelection(wePlayer.getWorld());

            // Create a clipboard from the region
            World world = wePlayer.getWorld();

            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

            // Copy the region
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    world, region, clipboard, region.getMinimumPoint()
            );

            // Prevent FAWE from copying entities to suppress the warning message
            forwardExtentCopy.setCopyingEntities(false);
            Operations.complete(forwardExtentCopy);

            // Save the clipboard to a file
            File dataFolder = ArenaRegenerator.getInstance().getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();

            File outFile = new File(dataFolder, arenaName + ".schem");
            ClipboardFormat format = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC;

            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(outFile))) {
                writer.write(clipboard);
            }

            player.sendMessage("§aSaved selection as " + arenaName + ".schem");

            // Get the selection origin
            BlockVector3 origin = clipboard.getOrigin();

            // Save origin position in a file (arena1.json or .yml)
            File jsonFile = new File(dataFolder, "arenas.json");

            // Load or create root object
            JsonObject root = new JsonObject();
            if (jsonFile.exists()) {
                root = JsonParser.parseReader(new FileReader(jsonFile)).getAsJsonObject();
            }

            // Create JSON entry for the arena
            JsonObject arenaData = new JsonObject();
            arenaData.addProperty("x", origin.x());
            arenaData.addProperty("y", origin.y());
            arenaData.addProperty("z", origin.z());
            arenaData.addProperty("world", world.getName());

            root.add(arenaName, arenaData);

            // Save back to file
            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(root));
            }

        } catch (Exception e) {
            player.sendMessage("§cFailed to save selection: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // Check if the user is trying to autocomplete the first argument
        if (args.length == 1) {
            // Return a list containing one element: "<arenaName>"
            // This will show up as a suggestion when the player presses tab
            return Collections.singletonList("<arenaName>");
        }

        // For all other argument positions, return an empty list = no suggestions
        return Collections.emptyList();
    }
}
