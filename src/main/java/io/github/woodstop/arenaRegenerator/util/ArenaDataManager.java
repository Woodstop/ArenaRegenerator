package io.github.woodstop.arenaRegenerator.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import io.github.woodstop.arenaRegenerator.ArenaRegenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages loading and saving arena data from arenas.json and schematic files.
 */
public class ArenaDataManager {

    private final File dataFolder;
    private final File arenasJsonFile;

    public ArenaDataManager() {
        this.dataFolder = ArenaRegenerator.getInstance().getDataFolder();
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs(); // Ensure data folder exists
        }
        this.arenasJsonFile = new File(dataFolder, "arenas.json");
    }

    /**
     * Loads the root JsonObject from arenas.json.
     * If the file does not exist or is empty, an empty JsonObject is returned.
     *
     * @return The root JsonObject containing all arena data.
     * @throws IOException If an error occurs during file reading.
     */
    public JsonObject loadArenasJson() throws IOException {
        if (!arenasJsonFile.exists() || arenasJsonFile.length() == 0) {
            return new JsonObject(); // Return empty object if file doesn't exist or is empty
        }
        try (FileReader reader = new FileReader(arenasJsonFile)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * Saves the given JsonObject back to arenas.json.
     *
     * @param root The JsonObject to save.
     * @throws IOException If an error occurs during file writing.
     */
    public void saveArenasJson(JsonObject root) throws IOException {
        try (FileWriter writer = new FileWriter(arenasJsonFile)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(root));
        }
    }

    /**
     * Retrieves the JsonObject for a specific arena.
     *
     * @param arenaName The name of the arena.
     * @return The JsonObject for the arena, or null if not found.
     * @throws IOException If an error occurs during file reading.
     */
    public JsonObject getArenaData(String arenaName) throws IOException {
        JsonObject root = loadArenasJson();
        if (root.has(arenaName)) {
            return root.getAsJsonObject(arenaName);
        }
        return null;
    }

    /**
     * Loads a WorldEdit Clipboard from a schematic file for a given arena.
     *
     * @param arenaName The name of the arena.
     * @return The loaded Clipboard, or null if the schematic file is not found or invalid.
     * @throws IOException If an error occurs during file reading.
     */
    public Clipboard loadArenaSchematic(String arenaName) throws IOException {
        File schematicFile = new File(dataFolder, arenaName + ".schem");
        if (!schematicFile.exists()) {
            return null; // Schematic file not found
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            ArenaRegenerator.getInstance().getLogger().warning("Invalid schematic format for file: " + schematicFile.getName());
            return null; // Invalid format
        }

        try (FileInputStream stream = new FileInputStream(schematicFile)) {
            ClipboardReader reader = format.getReader(stream);
            return reader.read();
        }
    }

    /**
     * Checks if an arena exists in the data file.
     * @param arenaName The name of the arena.
     * @return true if the arena exists, false otherwise.
     * @throws IOException If an error occurs during file reading.
     */
    public boolean arenaExists(String arenaName) throws IOException {
        JsonObject root = loadArenasJson();
        return root.has(arenaName);
    }

    /**
     * Gets the schematic file for a given arena name.
     * @param arenaName The name of the arena.
     * @return The File object for the schematic.
     */
    public File getSchematicFile(String arenaName) {
        return new File(dataFolder, arenaName + ".schem");
    }

    /**
     * Calculates and returns the WorldEdit Region (CuboidRegion) for a given arena.
     * This method combines loading arena data, loading the schematic, and calculating
     * the absolute min/max points based on the saved origin and schematic dimensions.
     *
     * @param arenaName The name of the arena.
     * @return A CuboidRegion representing the arena's boundaries, or null if data is missing/invalid.
     * @throws IOException If an error occurs during file reading.
     */
    public Region getArenaRegion(String arenaName) throws IOException {
        JsonObject data = getArenaData(arenaName);
        if (data == null) {
            return null; // Arena data not found
        }

        int originX = data.get("x").getAsInt();
        int originY = data.get("y").getAsInt();
        int originZ = data.get("z").getAsInt();
        String worldName = data.get("world").getAsString();

        org.bukkit.World bukkitWorld = org.bukkit.Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            // The world is not loaded, cannot create a region for it
            return null;
        }
        World weWorld = BukkitAdapter.adapt(bukkitWorld);

        Clipboard clipboard = loadArenaSchematic(arenaName);
        if (clipboard == null) {
            // Schematic missing or corrupted, cannot determine region dimensions
            return null;
        }

        // Calculate the absolute min and max points of the selection
        // The schematic's min/max points are relative to its internal origin (often 0,0,0)
        // We need to add the saved arena's origin to these relative points.
        BlockVector3 schematicMin = clipboard.getMinimumPoint();
        BlockVector3 schematicMax = clipboard.getMaximumPoint();

        BlockVector3 absoluteMin = BlockVector3.at(
                originX + schematicMin.x(),
                originY + schematicMin.y(),
                originZ + schematicMin.z()
        );
        BlockVector3 absoluteMax = BlockVector3.at(
                originX + schematicMax.x(),
                originY + schematicMax.y(),
                originZ + schematicMax.z()
        );

        // Create and return the CuboidRegion
        return new CuboidRegion(weWorld, absoluteMin, absoluteMax);
    }
}
