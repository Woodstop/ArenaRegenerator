package io.github.woodstop.arenaRegenerator.Commands;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    public ReloadCommand() {
        // No specific manager initialization needed here, as it calls plugin's reload method
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length > 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /arena reload");
            return true;
        }

        if (!sender.hasPermission("arenaregenerator.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin.");
            return true;
        }

        ArenaRegenerator.getInstance().reloadPlugin();
        sender.sendMessage(ChatColor.GREEN + "ArenaRegenerator plugin reloaded successfully!");
        return true;
    }
}
