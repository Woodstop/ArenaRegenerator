package io.github.woodstop.arenaRegenerator.Commands;

import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import io.github.woodstop.arenaRegenerator.Minigame.MinigameArena;
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
    private final SetSpawnCommand setSpawnCommand = new SetSpawnCommand();
    private final DelSpawnCommand delSpawnCommand = new DelSpawnCommand();

    // ArenaDataManager for tab completion
    private final ArenaDataManager arenaDataManager = new ArenaDataManager();

    // MinigameManager for minigame operations and tab completion (minigame arenas)
    private MinigameManager minigameManager;

    public ArenaCommand() {
        // MinigameManager needs to be initialized after plugin enables and managers are set up
        // We'll get it from the plugin instance when needed.
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        // Initialize minigameManager here to ensure ArenaRegenerator is fully loaded
        this.minigameManager = ArenaRegenerator.getInstance().getMinigameManager();

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
                return arenaListCommand.onCommand(sender, command, label, subArgs);
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
            case "setspawn":
                return setSpawnCommand.onCommand(sender, command, label, subArgs);
            case "delspawn":
                return delSpawnCommand.onCommand(sender, command, label, subArgs);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        this.minigameManager = ArenaRegenerator.getInstance().getMinigameManager();

        List<String> completions = new ArrayList<>();
        String partialArg = args[args.length - 1].toLowerCase();

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
            if (sender.hasPermission("arenaregenerator.setspawn")) completions.add("setspawn");
            if (sender.hasPermission("arenaregenerator.delspawn")) completions.add("delspawn");

            return completions.stream()
                    .filter(s -> s.startsWith(partialArg))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "save":
                case "regen":
                case "clear":
                case "delete":
                case "info":
                case "select":
                    // Suggest existing saved arena names from ArenaDataManager
                    return getSavedArenaNameCompletions(partialArg);
                case "delspawn":
                case "setspawn":
                    // suggest spawn types (lobby, exit, spectator, game)
                    return getSpawnTypeCompletions(partialArg);
                case "join":
                    // Suggest configured minigame arena names from MinigameManager
                    return getMinigameArenaNameCompletions(partialArg);
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String arenaName = args[1].toLowerCase(); // The arena name already typed

            switch (subCommand) {
                case "setspawn":
                case "delspawn":
                    return getSavedArenaNameCompletions(partialArg);
            }
        } else if (args.length == 4) { // For /arena setspawn game <arenaName> <spawnName>
            String subCommand = args[0].toLowerCase();
            String spawnType = args[1].toLowerCase();
            String arenaName = args[2].toLowerCase();

            if (subCommand.equals("setspawn") && spawnType.equals("game")) {
                return Collections.singletonList("[spawnName]");
            } else if (subCommand.equals("delspawn") && spawnType.equals("game")) {
                return getGameSpawnPointNameCompletions(arenaName, partialArg);
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
        if (sender.hasPermission("arenaregenerator.setspawn")) sender.sendMessage(ChatColor.YELLOW + "/arena setspawn <lobby|exit|spectator|game> <arenaName> " + ChatColor.GRAY + "- Sets a specific spawn point.");
        if (sender.hasPermission("arenaregenerator.delspawn")) sender.sendMessage(ChatColor.YELLOW + "/arena delspawn <lobby|exit|spectator|game> <arenaName> [spawnName] " + ChatColor.GRAY + "- Deletes a named game spawn point.");
        sender.sendMessage(ChatColor.GOLD + "---------------------------------");
    }

    /**
     * Helper to get completions for saved arena names (from ArenaDataManager).
     * @param partialName The partial name typed by the user.
     * @return A list of matching arena names.
     */
    private List<String> getSavedArenaNameCompletions(String partialName) {
        try {
            return arenaDataManager.loadArenasJson().keySet().stream()
                    .filter(s -> s.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            ArenaRegenerator.getInstance().getLogger().warning("Error loading saved arena names for tab completion: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Helper to get completions for configured minigame arena names (from MinigameManager).
     * @param partialName The partial name typed by the user.
     * @return A list of matching minigame arena names.
     */
    private List<String> getMinigameArenaNameCompletions(String partialName) {
        return minigameManager.getConfiguredMinigameNames().stream()
                .filter(s -> s.toLowerCase().startsWith(partialName))
                .collect(Collectors.toList());
    }

    /**
     * Helper to get completions for spawn types (lobby, exit, spectator).
     * @param partialName The partial name typed by the user.
     * @return A list of matching spawn types.
     */
    private List<String> getSpawnTypeCompletions(String partialName) {
        return Arrays.asList("lobby", "exit", "spectator", "game").stream()
                .filter(s -> s.startsWith(partialName))
                .collect(Collectors.toList());
    }

    /**
     * Helper to get completions for game spawn point names within a specific arena.
     * @param arenaNameInput The name of the arena.
     * @param partialName The partial name typed by the user.
     * @return A list of matching game spawn point names.
     */
    private List<String> getGameSpawnPointNameCompletions(String arenaNameInput, String partialName) {
        try {
            // Get all arena names from arenas.json (case-insensitively)
            String actualArenaName = arenaDataManager.loadArenasJson().keySet().stream()
                    .filter(s -> s.equalsIgnoreCase(arenaNameInput))
                    .findFirst()
                    .orElse(null);

            if (actualArenaName != null) {
                // Load game spawn points for the actual arena name from arenas.json
                return arenaDataManager.loadGameSpawnPoints(actualArenaName).keySet().stream()
                        .filter(s -> s.toLowerCase().startsWith(partialName))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            ArenaRegenerator.getInstance().getLogger().warning("Error loading game spawn names for tab completion: " + e.getMessage());
        }
        return Collections.emptyList();
    }
}