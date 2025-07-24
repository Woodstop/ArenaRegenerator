package io.github.woodstop.arenaRegenerator.Commands;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LeaveArenaCommand implements CommandExecutor {

    private MinigameManager minigameManager; // Will be initialized via plugin instance

    public LeaveArenaCommand() {
        // MinigameManager needs to be initialized after plugin enables and managers are set up
        // We'll get it from the plugin instance when needed.
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        // No arguments expected for leave command
        if (args.length > 0) {
            player.sendMessage(ChatColor.RED + "Usage: /arena leave");
            return true;
        }

        // Initialize minigameManager here to ensure ArenaRegenerator is fully loaded
        if (minigameManager == null) {
            minigameManager = ArenaRegenerator.getInstance().getMinigameManager();
        }

        if (minigameManager.isPlayerInMinigame(player)) {
            minigameManager.leaveMinigame(player, player.getName(), true);
        } else {
            player.sendMessage(ChatColor.RED + "You are not currently in a minigame.");
        }
        return true;
    }
}
