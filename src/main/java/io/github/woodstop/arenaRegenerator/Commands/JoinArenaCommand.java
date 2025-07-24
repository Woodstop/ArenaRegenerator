package io.github.woodstop.arenaRegenerator.Commands;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JoinArenaCommand implements CommandExecutor {

    private MinigameManager minigameManager;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /arena join <arenaName>");
            return true;
        }

        // Initialize minigameManager here to ensure ArenaRegenerator is fully loaded
        if (minigameManager == null) {
            minigameManager = ArenaRegenerator.getInstance().getMinigameManager();
        }

        String arenaName = args[0];
        minigameManager.joinMinigame(player, arenaName);
        return true;
    }
}
