# **ArenaRegenerator Plugin**

A Minecraft Paper plugin for managing and regenerating arena regions with WorldEdit or FastAsyncWorldEdit. Ideal for Spleef, minigames, or any frequently reset area.

## **Table of Contents**

1. [Features](#features)
2. [Dependencies](#dependencies)
3. [Installation](#installation)
4. [Usage](#usage)
    * [Commands](#commands)
    * [Arena Reset Signs](#arena-reset-signs)
5. [Permissions](#permissions)
6. [Data Storage](#data-storage)
7. [Building from Source](#building-from-source)
8. [Support & Contribution](#support--contribution)

## **Features**

Key features include: saving WorldEdit selections as schematics with origin points, instantly regenerating arenas, clearing arena blocks to air (schematic retained), listing/deleting/viewing info for saved arenas, loading arena boundaries as WorldEdit selections, and creating interactive reset signs with cooldowns.

## **Dependencies**

This plugin requires either **WorldEdit** or **FastAsyncWorldEdit (FAWE)** to be installed on your server.

* [Download WorldEdit](https://dev.bukkit.org/projects/worldedit/files)
* [Download FAWE](https://www.spigotmc.org/resources/fastasyncworldedit.13932/)

**Important:** Only install ONE of these plugins (WorldEdit or FAWE) on your server. Having both can cause conflicts.
## **Installation**

1. Download the latest `ArenaRegenerator-X.X.X.jar` from the [releases](https://www.google.com/search?q=https://github.com/Woodstop/ArenaRegenerator/releases) page (or compile it yourself).
2. Download either **WorldEdit** or **FastAsyncWorldEdit (FAWE)** compatible with your server version.
3. Place both `ArenaRegenerator-X.X.X.jar` and your chosen WorldEdit/FAWE JAR into your server's plugins/ folder.
4. Restart server.

## **Usage**

### **Commands**

Commands require arenaregenerator.\<command\> permission. Use \<arenaName\> for your arena name.

* `/savearena <arenaName>`: Saves current WorldEdit selection as a schematic with origin.
    * **Usage**: Select region with WorldEdit, then run.
    * **Permission**: `arenaregenerator.savearena`
* `/regenarena <arenaName>`: Pastes saved arena schematic at original location.
    * **Usage**: `/regenarena myArena`
    * **Permission**: `arenaregenerator.regenarena`
* `/cleararena <arenaName>`: Clears arena blocks to air; schematic remains.
    * **Usage**: `/cleararena myArena`
    * **Permission**: `arenaregenerator.cleararena`
* `/arenas`: Lists all saved arena names.
    * **Usage**: `/arenas`
    * **Permission**: `arenaregenerator.arenas`
* `/delarena <arenaName>`: Deletes arena schematic and data.
    * **Usage**: `/delarena myArena`
    * **Permission**: `arenaregenerator.delarena`
* `/arenainfo <arenaName>`: Displays arena metadata (origin, world, schematic status).
    * **Usage**: `/arenainfo myArena`
    * **Permission**: `arenaregenerator.arenainfo`
* `/selectarena <arenaName>`: Loads saved arena boundaries as WorldEdit selection.
    * **Usage**: `/selectarena myArena`
    * **Permission**: `arenaregenerator.selectarena`

### **Arena Reset Signs**

Create signs to automatically regenerate arenas.

1. Place sign.
2. Line 1: \[ResetArena\] (turns light blue with permission).
3. Line 2: Exact arena name.
4. Players with `arenaregenerator.sign.use` click to regenerate.
5. Default 10-second cooldown; `arenaregenerator.sign.bypass` overrides.

## **Permissions**

All permissions are default: false (requires explicit granting) unless otherwise specified.

* `arenaregenerator.regenarena`: Allows use of /regenarena.
* `arenaregenerator.savearena`: Allows use of /savearena.
* `arenaregenerator.arenas`: Allows use of /arenas.
* `arenaregenerator.delarena`: Allows use of /delarena.
* `arenaregenerator.cleararena`: Allows use of /cleararena.
* `arenaregenerator.arenainfo`: Allows use of /arenainfo.
* `arenaregenerator.selectarena`: Allows use of /selectarena.
* `arenaregenerator.sign.create`: Allows players to create \[ResetArena\] signs.
* `arenaregenerator.sign.use`: Allows players to use \[ResetArena\] signs.
* `arenaregenerator.sign.bypass`: Allows players to bypass the cooldown on \[ResetArena\] signs.
* `arenaregenerator.sign.break`: Allows players to break \[ResetArena\] signs.

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