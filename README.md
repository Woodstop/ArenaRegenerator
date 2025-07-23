# **ArenaRegenerator Plugin**

A Minecraft Paper plugin for managing and regenerating arena regions with WorldEdit or FastAsyncWorldEdit. Ideal for Spleef, minigames, or any frequently reset area.

## **Table of Contents**

1. [Features](#features)
2. [Dependencies](#dependencies)
3. [Installation](#installation)
4. [Usage](#usage)
    * [Commands](#commands)
    * [Arena Regen Signs](#arena-regen-signs)
5. [Permissions](#permissions)
6. [Data Storage](#data-storage)
7. [Building from Source](#building-from-source)
8. [Support & Contribution](#support--contribution)

## **Features**

Key features include: saving WorldEdit selections as schematics with origin points, instantly regenerating arenas, clearing arena blocks to air (schematic retained), listing/deleting/viewing info for saved arenas, loading arena boundaries as WorldEdit selections, and creating interactive regen signs with cooldowns.

## **Dependencies**

This plugin requires either **WorldEdit** or **FastAsyncWorldEdit (FAWE)** to be installed on your server.

* [Download WorldEdit](https://dev.bukkit.org/projects/worldedit/files)
* [Download FAWE](https://www.spigotmc.org/resources/fastasyncworldedit.13932/)

**Important:** Only install ONE of these plugins (WorldEdit or FAWE) on your server. Having both can cause conflicts.
## **Installation**

1. Download the latest `ArenaRegenerator-X.X.X.jar` from the [releases](https://github.com/Woodstop/ArenaRegenerator/releases) page (or compile it yourself).
2. Download either **WorldEdit** or **FastAsyncWorldEdit (FAWE)** compatible with your server version.
3. Place both `ArenaRegenerator-X.X.X.jar` and your chosen WorldEdit/FAWE JAR into your server's plugins/ folder.
4. Restart server.

## **Usage**

### **Commands**

Commands use the `/arena` prefix. An alias `/ar` is also available. Replace `<arenaName>` with your desired name for the arena.

* `/arena save <arenaName>`: Saves current WorldEdit selection as a schematic with origin.
* `/arena regen <arenaName>`: Pastes saved arena schematic at original location.
* `/arena clear <arenaName>`: Clears arena blocks to air; schematic remains.
* `/arena list`: Lists all saved arena names.
* `/arena delete <arenaName>`: Deletes arena schematic and data.
* `/arena info <arenaName>`: Displays arena metadata (origin, world, schematic status).
* `/arena select <arenaName>`: Loads saved arena boundaries as WorldEdit selection.

### **Arena Regen Signs**

Create signs to automatically regenerate arenas.

1. Place sign.
2. Line 1: \[RegenArena\] (turns light blue with permission).
3. Line 2: Exact arena name.
4. Players with `arenaregenerator.sign.use` click to regenerate.
5. Default 10-second cooldown; `arenaregenerator.sign.bypass` overrides.

## **Permissions**

All permissions are default: false (requires explicit granting) unless otherwise specified.

* `arenaregenerator.regen`: Allows use of /arena regen.
* `arenaregenerator.save`: Allows use of /arena save.
* `arenaregenerator.list`: Allows use of /arena list.
* `arenaregenerator.delete`: Allows use of /arena delete.
* `arenaregenerator.clear`: Allows use of /arena clear.
* `arenaregenerator.info`: Allows use of /arena info.
* `arenaregenerator.select`: Allows use of /arena select.
* `arenaregenerator.sign.create`: Allows players to create \[RegenArena\] signs.
* `arenaregenerator.sign.use`: Allows players to use \[RegenArena\] signs.
* `arenaregenerator.sign.bypass`: Allows players to bypass the cooldown on \[RegenArena\] signs.
* `arenaregenerator.sign.break`: Allows players to break \[RegenArena\] signs.

## **Data Storage**

The plugin stores arena metadata (origin, world) in plugins/ArenaRegenerator/arenas.json. Schematic files (.schem) are stored in the same directory.

## **Building from Source**

To build from source:

1. Clone repository.
2. Ensure Java 21 and JAVA\_HOME are set.
3. Build with Maven (mvn clean package).  
   JAR is in target/.

## **Support & Contribution**

If you encounter issues, have suggestions, or wish to contribute, visit the [GitHub repository](https://github.com/Woodstop/ArenaRegenerator).