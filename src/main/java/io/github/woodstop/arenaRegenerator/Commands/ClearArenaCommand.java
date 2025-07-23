package io.github.woodstop.arenaRegenerator.Commands;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ClearArenaCommand implements CommandExecutor {

    private final ArenaDataManager dataManager;

    public ClearArenaCommand() {
        this.dataManager = new ArenaDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /cleararena <arenaName>");
            return true;
        }

        String arenaName = args[0];

        try {
            // Use data manager to get the arena region directly
            Region regionToClear = dataManager.getArenaRegion(arenaName);

            if (regionToClear == null) {
                // Specific error messages from dataManager's internal checks are not directly propagated here.
                // We'll provide a generic message, or dataManager could throw specific exceptions.
                if (!dataManager.arenaExists(arenaName)) {
                    sender.sendMessage("§cArena '" + arenaName + "' not found.");
                } else {
                    sender.sendMessage("§cCould not determine region for arena '" + arenaName + "'. Check server logs for details (world not loaded or schematic corrupted).");
                }
                return true;
            }

            // Perform the clearing operation using the obtained region
            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder().world(regionToClear.getWorld()).build()) {
                editSession.setBlocks(regionToClear, BlockTypes.AIR.getDefaultState());
            }

            sender.sendMessage("§aArena '" + arenaName + "' cleared.");
            return true;

        } catch (IOException e) {
            sender.sendMessage("§cError accessing arena data: " + e.getMessage());
            e.printStackTrace();
        } catch (WorldEditException e) {
            sender.sendMessage("§cError clearing arena: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch any other unexpected exceptions
            sender.sendMessage("§cAn unexpected error occurred while clearing arena: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}