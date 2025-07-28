package io.github.woodstop.arenaRegenerator.Commands;

import com.google.gson.JsonObject;
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

import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;

public class SaveArenaCommand implements CommandExecutor {

    private final ArenaDataManager dataManager;

    public SaveArenaCommand() {
        this.dataManager = new ArenaDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /arena save <arenaName>");
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
            File outFile = dataManager.getSchematicFile(arenaName);
            ClipboardFormat format = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC;

            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(outFile))) {
                writer.write(clipboard);
            }

            player.sendMessage("§aSaved selection as " + arenaName + ".schem");

            // Get the selection origin
            BlockVector3 origin = clipboard.getOrigin();

            JsonObject root = dataManager.loadArenasJson();

            // Create JSON entry for the arena
            JsonObject arenaData = new JsonObject();
            arenaData.addProperty("x", origin.x());
            arenaData.addProperty("y", origin.y());
            arenaData.addProperty("z", origin.z());
            arenaData.addProperty("world", world.getName());

            root.add(arenaName, arenaData);

            // Save back to file
            dataManager.saveArenasJson(root);
            player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' saved successfully!");

        } catch (Exception e) {
            player.sendMessage("§cFailed to save selection: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
