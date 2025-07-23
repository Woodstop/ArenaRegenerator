package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.JsonObject;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import java.io.IOException;

// Command class triggered by /regenarena
public class RegenerateArenaCommand implements CommandExecutor {

    private final ArenaDataManager dataManager;

    public RegenerateArenaCommand() {
        this.dataManager = new ArenaDataManager(); // Initialize the data manager
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        if (args.length != 1) {
            commandSender.sendMessage("§cUsage: /regenarena <arenaName>");
            return true;
        }
        
        String arenaName = args[0]; // Define arenaName from command argument

        try {
            // Use data manager to get arena data
            JsonObject data = dataManager.getArenaData(arenaName);
            if (data == null) {
                commandSender.sendMessage("§cArena '" + arenaName + "' not found in data file.");
                return true;
            }

            int x = data.get("x").getAsInt();
            int y = data.get("y").getAsInt();
            int z = data.get("z").getAsInt();
            String worldName = data.get("world").getAsString();

            org.bukkit.World bukkitWorld = org.bukkit.Bukkit.getWorld(worldName);
            if (bukkitWorld == null) {
                commandSender.sendMessage("§cWorld '" + worldName + "' is not loaded.");
                return true;
            }

            // Use data manager to load the schematic
            Clipboard clipboard = dataManager.loadArenaSchematic(arenaName);
            if (clipboard == null) {
                // Error message already logged by dataManager or specific message here
                commandSender.sendMessage("§cCould not load schematic for arena '" + arenaName + "'. It might be missing or corrupted.");
                return true;
            }

            commandSender.sendMessage("Regenerating arena...");

            World world = BukkitAdapter.adapt(bukkitWorld);
            BlockVector3 pasteLocation = BlockVector3.at(x, y, z);

            //  Paste it with FAWE
            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(world)
                    .build()) {

                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(pasteLocation)
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);
            }

            commandSender.sendMessage("§aArena '" + arenaName + "' regenerated!");

        } catch (IOException e) {
            commandSender.sendMessage("§cError accessing arena data: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch other WorldEdit related exceptions
            commandSender.sendMessage("§cError regenerating arena: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
