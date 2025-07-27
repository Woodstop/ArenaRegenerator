![Java](https://img.shields.io/badge/Java-21-blue)
![License](https://img.shields.io/github/license/Woodstop/ArenaRegenerator)
![Release](https://img.shields.io/github/v/release/Woodstop/ArenaRegenerator)
![Issues](https://img.shields.io/github/issues/Woodstop/ArenaRegenerator)

# **ArenaRegenerator Plugin**

ArenaRegenerator is a plugin for Minecraft servers that handles arena saving, regeneration, and full customizable minigame logic using WorldEdit or FAWE.

This plugin manages and regenerates WorldEdit or FAWE-defined arenas. Includes full minigame support (e.g., lobby, countdown, game state, win detection) and interactive signs for easy use.

## **Table of Contents**

1. [Features](#features)
2. [Dependencies](#dependencies)
3. [Installation](#installation)
4. [Usage](#usage)
    * [Commands](#commands)
    * [Interactive Signs](#interactive-signs)
5. [Permissions](#permissions)
6. [Data Storage](#data-storage)
7. [Building from Source](#building-from-source)
8. [Support & Contribution](#support--contribution)

## **Features**

* Arena Management:

   - Save WorldEdit selections as arenas.

   - Instantly clear or regenerate arenas.

   - List, delete, and view detailed information for saved arenas.

* Minigame System:
  - **Lobby System**: Players can join an arena's lobby and wait for enough players.

  - **Countdown**: A configurable countdown begins when the minimum player count is met.

  - **Game Start**: Players are moved from the lobby to game spawn points, inventories are cleared (configurable), and game mode is set.

  - **Game End**: Game ends after a set duration or when a win condition (e.g., last player standing) is met. Players are teleported out, their state is restored (configurable), and the arena is reset.

  - **Spectator Mode**: Players leaving the arena boundaries during a game can be moved to spectator mode.

  - **Configurable Rules**: Set minimum/maximum players, game duration, lobby countdown, items and game mode on join, what blocks can be broken or placed, and damage prevention per arena.

* Interactive Signs:
  - Regenerate arenas on click with `[RegenArena]` signs
  - Join minigame arenas with `[JoinArena]` signs
  - Leave minigame arenas with `[LeaveArena]` signs


### /arena save and /arena info
<p align="center">
  <img src="https://github.com/Woodstop/ArenaRegenerator/blob/main/ArenaSave.gif?raw=true" />
</p>


### /arena clear and /arena regen
<p align="center">
  <img src="https://github.com/Woodstop/ArenaRegenerator/blob/main/ArenaRegen.gif?raw=true" />
</p>


### Arena Regen Signs
<p align="center">
  <img src="https://github.com/Woodstop/ArenaRegenerator/blob/main/RegenSign.gif?raw=true" />
</p>

### Join Arena Signs
<p align="center">
    <img src="https://raw.githubusercontent.com/Woodstop/ArenaRegenerator/fd916e0b314a1e942c7eddfa959ab092866aa8b3/JoinArenaSign.gif" />
</p>

### Minigame Logic
<p align="center">
    <img src="https://raw.githubusercontent.com/Woodstop/ArenaRegenerator/1e91d9673727e0268a68fbd1a2bac4fe01e7679b/SpleefGame.gif" />
</p>

## **Dependencies**

This plugin requires either **WorldEdit** or **FastAsyncWorldEdit (FAWE)** to be installed on your server.

* [Download WorldEdit](https://dev.bukkit.org/projects/worldedit/files)
* [Download FAWE](https://www.spigotmc.org/resources/fastasyncworldedit.13932/)

**Important:** Only install ONE of these plugins (WorldEdit or FAWE) on your server. Having both can cause conflicts.
## **Installation**

1. Download the latest `ArenaRegenerator-X.X.X.jar` from the [releases](https://github.com/Woodstop/ArenaRegenerator/releases) page (or compile it yourself).
2. Download either **WorldEdit** or **FastAsyncWorldEdit (FAWE)** that is compatible with your server version.
3. Place both `ArenaRegenerator-X.X.X.jar` and your chosen WorldEdit/FAWE JAR into your server's plugins/ folder.
4. Restart server.

## **Usage**

### **Interactive Signs**

Create signs to automatically regenerate arenas, join minigames, or leave minigames.

1. Place sign.
2. Line 1: `[RegenArena]` or `[JoinArena]` or `[LeaveArena]`.
3. Line 2: Your exact arena name.
4. Players with appropriate permissions can click to use.
5. Default 10-second cooldown; `arenaregenerator.sign.bypass` overrides.

## **Commands and Permissions**

Commands use the `/arena` prefix. An alias `/ar` is also available. Replace `<arenaName>` with your desired name for the arena.

`<arenaType>` can be either `lobby`, `exit`, `spectator`, or `game`.

| **Permission Node**                  | **Description**                                                      | **Command**                                           |
|--------------------------------------|----------------------------------------------------------------------|-------------------------------------------------------|
| `arenaregenerator.regen`             | Allows use of the regen function                                     | `/arena regen`                                        |
| `arenaregenerator.save`              | Allows saving an arena                                               | `/arena save`                                         |
| `arenaregenerator.list`              | Allows listing saved arenas                                          | `/arena list`                                         |
| `arenaregenerator.delete`            | Allows deleting an arena                                             | `/arena delete`                                       |
| `arenaregenerator.clear`             | Allows clearing an arena without deleting the schematic              | `/arena clear`                                        |
| `arenaregenerator.info`              | Allows viewing info about the current arena                          | `/arena info`                                         |
| `arenaregenerator.select`            | Allows selecting a region for an arena                               | `/arena select`                                       |
| `arenaregenerator.setspawn`          | Allows setting lobby, exit, spectator, and game spawns for minigames | `/arena setspawn <arenaName> <arenaType> [spawnName]` |
| `arenaregenerator.delspawn`          | Allows deleting spawn points for minigames                           | `/arena delspawn <arenaName> <arenaType> [spawnName]` |
| `arenaregenerator.join`              | Allows joining minigame arenas                                       | `/arena join <arenaName>`                             |
| `arenaregenerator.leave`             | Allows leaving minigame arenas                                       | `/arena leave`                                        |
| `arenaregenerator.reload`            | Allows reloading the plugin configuration                            | `/arena reload`                                       |
| `arenaregenerator.sign.create.regen` | Allows players to create `[RegenArena]` signs                        | *Create sign with tags*                               |
| `arenaregenerator.sign.create.join`  | Allows players to create `[JoinArena]` signs                         | *Create sign with tags*                               |
| `arenaregenerator.sign.create.use`   | Allows players to create `[LeaveArena]` signs                        | *Create sign with tags*                               |
| `arenaregenerator.sign.use.regen`    | Allows players to use `[RegenArena]` signs                           | *Click `[RegenArena]` sign*                           |
| `arenaregenerator.sign.use.join`     | Allows players to use `[JoinArena]` signs                            | *Click `[JoinArena]` sign*                            |
| `arenaregenerator.sign.use.leave`    | Allows players to use `[LeaveArena]` signs                           | *Click `[LeaveArena]` sign*                           |
| `arenaregenerator.sign.bypass`       | Allows players to bypass sign cooldowns                              | *Click signs repeatedly*                              |
| `arenaregenerator.sign.break`        | Allows players to break interactive signs                            | *Break sign block*                                    |


## **Data Storage**

The plugin stores: 

* Arena Metadata and Spawn Points: Located in `plugins/ArenaRegenerator/arenas.json`. This file contains the origin, world, and all configured lobby, exit, spectator, and named game spawn points for each arena.

* Schematic Files: Located in `plugins/ArenaRegenerator/schematics/`. These are the WorldEdit schematic files (.schem) for each saved arena.

* Minigame Configurations: Located in `plugins/ArenaRegenerator/config.yml`. This file defines the rules and settings for which saved arenas function as minigames (e.g., min/max players, game duration, specific game rules).

## **Building from Source**

To build from source:

1. Clone repository.
2. Ensure Java 21 and JAVA\_HOME are set.
3. Build with Maven (`mvn clean package`).  
   The compiled JAR will be in the `target/` directory.

## **Support & Contribution**

If you encounter issues, have suggestions, or wish to contribute, visit the [GitHub repository](https://github.com/Woodstop/ArenaRegenerator).