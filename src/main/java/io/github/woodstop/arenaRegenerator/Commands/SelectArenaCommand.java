package io.github.woodstop.arenaRegenerator.Commands;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.SessionManager;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class SelectArenaCommand implements CommandExecutor {

    private final ArenaDataManager dataManager; // Instance of the data manager

    public SelectArenaCommand() {
        this.dataManager = new ArenaDataManager(); // Initialize the data manager
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /arena select <arenaName>");
            return true;
        }

        String arenaName = args[0];

        try {
            // Use data manager to get the arena region directly
            Region newSelection = dataManager.getArenaRegion(arenaName);

            if (newSelection == null) {
                // Specific error messages from dataManager's internal checks are not directly propagated here.
                // We'll provide a generic message, or dataManager could throw specific exceptions.
                if (!dataManager.arenaExists(arenaName)) {
                    player.sendMessage("§cArena '" + arenaName + "' not found.");
                } else {
                    player.sendMessage("§cCould not select arena '" + arenaName + "'. Check server logs for details (world not loaded or schematic corrupted).");
                }
                return true;
            }

            // Get the player's WorldEdit session and set the selection
            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = manager.get(wePlayer);

            session.setRegionSelector(wePlayer.getWorld(), new CuboidRegionSelector(wePlayer.getWorld(),
                    newSelection.getMinimumPoint(),
                    newSelection.getMaximumPoint()));

            session.getRegionSelector(wePlayer.getWorld())
                    .learnChanges(); // Tell WE the selection changed
            session.dispatchCUISelection(wePlayer); // For client-side WorldEdit CUI if installed

            player.sendMessage("§aArena '" + arenaName + "' selected in WorldEdit!");

        } catch (IOException e) {
            player.sendMessage("§cError accessing arena data: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch other WorldEdit related exceptions
            player.sendMessage("§cFailed to select arena: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}