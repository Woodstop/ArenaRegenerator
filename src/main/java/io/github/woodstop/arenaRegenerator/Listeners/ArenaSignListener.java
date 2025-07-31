package io.github.woodstop.arenaRegenerator.Listeners;

import io.github.woodstop.arenaRegenerator.util.ArenaDataManager;
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

public class ArenaSignListener implements Listener {

    private final Map<String, Long> lastClickTime = new HashMap<>();
    private final long cooldownMillis = 10 * 1000; // 10 seconds
    private final ArenaDataManager dataManager;

    public ArenaSignListener() {
        this.dataManager = new ArenaDataManager();
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) return;

        // Get the first line and second line of the sign
        String header = getSignLine(sign, 0);
        String arenaName = getSignLine(sign, 1);

        // If the first line of the sign is not [RegenArena]. [JoinArena], or [LeaveArena], stop
        if (header.equalsIgnoreCase("[RegenArena]") || header.equalsIgnoreCase("[JoinArena]") || header.equalsIgnoreCase("[LeaveArena]")) {
            event.setCancelled(true); // prevent the edit screen from opening
        } else {
            return; // Not a recognized arena sign, so do nothing.
        }

        Player player = event.getPlayer();

        // If the player has bypass perms, skip the cooldown
        if (!player.hasPermission("arenaregenerator.sign.bypass")) {
            // Cooldown check
            long now = System.currentTimeMillis();
            if (lastClickTime.containsKey(player.getName())) {
                long last = lastClickTime.get(player.getName());
                if ((now - last) < cooldownMillis) {
                    long remainingSeconds = (cooldownMillis - (now - last)) / 1000;
                    player.sendMessage("§cYou must wait before using this again. Remaining: " + remainingSeconds + "s");
                    return;
                }
            }
            lastClickTime.put(player.getName(), now);
        }

        // Validate arena
        try {
            if (!dataManager.arenaExists(arenaName)) {
                player.sendMessage("§cArena not found: " + arenaName);
                return;
            }
        } catch (IOException e) {
            player.sendMessage("§cError reading arena data. See console for details.");
            e.printStackTrace();
            return;
        }

        // --- Handle different sign types ---
        if (header.equalsIgnoreCase("[RegenArena]")) {
            if (!player.hasPermission("arenaregenerator.sign.use.regen")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use RegenArena signs.");
                return;
            }
            // Validate arena existence for Regen signs
            try {
                if (!dataManager.arenaExists(arenaName)) {
                    player.sendMessage(ChatColor.RED + "Arena not found: " + arenaName);
                    return;
                }
            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "Error reading arena data. See console for details.");
                e.printStackTrace();
                return;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "arena regen " + arenaName);
            player.sendMessage(ChatColor.GREEN + "Regenerating arena: " + ChatColor.WHITE + arenaName);

        } else if (header.equalsIgnoreCase("[JoinArena]")) {
            if (!player.hasPermission("arenaregenerator.sign.use.join")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use JoinArena signs.");
                return;
            }
            // For Join signs, arenaName is required
            if (arenaName.isEmpty()) {
                player.sendMessage(ChatColor.RED + "This JoinArena sign is not configured with an arena name on the second line.");
                return;
            }
            // Dispatch the join command as the player to ensure proper permission checks by the command handler
            Bukkit.dispatchCommand(player, "arena join " + arenaName);

        } else if (header.equalsIgnoreCase("[LeaveArena]")) {
            if (!player.hasPermission("arenaregenerator.sign.use.leave")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use LeaveArena signs.");
                return;
            }
            // For Leave signs, no arenaName is needed on the second line, but we still read it.
            // Dispatch the leave command as the player
            Bukkit.dispatchCommand(player, "arena leave");
        }
    }

    // Listener to change [RegenArena] to blue when typed and prevent players without permission from creating a sign
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String line0 = event.line(0) != null
                ? PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim()
                : "";

        if (line0.equalsIgnoreCase("[RegenArena]")) {
            if (!event.getPlayer().hasPermission("arenaregenerator.sign.create.regen")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You don’t have permission to create a RegenArena sign.");
                return;
            }
            event.setLine(0, ChatColor.AQUA + "[RegenArena]");
        } else if (line0.equalsIgnoreCase("[JoinArena]")) {
            if (!event.getPlayer().hasPermission("arenaregenerator.sign.create.join"))  {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You don’t have permission to create a JoinArena sign.");
                return;
            }
            event.setLine(0, ChatColor.GREEN + "[JoinArena]");
        } else if (line0.equalsIgnoreCase("[LeaveArena]")) {
            if (!event.getPlayer().hasPermission("arenaregenerator.sign.create.leave")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You don’t have permission to create a LeaveArena sign.");
                return;
            }
            event.setLine(0, ChatColor.RED + "[LeaveArena]");
        }
    }

    // Listener to prevent players from breaking the sign unless they have permission
    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Sign sign)) return;

        String raw = getSignLine(sign, 0);

        if (raw.equalsIgnoreCase("[RegenArena]") || raw.equalsIgnoreCase("[JoinArena]") || raw.equalsIgnoreCase("[LeaveArena]")) {
            if (!event.getPlayer().hasPermission("arenaregenerator.sign.break")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to break this sign.");
            }
        }
    }
    /**
     * Helper method to get a sign line
     *
     * @param sign      The Sign or SignChangeEvent object.
     * @param lineIndex The index of the line (0-3).
     * @return The plain text content of the sign line.
     */
    private String getSignLine(Sign sign, int lineIndex) {
     SignSide side = sign.getSide(Side.FRONT);
        return ChatColor.stripColor(side.getLine(lineIndex)).trim();
    }
}