package io.github.woodstop.arenaRegenerator.Commands;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final SaveArenaCommand saveArenaCommand = new SaveArenaCommand();
    private final RegenerateArenaCommand regenerateArenaCommand = new RegenerateArenaCommand();
    private final ClearArenaCommand clearArenaCommand = new ClearArenaCommand();
    private final ArenaListCommand arenaListCommand = new ArenaListCommand();
    private final DeleteArenaCommand deleteArenaCommand = new DeleteArenaCommand();
    private final ArenaInfoCommand arenaInfoCommand = new ArenaInfoCommand();
    private final SelectArenaCommand selectArenaCommand = new SelectArenaCommand();
    private final LeaveArenaCommand leaveArenaCommand = new LeaveArenaCommand();
    private final JoinArenaCommand joinArenaCommand = new JoinArenaCommand();
    private final ReloadCommand reloadCommand = new ReloadCommand();

    // ArenaDataManager for tab completion
    private final ArenaDataManager dataManager = new ArenaDataManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length); // Arguments for the sub-command

        switch (subCommand) {
            case "save":
                return saveArenaCommand.onCommand(sender, command, label, subArgs);
            case "regen":
                return regenerateArenaCommand.onCommand(sender, command, label, subArgs);
            case "clear":
                return clearArenaCommand.onCommand(sender, command, label, subArgs);
            case "list":
                return arenaListCommand.onCommand(sender, command, label, subArgs); // List command might not use subArgs, but pass anyway
            case "delete":
                return deleteArenaCommand.onCommand(sender, command, label, subArgs);
            case "info":
                return arenaInfoCommand.onCommand(sender, command, label, subArgs);
            case "select":
                return selectArenaCommand.onCommand(sender, command, label, subArgs);
            case "join":
                return joinArenaCommand.onCommand(sender, command, label, subArgs);
            case "leave":
                return leaveArenaCommand.onCommand(sender, command, label, subArgs);
            case "reload":
                return reloadCommand.onCommand(sender, command, label, subArgs);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            // Top-level sub-commands
            if (sender.hasPermission("arenaregenerator.save")) completions.add("save");
            if (sender.hasPermission("arenaregenerator.regen")) completions.add("regen");
            if (sender.hasPermission("arenaregenerator.clear")) completions.add("clear");
            if (sender.hasPermission("arenaregenerator.list")) completions.add("list");
            if (sender.hasPermission("arenaregenerator.delete")) completions.add("delete");
            if (sender.hasPermission("arenaregenerator.info")) completions.add("info");
            if (sender.hasPermission("arenaregenerator.select")) completions.add("select");
            if (sender.hasPermission("arenaregenerator.leave")) completions.add("leave");
            if (sender.hasPermission("arenaregenerator.join")) completions.add("join");
            if (sender.hasPermission("arenaregenerator.reload")) completions.add("reload");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("regen") || subCommand.equals("clear") || subCommand.equals("delete") || subCommand.equals("info") || subCommand.equals("select") || subCommand.equals("join")) {
                // For commands requiring an arena name, suggest existing arena names
                try {
                    return dataManager.loadArenasJson().keySet().stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    ArenaRegenerator.getInstance().getLogger().warning("Error loading arena names for tab completion: " + e.getMessage());
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- ArenaRegenerator Commands ---");
        if (sender.hasPermission("arenaregenerator.save")) sender.sendMessage(ChatColor.YELLOW + "/arena save <arenaName> " + ChatColor.GRAY + "- Saves your current WorldEdit selection as an arena.");
        if (sender.hasPermission("arenaregenerator.regen")) sender.sendMessage(ChatColor.YELLOW + "/arena regen <arenaName> " + ChatColor.GRAY + "- Regenerates a saved arena.");
        if (sender.hasPermission("arenaregenerator.clear")) sender.sendMessage(ChatColor.YELLOW + "/arena clear <arenaName> " + ChatColor.GRAY + "- Clears blocks in a saved arena without deleting the schematic.");
        if (sender.hasPermission("arenaregenerator.list")) sender.sendMessage(ChatColor.YELLOW + "/arena list " + ChatColor.GRAY + "- Lists all saved arenas.");
        if (sender.hasPermission("arenaregenerator.delete")) sender.sendMessage(ChatColor.YELLOW + "/arena delete <arenaName> " + ChatColor.GRAY + "- Deletes a saved arena.");
        if (sender.hasPermission("arenaregenerator.info")) sender.sendMessage(ChatColor.YELLOW + "/arena info <arenaName> " + ChatColor.GRAY + "- Shows info about a saved arena.");
        if (sender.hasPermission("arenaregenerator.select")) sender.sendMessage(ChatColor.YELLOW + "/arena select <arenaName> " + ChatColor.GRAY + "- Selects a saved arena in WorldEdit.");
        if (sender.hasPermission("arenaregenerator.join")) sender.sendMessage(ChatColor.YELLOW + "/arena join <arenaName> " + ChatColor.GRAY + "- Joins a minigame arena.");
        if (sender.hasPermission("arenaregenerator.leave")) sender.sendMessage(ChatColor.YELLOW + "/arena leave " + ChatColor.GRAY + "- Leaves the current minigame arena.");
        if (sender.hasPermission("arenaregenerator.reload")) sender.sendMessage(ChatColor.YELLOW + "/arena reload " + ChatColor.GRAY + "- Reloads the plugin configuration.");
        sender.sendMessage(ChatColor.GOLD + "---------------------------------");
    }
}