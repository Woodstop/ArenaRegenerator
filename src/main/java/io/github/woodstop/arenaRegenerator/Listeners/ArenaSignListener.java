package io.github.woodstop.arenaRegenerator.Listeners;

import io.github.woodstop.arenaRegenerator.Managers.MinigameManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID; // Import UUID

public class ArenaSignListener implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final MinigameManager minigameManager;


    public ArenaSignListener(MinigameManager minigameManager) {
        this.minigameManager = minigameManager;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String line0 = event.line(0) != null
                ? PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim()
                : "";

        if (line0.equalsIgnoreCase("[RegenArena]")) { // Consistent with user's stated preference
            if (!player.hasPermission("arenaregenerator.sign.create.regen")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You don’t have permission to create a RegenArena sign.");
                return;
            }
            event.setLine(0, ChatColor.AQUA + "[RegenArena]");
            player.sendMessage(ChatColor.GREEN + "Regen Arena sign created!");
        } else if (line0.equalsIgnoreCase("[JoinArena]")) {
            if (!player.hasPermission("arenaregenerator.sign.create.join"))  {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You don’t have permission to create a JoinArena sign.");
                return;
            }
            event.setLine(0, ChatColor.GREEN + "[JoinArena]");
            player.sendMessage(ChatColor.GREEN + "Join Arena sign created!");
        } else if (line0.equalsIgnoreCase("[LeaveArena]")) {
            if (!player.hasPermission("arenaregenerator.sign.create.leave")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You don’t have permission to create a LeaveArena sign.");
                return;
            }
            event.setLine(0, ChatColor.RED + "[LeaveArena]");
            player.sendMessage(ChatColor.GREEN + "Leave Arena sign created!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Sign)) return;

        Sign sign = (Sign) clickedBlock.getState();
        Player player = event.getPlayer();

        String header = getSignLine(sign, 0);
        String arenaName = getSignLine(sign, 1);

        // Harmonized check for all recognized sign types
        if (!(header.equalsIgnoreCase("[RegenArena]") || header.equalsIgnoreCase("[JoinArena]") || header.equalsIgnoreCase("[LeaveArena]"))) {
            return;
        }

        event.setCancelled(true); // Prevent the edit sign screen from opening

        // Get cooldown from config via the MinigameManager and main plugin instance
        long cooldownSeconds = minigameManager.getPlugin().getSignUseCooldownSeconds();

        // Cooldown check (only for non-bypass players AND if cooldown is enabled)
        if (cooldownSeconds != -1 && !player.hasPermission("arenaregenerator.sign.bypass")) {
            long cooldownMillis = cooldownSeconds * 1000; // Convert to milliseconds
            long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L); // Use UUID for cooldowns
            long now = System.currentTimeMillis();
            if (now - lastUsed < cooldownMillis) {
                long remainingSeconds = (cooldownMillis - (now - lastUsed)) / 1000 + 1; // +1 to round up
                player.sendMessage(ChatColor.YELLOW + "Please wait " + remainingSeconds + " seconds before using this sign again.");
                return; // Exit if cooldown is active
            }
            // If cooldown is NOT active, then update the last used time for THIS click
            cooldowns.put(player.getUniqueId(), now); // Use UUID for cooldowns
        }

        // --- Execute action based on sign type and specific permissions ---
        // The general 'arenaregenerator.sign.use' check has been removed.
        // Each sign type now relies solely on its specific 'use' permission.
        if (header.equalsIgnoreCase("[RegenArena]")) { // Consistent with onSignChange
            if (!player.hasPermission("arenaregenerator.sign.use.regen")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use RegenArena signs.");
                return;
            }
            if (arenaName.isEmpty()) {
                player.sendMessage(ChatColor.RED + "This RegenArena sign is not configured with an arena name!");
                return;
            }
            try {
                // Ensure MinigameManager and its ArenaDataManager are not null before use
                if (minigameManager == null || minigameManager.getArenaDataManager() == null) {
                    player.sendMessage(ChatColor.RED + "Plugin internal error: MinigameManager or ArenaDataManager not initialized.");
                    minigameManager.getPlugin().getLogger().severe("ERROR: MinigameManager or ArenaDataManager is null when processing RegenArena sign for " + player.getName());
                    return;
                }
                if (!minigameManager.getArenaDataManager().arenaExists(arenaName)) {
                    player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' not found.");
                    return;
                }
            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "Error reading arena data. See console for details.");
                minigameManager.getPlugin().getLogger().severe("Error checking arena existence for sign: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            // Dispatch the command using the unified /arena structure
            Bukkit.dispatchCommand(minigameManager.getPlugin().getServer().getConsoleSender(), "arena regen " + arenaName);
            player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' is being regenerated!");

        } else if (header.equalsIgnoreCase("[JoinArena]")) {
            if (!player.hasPermission("arenaregenerator.sign.use.join")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use JoinArena signs.");
                return;
            }
            if (arenaName.isEmpty()) {
                player.sendMessage(ChatColor.RED + "This JoinArena sign is not configured with an arena name on the second line.");
                return;
            }
            if (minigameManager == null) { // Check if minigameManager is null before using it
                player.sendMessage(ChatColor.RED + "Plugin internal error: MinigameManager not initialized.");
                Bukkit.getLogger().severe("ERROR: MinigameManager is null when processing JoinArena sign for " + player.getName());
                return;
            }
            minigameManager.joinMinigame(player, arenaName);

        } else if (header.equalsIgnoreCase("[LeaveArena]")) {
            if (!player.hasPermission("arenaregenerator.sign.use.leave")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use LeaveArena signs.");
                return;
            }
            if (minigameManager == null) { // Check if minigameManager is null before using it
                player.sendMessage(ChatColor.RED + "Plugin internal error: MinigameManager not initialized.");
                Bukkit.getLogger().severe("ERROR: MinigameManager is null when processing LeaveArena sign for " + player.getName());
                return;
            }
            minigameManager.leaveMinigame(player, true);

        } else {
            player.sendMessage(ChatColor.RED + "Unknown sign action type: " + header);
        }
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Sign sign)) return;

        String line0Text = getSignLine(sign, 0);

        if (line0Text.equalsIgnoreCase("[RegenArena]") || // Consistent with onSignChange
                line0Text.equalsIgnoreCase("[JoinArena]") ||
                line0Text.equalsIgnoreCase("[LeaveArena]")) {

            if (!event.getPlayer().hasPermission("arenaregenerator.sign.break")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to break this sign.");
            } else {
                event.getPlayer().sendMessage(ChatColor.GREEN + "ArenaRegenerator sign broken.");
            }
        }
    }

    /**
     * Helper method to get a sign line, stripping color codes and trimming.
     *
     * @param sign      The Sign object.
     * @param lineIndex The index of the line (0-3).
     * @return The plain text content of the sign line.
     */
    private String getSignLine(Sign sign, int lineIndex) {
        SignSide side = sign.getSide(Side.FRONT);
        if (lineIndex >= 0 && lineIndex < 4 && side.getLine(lineIndex) != null) {
            return PlainTextComponentSerializer.plainText().serialize(side.line(lineIndex)).trim();
        }
        return "";
    }
}
