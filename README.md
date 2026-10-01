# Stun FX

A **client-side** Fabric mod for mace PvP. It adds visual and audio feedback for stunslams and mace hits, a totem pop counter and a death sound. Everything is configurable in-game.

## Features

- **Stunslam / Black Flash** (Jujutsu Kaisen style): break a shield, then hit within 0.7 seconds. Choose whether only a mace smash (falling or elytra) or any weapon (axe included) counts. Five looks (Manga, Cursed Blue, Gold, Violet, Minimal) and colours, size, duration and sound are adjustable.
- **Shockwave ring** on every mace smash (falling or elytra, not on a plain hit on the ground)
- **Totem pop effect**, **kill effect**
- **Combo counter** ("BLACK FLASH x3"): counts stunslams in a row and only ends when you fail one or die
- **Totem pop counter** with size, drag-and-drop position and an optional pop sound
- **Death sound** when a player dies or a death message appears (Unstable SMP style)
- One compact settings screen with an Edit page per effect, tooltips and a reset button

## Controls

- Open settings: `Right Shift` or `/stunfx` (rebindable under Controls > Stun FX)
- `/popcounter` toggles the counter, `/popcounter reset` clears it, `/deathsound` toggles the death sound
- `/stunfx test` shows every effect one after another, `/stunfx test stunslam|ring|totem|kill|sound` shows one

## Versions

Each Minecraft version has its own folder and its own Gradle wrapper:

| Minecraft | Folder | Notes |
|---|---|---|
| 1.21.11 | [`1.21.11`](1.21.11) | main version, most tested |
| 1.21.4 | [`1.21.4`](1.21.4) | same features, port |
| 26.2 | [`26.2`](26.2) | same features, port, needs Java 25+ (no obfuscation mappings, uses Mojang names) |

The jars are named `StunFX-<minecraft version>-<mod version>.jar`.

Requires Fabric Loader and Fabric API.

## Building

```
cd 1.21.11
./gradlew build
```

The jar is written to `<folder>/build/libs`. Each `gradle.properties` contains a local `org.gradle.java.home` path. Change or remove it to match your own JDK.

## Sounds

The sound files (Black Flash and death sound) are not part of this repository. Put your own `black_flash.ogg`, `black_flash.wav` and `unstable_death.ogg` into `src/main/resources/assets/maseffectsplus/sounds/` of the version you build to get audio. Without them the mod still runs, only the sounds are missing.

## Notes

Fan-made visual mod. Not affiliated with or endorsed by Mojang, Microsoft or the creators of *Jujutsu Kaisen*.
Large parts of the code were written with the help of an AI coding assistant (Claude).

## License

MIT, see [LICENSE](LICENSE). Sound files may be subject to their own rights.
