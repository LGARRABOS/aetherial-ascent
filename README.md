# Aetherial Ascent

NeoForge mod for **Minecraft 1.21.1** that adds **smooth vertical travel between the Overworld and The Aether** using altitude thresholds. It moves the **whole convoy** (vehicle root plus passengers), preserves momentum, uses **vanilla-style dimension changes** (`DimensionTransition`), and checks arrival space against the **combined collision box** so large rides do not clip into terrain.

## Requirements

| Dependency | Notes |
|------------|--------|
| **NeoForge** | `21.1.228` (see `gradle.properties`; use a compatible 1.21.1 build) |
| **The Aether** | Only **required** content mod (declared in `neoforge.mods.toml`) |

Java **21** matches what Minecraft 1.21.1 ships with.

The mod does **not** depend on **Create**, **Create Aeronautics**, or **Sable**; those are only used on the **`dev`** branch to author and test in a heavier mod pack.

## Features

- **Overworld → Aether** when the transport root is above a configurable Y threshold (default on vanilla-like presets: high altitude).
- **Aether → Overworld** when below a configurable Y threshold (underworld side).
- **Cooldown**, short **blindness / slow falling** presentation (duration configurable in `serverconfig`).
- **Arrival placement** uses the destination heightmap plus clearance and scans upward if the volume is blocked; transition is **cancelled** with feedback if no free space is found.
- **Server configuration** (`serverconfig/aether-ascent-server.toml`): thresholds, landing height, cooldown, effect duration, clearance, collision padding, vertical search limit.
- **In-game config UI** (NeoForge **Mods → Aetherial Ascent → Config**) when the mod is installed on the **client** (optional on dedicated servers; see below).

## Installation

- Put **`aether_ascent-<version>.jar`** in the `mods` folder on the **server** (required for the logic to run).
- **Dedicated multiplayer:** gameplay is **server-side** and this project does **not** register custom NeoForge network payloads. Players can **often omit the jar on the client** as long as they use the same NeoForge stack and **The Aether** as the server. If your launcher enforces an identical mod list, include the jar on clients too.
- **Single-player / Open to LAN:** keep the jar on your game instance so the integrated server loads it and you still get the Mods config screen.

## Configuration

After a world has loaded once, edit:

`config` folder is per instance; server worlds store it under:

`saves/<world>/serverconfig/aether-ascent-server.toml`

Changing values while the game is running may apply depending on NeoForge reload behaviour; a restart is always safe.

## Building from source

```bash
./gradlew build
```

The output JAR is under `build/libs/`.

Branch **`main`** keeps a **minimal Gradle layout**: no third-party mod repositories and no `localRuntime` lines so CI and publishing stay clean. **`dev`** adds Modrinth / CurseMaven / JitPack and `localRuntime` entries (Aether plus optional stack such as Create / Create Aeronautics / Sable for **local testing only**). Prefer **`dev`** for that integrated playtest workflow; use branch protections on **`main`** if you want to block accidental commits of that environment.

## Repository

- **GitHub:** [LGARRABOS/aetherial-ascent](https://github.com/LGARRABOS/aetherial-ascent)

## License

See `gradle.properties` / mod metadata (`mod_license`). All Rights Reserved unless you change it in your fork.
