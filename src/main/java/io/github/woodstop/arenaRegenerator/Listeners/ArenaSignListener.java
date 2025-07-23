package io.github.woodstop.arenaRegenerator.Listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ArenaSignListener implements Listener {

    private final Map<String, Long> lastClickTime = new HashMap<>();
    private final long cooldownMillis = 10 * 1000; // 10 seconds

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) return;

        // Assuming `sign` is already confirmed to be a Sign instance
        Component line0 = sign.getSide(Side.FRONT).line(0);
        Component line1 = sign.getSide(Side.FRONT).line(1);

        // Get the first line and second line of the sign
        String header = PlainTextComponentSerializer.plainText().serialize(line0).trim();
        String arenaName = PlainTextComponentSerializer.plainText().serialize(line1).trim();

        // If the first line of the sign is not [ResetArena], stop
        if (!header.equalsIgnoreCase("[ResetArena]")) return;

        // Prevent the edit sign screen from opening
        event.setCancelled(true);

        Player player = event.getPlayer();

        // If the player does not have permission, stop
        if (!player.hasPermission("arenaregenerator.sign.use")) {
            player.sendMessage("§cYou don’t have permission to use this sign.");
            return;
        }

        // If the player has bypass perms, skip the cooldown
        if (!player.hasPermission("arenaregenerator.sign.bypass")) {
            // Cooldown check
            long now = System.currentTimeMillis();
            if (lastClickTime.containsKey(player.getName())) {
                long last = lastClickTime.get(player.getName());
                if ((now - last) < cooldownMillis) {
                    player.sendMessage("§cYou must wait before using this again.");
                    return;
                }
            }
            lastClickTime.put(player.getName(), now);
        }

        // Validate arena
        File jsonFile = new File(ArenaRegenerator.getInstance().getDataFolder(), "arenas.json");
        if (!jsonFile.exists()) {
            player.sendMessage("§cArena data file missing.");
            return;
        }

        // If the arena on the sign is not found in arenas.json, stop
        try (FileReader reader = new FileReader(jsonFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has(arenaName)) {
                player.sendMessage("§cArena not found: " + arenaName);
                return;
            }
        } catch (IOException e) {
            player.sendMessage("§cError reading arenas.json.");
            e.printStackTrace();
            return;
        }

        // Trigger your existing regen logic here:
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "regenarena " + arenaName);
        player.sendMessage("§aRegenerating arena: " + arenaName);
    }

    // Listener to change [ResetArena] to blue when typed and prevent players without permission from creating a sign
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String line0 = event.line(0) != null
                ? PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim()
                : "";


        if (line0.equalsIgnoreCase("[ResetArena]")) {
            if (!event.getPlayer().hasPermission("arenaregenerator.sign.create")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cYou don’t have permission to create a ResetArena sign.");
                return;
            }
            Component text = Component.text("[ResetArena]", TextColor.color(85,255,255));
            event.line(0,text);
        }
    }

    // Listener to prevent players from breaking the sign unless they have permission
    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Sign sign)) return;

        Component line0 = sign.getSide(Side.FRONT).line(0);
        String raw = PlainTextComponentSerializer.plainText().serialize(line0).trim();

        if (raw.equalsIgnoreCase("[ResetArena]") && !event.getPlayer().hasPermission("arenaregenerator.sign.break")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou don't have permission to break this sign.");
        }
    }
}
