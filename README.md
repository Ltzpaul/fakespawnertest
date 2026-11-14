# Fake Spawner Mod (KingSMP / KingMC)

A Fabric **client-only** cosmetic mod targeting Minecraft Java 1.21.11.

## Included features

- Copper Block item stacks are displayed as **Lồng triệu hồi Skeleton** in aqua.
- Copper Block tooltips gain the two requested lore lines.
- Copper Block's item and placed-block model are redirected to the vanilla spawner model.
- Nearby Copper Blocks get a small, client-created Skeleton preview plus smoke and flame particles.
- Right-clicking a Copper Block while holding Copper Blocks updates a **local display counter** and plays the metal-place sound.
- Right-clicking a Copper Block with an empty hand or pickaxe opens a six-row chest-style local GUI. Other held items keep normal right-click behavior.
- The GUI shows eight stacks of 64 bones and the requested bottom-row controls.
- The local EXP button resets the display value, plays client sounds, shows an action-bar message and a 14-second HUD reward notice.
- Mining a Copper Block with a suitable tool shows the local stack-count message. The server still handles the real block break and its ordinary drop.

## Important multiplayer limitation

This build is deliberately client-side. The stack counter, GUI, Skeleton preview, EXP amount, sale screen and HUD are **local visual simulation only**. It does not create or merge real server spawners, does not generate server drops or EXP, does not credit money, and cannot return extra Copper Blocks to the server inventory. The actual server continues to control inventory, block drops, XP and economy. Use a server plugin/custom server implementation if those gameplay effects are required.

## Versions

- Minecraft: `1.21.11`
- Java toolchain: `21`
- Fabric Loader: `0.18.1` minimum
- Fabric API: `0.141.3+1.21.11`
- Yarn mappings: `1.21.11+build.6`
- Fabric Loom: `1.14-SNAPSHOT`
- Gradle distribution: `8.14.2`

## Build

1. Install a 64-bit JDK 21.
2. Extract the ZIP to a short path without Vietnamese accents, for example `C:\MinecraftMods\FakeSpawnerMod`.
3. Open a terminal in the extracted folder.
4. Run `gradlew.bat build` on Windows, or `./gradlew build` on Linux/macOS. The first build needs an internet connection to download Gradle, Minecraft and Fabric dependencies.
5. The remapped mod JAR is written to `build/libs/` (normally `fake-spawner-1.0.0.jar`).

The wrapper configuration is pinned to Gradle 8.14.2. The included launcher scripts bootstrap that distribution if it is not already cached.

## Install

Put the built `fake-spawner-1.0.0.jar` in the Minecraft instance's `mods` folder. The instance must use Fabric Loader for Minecraft 1.21.11 and include Fabric API `0.141.3+1.21.11`.

## Controls

- Place Copper Blocks as usual; their appearance is replaced client-side.
- Right-click a placed Copper Block while holding Copper Blocks: increments the local counter by the held stack size.
- Right-click a placed Copper Block with an empty hand or pickaxe: opens the local 6-row GUI.
- Click the experience bottle (slot 49): collects the locally simulated 512 EXP display value.
- Mine the Copper Block: displays the stored local counter before vanilla/server block breaking continues. The local count is retained while the player moves out of preview range, and reset when changing worlds.
