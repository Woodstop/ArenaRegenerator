package io.github.woodstop.arenaRegenerator.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages saving and loading of arena data (schematics and JSON metadata).
 */
public class ArenaDataManager {

    private final ArenaRegenerator plugin;
    private final File dataFolder;
    private final File arenasJsonFile;
    private final File schematicsFolder;
    private final Gson gson;

    public ArenaDataManager() {
        this.plugin = ArenaRegenerator.getInstance();
        this.dataFolder = plugin.getDataFolder();
        this.arenasJsonFile = new File(dataFolder, "arenas.json");
        this.schematicsFolder = new File(dataFolder, "schematics");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Ensure data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        // Ensure schematics folder exists
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
            plugin.getLogger().info("Created Schematics folder at: " + schematicsFolder.getAbsolutePath());
        }
        // Create arenas.json if it doesn't exist
        if (!arenasJsonFile.exists()) {
            try (FileWriter writer = new FileWriter(arenasJsonFile)) {
                writer.write(new JsonObject().toString()); // Write an empty JSON object
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create arenas.json: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the File object for an arena's schematic.
     * @param arenaName The name of the arena.
     * @return The File object.
     */
    public File getSchematicFile(String arenaName) {
        return new File(schematicsFolder, arenaName + ".schem");
    }

    /**
     * Checks if an arena's data (entry in JSON and schematic file) exists.
     * @param arenaName The name of the arena.
     * @return true if both JSON data and schematic file exist, false otherwise.
     * @throws IOException if there's an error reading the arenas.json file.
     */
    public boolean arenaExists(String arenaName) throws IOException {
        JsonObject root = loadArenasJson();
        return root.has(arenaName) && getSchematicFile(arenaName).exists();
    }

    /**
     * Loads the main arenas.json file into a JsonObject.
     * @return The JsonObject representing the arenas.json content.
     * @throws IOException if there's an error reading the file.
     */
    public JsonObject loadArenasJson() throws IOException {
        if (!arenasJsonFile.exists()) {
            // If file doesn't exist, create it with an empty object and return it
            try (FileWriter writer = new FileWriter(arenasJsonFile)) {
                JsonObject empty = new JsonObject();
                gson.toJson(empty, writer);
                return empty;
            }
        }
        try (FileReader reader = new FileReader(arenasJsonFile)) {
            JsonElement element = gson.fromJson(reader, JsonElement.class);
            if (element == null || !element.isJsonObject()) {
                // If file is empty or malformed, return an empty object and log a warning
                plugin.getLogger().warning("arenas.json is empty or malformed. Creating a new empty JSON object.");
                return new JsonObject();
            }
            return element.getAsJsonObject();
        }
    }

    /**
     * Saves a JsonObject to the arenas.json file.
     * @param jsonObject The JsonObject to save.
     * @throws IOException if there's an error writing to the file.
     */
    public void saveArenasJson(JsonObject jsonObject) throws IOException {
        try (FileWriter writer = new FileWriter(arenasJsonFile)) {
            gson.toJson(jsonObject, writer);
        }
    }

    /**
     * Retrieves the JSON data for a specific arena.
     * @param arenaName The name of the arena.
     * @return The JsonObject for the arena, or null if not found.
     * @throws IOException if there's an error reading the arenas.json file.
     */
    public JsonObject getArenaData(String arenaName) throws IOException {
        JsonObject root = loadArenasJson();
        if (root.has(arenaName) && root.get(arenaName).isJsonObject()) {
            return root.getAsJsonObject(arenaName);
        }
        return null;
    }

    /**
     * Loads a WorldEdit schematic for a given arena name.
     * @param arenaName The name of the arena.
     * @return The loaded Clipboard, or null if the schematic file is missing or corrupted.
     * @throws IOException if there's an error reading the schematic file.
     */
    public Clipboard loadArenaSchematic(String arenaName) throws IOException {
        File schematicFile = getSchematicFile(arenaName);
        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic file not found for arena: " + arenaName + " at " + schematicFile.getAbsolutePath());
            return null;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            plugin.getLogger().warning("Unknown schematic format for file: " + schematicFile.getName());
            return null;
        }

        try (ClipboardReader reader = format.getReader(new java.io.FileInputStream(schematicFile))) {
            return reader.read();
        } catch (IOException e) {
            plugin.getLogger().severe("Error loading schematic for arena '" + arenaName + "': " + e.getMessage());
            throw e; // Re-throw to be handled by calling command
        }
    }

    /**
     * Gets the WorldEdit Region object for a saved arena.
     * This method loads the schematic to determine its dimensions and applies the origin.
     * @param arenaName The name of the arena.
     * @return The WorldEdit Region, or null if data is incomplete or world is not loaded.
     * @throws IOException if there's an error reading the arenas.json or schematic file.
     */
    public Region getArenaRegion(String arenaName) throws IOException {
        JsonObject data = getArenaData(arenaName);
        if (data == null) {
            plugin.getLogger().warning("Arena data not found for region retrieval: " + arenaName);
            return null;
        }

        int x = data.get("x").getAsInt();
        int y = data.get("y").getAsInt();
        int z = data.get("z").getAsInt();
        String worldName = data.get("world").getAsString();

        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            plugin.getLogger().warning("World '" + worldName + "' not loaded for arena region: " + arenaName);
            return null;
        }
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);

        Clipboard clipboard = loadArenaSchematic(arenaName);
        if (clipboard == null) {
            plugin.getLogger().warning("Schematic not found or corrupted for arena region: " + arenaName);
            return null;
        }

        BlockVector3 minPoint = BlockVector3.at(x, y, z);
        BlockVector3 maxPoint = minPoint.add(clipboard.getDimensions().subtract(BlockVector3.ONE));

        return new CuboidRegion(weWorld, minPoint, maxPoint);
    }

    /**
     * Gets the WorldEdit Region object for the *playable area* of a minigame.
     * This region is defined by the min and max points saved with the schematic,
     * with an additional 3-block buffer on all sides to prevent players from easily
     * escaping the bounds.
     * @param arenaName The name of the arena.
     * @return The WorldEdit Region, or null if data is incomplete or world is not loaded.
     * @throws IOException if there's an error reading the arenas.json or schematic file.
     */
    public Region getMinigamePlayableRegion(String arenaName) throws IOException {
        Region baseRegion = getArenaRegion(arenaName);
        if (baseRegion == null) {
            return null;
        }

        // Apply a 3-block buffer on all sides
        int buffer = 3;
        BlockVector3 min = baseRegion.getMinimumPoint();
        BlockVector3 max = baseRegion.getMaximumPoint();

        BlockVector3 bufferedMin = BlockVector3.at(min.x() - buffer, min.y() - buffer, min.z() - buffer);
        BlockVector3 bufferedMax = BlockVector3.at(max.x() + buffer, max.y() + buffer, max.z() + buffer);

        return new CuboidRegion(baseRegion.getWorld(), bufferedMin, bufferedMax);
    }


    /**
     * Saves a spawn location for an arena in arenas.json.
     * The path can be "lobby-spawn", "exit-spawn", "spectator-spawn", or "game-spawn-points.<spawnName>".
     * @param arenaName The name of the arena.
     * @param path The JSON path to the spawn point (e.g., "lobby-spawn", "game-spawn-points.spawn1").
     * @param location The Location to save.
     * @throws IOException if there's an error writing to the file.
     */
    public void saveSpawnLocation(String arenaName, String path, Location location) throws IOException {
        JsonObject root = loadArenasJson();
        JsonObject arenaData = root.has(arenaName) ? root.getAsJsonObject(arenaName) : new JsonObject();

        // Navigate to the correct JSON path
        String[] pathParts = path.split("\\.");
        JsonObject currentObject = arenaData;
        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            if (!currentObject.has(part) || !currentObject.get(part).isJsonObject()) {
                currentObject.add(part, new JsonObject());
            }
            currentObject = currentObject.getAsJsonObject(part);
        }
        String finalKey = pathParts[pathParts.length - 1];

        JsonObject spawnLoc = new JsonObject();
        spawnLoc.addProperty("world", location.getWorld().getName());
        spawnLoc.addProperty("x", location.getX());
        spawnLoc.addProperty("y", location.getY());
        spawnLoc.addProperty("z", location.getZ());
        spawnLoc.addProperty("yaw", location.getYaw());
        spawnLoc.addProperty("pitch", location.getPitch());

        currentObject.add(finalKey, spawnLoc);
        root.add(arenaName, arenaData); // Add/replace arena data back to root
        saveArenasJson(root);
        plugin.getLogger().info("Saved spawn location for arena '" + arenaName + "' at path '" + path + "'.");
    }

    /**
     * Loads a spawn location from arenas.json.
     * @param arenaName The name of the arena.
     * @param path The JSON path to the spawn point (e.g., "lobby-spawn", "game-spawn-points.spawn1").
     * @return The loaded Location, or null if not found or invalid.
     * @throws IOException if there's an error reading the file.
     */
    public Location loadSpawnLocation(String arenaName, String path) throws IOException {
        JsonObject arenaData = getArenaData(arenaName);
        if (arenaData == null) {
            return null;
        }

        String[] pathParts = path.split("\\.");
        JsonObject currentObject = arenaData;
        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            if (!currentObject.has(part) || !currentObject.get(part).isJsonObject()) {
                return null; // Path does not exist
            }
            currentObject = currentObject.getAsJsonObject(part);
        }
        String finalKey = pathParts[pathParts.length - 1];

        if (currentObject.has(finalKey) && currentObject.get(finalKey).isJsonObject()) {
            JsonObject spawnLoc = currentObject.getAsJsonObject(finalKey);
            try {
                String worldName = spawnLoc.get("world").getAsString();
                double x = spawnLoc.get("x").getAsDouble();
                double y = spawnLoc.get("y").getAsDouble();
                double z = spawnLoc.get("z").getAsDouble();
                float yaw = spawnLoc.has("yaw") ? spawnLoc.get("yaw").getAsFloat() : 0.0f;
                float pitch = spawnLoc.has("pitch") ? spawnLoc.get("pitch").getAsFloat() : 0.0f;

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("World '" + worldName + "' for spawn point '" + path + "' in arena '" + arenaName + "' is not loaded.");
                    return null;
                }
                return new Location(world, x, y, z, yaw, pitch);
            } catch (Exception e) {
                plugin.getLogger().severe("Malformed spawn data for arena '" + arenaName + "' at path '" + path + "': " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Loads all named game spawn points for an arena.
     * @param arenaName The name of the arena.
     * @return A Map of spawnName to Location, or an empty map if none are set.
     * @throws IOException if there's an error reading the file.
     */
    public Map<String, Location> loadGameSpawnPoints(String arenaName) throws IOException {
        Map<String, Location> gameSpawns = new HashMap<>();
        JsonObject arenaData = getArenaData(arenaName);
        if (arenaData == null || !arenaData.has("game-spawn-points") || !arenaData.get("game-spawn-points").isJsonObject()) {
            return gameSpawns; // No game spawn points section
        }

        JsonObject gameSpawnPointsJson = arenaData.getAsJsonObject("game-spawn-points");
        for (Map.Entry<String, JsonElement> entry : gameSpawnPointsJson.entrySet()) {
            String spawnName = entry.getKey();
            JsonElement spawnElement = entry.getValue();

            if (spawnElement.isJsonObject()) {
                JsonObject spawnLoc = spawnElement.getAsJsonObject();
                try {
                    String worldName = spawnLoc.get("world").getAsString();
                    double x = spawnLoc.get("x").getAsDouble();
                    double y = spawnLoc.get("y").getAsDouble();
                    double z = spawnLoc.get("z").getAsDouble();
                    float yaw = spawnLoc.has("yaw") ? spawnLoc.get("yaw").getAsFloat() : 0.0f;
                    float pitch = spawnLoc.has("pitch") ? spawnLoc.get("pitch").getAsFloat() : 0.0f;

                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("World '" + worldName + "' for game spawn '" + spawnName + "' in arena '" + arenaName + "' is not loaded. Skipping.");
                        continue;
                    }
                    gameSpawns.put(spawnName, new Location(world, x, y, z, yaw, pitch));
                } catch (Exception e) {
                    plugin.getLogger().severe("Malformed game spawn data for arena '" + arenaName + "', spawn '" + spawnName + "': " + e.getMessage());
                }
            }
        }
        return gameSpawns;
    }

    /**
     * Deletes a spawn location from arenas.json.
     * @param arenaName The name of the arena.
     * @param path The JSON path to the spawn point (e.g., "lobby-spawn", "game-spawn-points.spawn1").
     * @return true if the spawn point was found and deleted, false otherwise.
     * @throws IOException if there's an error writing to the file.
     */
    public boolean deleteSpawnLocation(String arenaName, String path) throws IOException {
        JsonObject root = loadArenasJson();
        JsonObject arenaData = root.has(arenaName) ? root.getAsJsonObject(arenaName) : null;

        if (arenaData == null) {
            return false; // Arena not found
        }

        String[] pathParts = path.split("\\.");
        JsonObject currentObject = arenaData;
        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            if (!currentObject.has(part) || !currentObject.get(part).isJsonObject()) {
                return false; // Path does not exist
            }
            currentObject = currentObject.getAsJsonObject(part);
        }
        String finalKey = pathParts[pathParts.length - 1];

        if (currentObject.has(finalKey)) {
            currentObject.remove(finalKey);
            // If the parent object for game spawns becomes empty, remove it too
            if (path.startsWith("game-spawn-points.") && currentObject.entrySet().isEmpty()) {
                arenaData.remove("game-spawn-points");
            }
            root.add(arenaName, arenaData); // Add/replace arena data back to root
            saveArenasJson(root);
            plugin.getLogger().info("Deleted spawn location for arena '" + arenaName + "' at path '" + path + "'.");
            return true;
        }
        return false; // Spawn point not found at the specified path
    }
}
