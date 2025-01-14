# Common

A common module that primarily contains a model for game objects including towers, enemies, projectiles, enemy tags, waves, etc. These game objects can be added by mods and loaded into game simulation instances on the server or client.

## Game Object Model

Game objects (`GameObject` class) include types of towers, enemies, projectiles, maps, waves, etc., which all have a role in the game simulation.

Game objects are the core of the advanced modding system that we aim to show off in this project. All types of game objects can easily be defined through a Kotlin DSL, and provide powerful features such as type safety and cross-references.

### Features of Game Objects

Game objects support the following features:

* Basic attributes (e.g. tower damage, enemy movement speed, names, descriptions)
  * This is supported by our use of Kotlin Serialization.
* Type safety
  * Kotlin DSLs are type-safe by default.
* Assets (images, sounds)
  * These are encoded in Base64 in this project for simplicity; the modding SDK populates these fields when loading a mod's game objects. Encoding the entire asset works for this project due to having a low-poly art style with small asset sizes. In another game with much larger assets, they would need to be represented by asset ID's instead.
* Safe cross-references between game objects
  * Powered by Kotlin delegates, one can simply reference one game object from another like any other Kotlin variable, including nested and even circular references. During serialization, the other game object's ID will be serialized in lieu of the actual game object. During the game simulation logic, the original reference to the other game object can be restored using the ID.
  * Game object definitions can also be directly nested within one another in lieu of using cross-references. This is handled similarly to anonymous classes in other programming languages - these anonymous definitions are compiled into top-level definitions with a random ID and referenced from the parent game object via its ID.

### Serialization

A core feature of all Game Objects is that they can be serialized easily by Kotlin Serialization. Game objects are serialized to JSON and/or deserialized in the following situations:

* When executing the main function of your mod project, your mod will be compiled into a structure of Kotlin objects, serialized into JSON, and stored on your computer. This file can then be loaded by the client.
* When the client wants to load a mod, the client loads the mod JSON, and then deserializes it and loads the contents into the client simulation.
* When the server needs to load a mod to verify a client's gameplay, the server will read the mod JSON sent from the client and then load it into its own simulation.

Note that in a more finished game, we anticipate that instead of being stored on the player's computer, mod files can also be stored on a central cloud server to allow the game community to easily play and share mods. Such a cloud server is not implemented in this version of the project to reduce its scope.

## Types of Game Objects

There are a large number of game objects in-game which allow for flexible definitions for towers and enemies which can be customized by modders.

### Tower Type

A type of tower that can be placed by the player to attack enemies. It has attributes like cost, base damage, range modifier, and targeting behaviors, as well as how these stats change when the tower is upgraded. Each tower can contain one or multiple "turrets" which independently target and attack enemies. Note that Tower Types are currently the only game object type that are loaded independently of the game map itself, meaning that it is possible to add new tower types without replacing the vanilla game map with a modded one.

### Tower Turret

Each tower turret can target and attack one or multiple enemies at once at certain intervals. For example, some towers may only spot enemies in certain areas or angles that may or may not be affected by the tower's range modifier. Attacks are then calculated based on attack behaviors.

### Tower Attack

Attack behavior is triggered when the tower attempts to attack an enemy target. This could include instant damage, splash damage, chain attacks, or spawning projectiles. Attack behaviors ultimately deal damage to enemies (and/or trigger effects like slowing) using damage behaviors.

### Tower Damage

Damage behaviors are triggered by a tower attack and deal a multiple of the tower's base damage. Towers may also have damage multipliers against certain enemy tags (categories of enemies).

### Enemy Type

Enemies are moving entities that the player must prevent from reaching the home tile on the map. Enemies will try to navigate the map using a tile navigation map built using the Dijkstra's Algorithm. (If trapped, enemies will ignore collision and try to move towards the closest tile that has a path to the player's home tile.) The player can use towers to block enemies and force them to take a longer path (this is known as "mazing" in the tower defense community), but the game will not allow the player to block off all available paths for an enemy. Enemy types have attributes such as hitbox size, health, speed, and tags.

### Enemy Tag

An enemy tag is a category of enemy like "armored" which let the enemy interact with the game world in a special way. In the current version of the game, it affects how much the enemy is affected by certain tower attacks.

### Projectile Type

Projectiles are caused by towers or other effects and can damage enemies on the map. Projectiles allow a delay between the tower attack and the enemy being damaged, and allow interesting gameplay behavior like splash damage, piercing through enemies, or a delayed "mortar round" projectile that can miss faster targets.

### Game Map

The Game Map is the entrypoint of the game world and determines the map size, the types of generated map tiles, as well as references to other objects that define how the game is played such as the Wave Definition (which in turn controls how all enemies in the game are spawned). When a mod is loaded, if it contains a Game Map, the game will be started with this Game Map rather than the one in the official game.

### Map Tile Type

Represents a type of tile which can be generated on the map, such as ground, dirt, or rock. Different tile types differ in appearance, whether enemies can walk on it, whether towers can be placed on it, as well as the movement speed of enemies on the tile.

### Spawnpoint

A spawnpoint with a specific location in the map and set of spawnpoint tags.

### Spawnpoint Tag

A tag for a spawnpoint that can be accessed by waves to customize which spawnpoints enemies should spawn at.

### Wave Definition

A wave definition system inspired by the Mob-Arena plugin for Minecraft. Waves of enemies can be defined to either spawn once or recur at a specific interval. At each wave number, the wave definition will find eligible waves and pick a wave to spawn based on the wave's priority. This object also defines how much money is awarded each wave, how fast enemy amount and level grows over time, and the final wave after which the player will win the game.

### Wave

A wave contains a weighted list of enemies that will be spawned during the wave, as well as various modifiers such as the amount and level of enemies spawned, how quickly the enemies will spawn over the course of the wave, as well as the duration of the wave. Waves can also be "special" waves where all enemies spawned will be the same random type chosen at the start of the wave, rather than each individual enemy having a different random type.
