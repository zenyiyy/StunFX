# Stun FX

Client-side mace PvP effects for Fabric: a **Black Flash** on stunslams, a combo counter, shockwave rings on mace smashes, a totem pop counter and a death sound. Everything is configurable in one compact in-game screen.

Stun FX only adds visual and audio effects on your own screen. It does not change combat or hits, and it sends nothing to servers.

Download: [Modrinth](https://modrinth.com/mod/stunfx)

## Features

- **Black Flash on stunslams** (Jujutsu Kaisen style): shows when a shield break is followed by a mace smash within 0.7 seconds. A smash follows the game's rule: a fall of more than 1.5 blocks, not while gliding. Optionally, a hit with any weapon after the shield break can count too.
- **Combo counter** ("BLACK FLASH x3"): counts stunslams in a row and ends when one fails or you die. Time on screen and position are adjustable.
- **Mace ring** on every real mace smash, with an option to show it together with the Black Flash.
- **Totem pop effect** and **kill effect** for targets you hit, plus a **totem pop counter** for every player within 128 blocks, with an optional pop sound.
- **Death sound** when a player dies (Unstable SMP style).
- Five looks (Manga, Cursed Blue, Gold, Violet, Minimal), colours, size, duration and sound per effect. Drag and drop the counters where you want them.

## Controls

- Open settings: `Right Shift` or `/stunfx` (all keys can be rebound under Controls > Stun FX)
- `/stunfx test` shows every effect, `/stunfx test stunslam|ring|totem|kill|sound` shows one
- `/stunfx debug` shows above the hotbar why an effect did or did not show
- `/popcounter` toggles the counter, `/popcounter reset` clears it, `/deathsound` toggles the death sound

## Versions

Each Minecraft version has its own folder and its own Gradle wrapper:

| Minecraft | Folder | Notes |
|---|---|---|
| 1.21.11 | [`1.21.11`](1.21.11) | main version |
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

- Works on 1.21.11, 1.21.4 and 26.2. Please report problems in the issues.
- Low impact on FPS.

Fan-made. Not affiliated with or endorsed by Mojang, Microsoft or the creators of *Jujutsu Kaisen*.

## License

MIT, see [LICENSE](LICENSE). Sound files may be subject to their own rights.
