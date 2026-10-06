# Stun FX

Client-side mace PvP effects for Fabric: a **Black Flash** on stunslams, a combo counter, shockwave rings on mace smashes, a totem pop counter and a death sound. Everything is configurable in one compact in-game screen.

Download: [Modrinth](https://modrinth.com/mod/stunfx)

## Features

- **Black Flash on stunslams** (Jujutsu Kaisen style): break a shield, then land the follow-up hit within 0.7 seconds. Choose whether only a mace smash (falling or elytra) counts or a hit with any weapon (axe included).
- **Combo counter** ("BLACK FLASH x3"): counts stunslams in a row and only ends when you fail one or die. Time on screen and position are adjustable.
- **Mace ring** on every real mace smash, with an option to show it together with the Black Flash.
- **Totem pop effect**, **kill effect** and a **totem pop counter** with an optional pop sound.
- **Death sound** when a player dies (Unstable SMP style).
- Five looks (Manga, Cursed Blue, Gold, Violet, Minimal), colours, size, duration and sound per effect. Drag and drop the counters where you want them.

## Controls

- Open settings: `Right Shift` or `/stunfx` (all keys can be rebound under Controls > Stun FX)
- `/stunfx test` shows every effect, `/stunfx test stunslam|ring|totem|kill|sound` shows one
- `/stunfx debug` shows above the hotbar why a stunslam did or did not trigger
- `/popcounter` toggles the counter, `/popcounter reset` clears it, `/deathsound` toggles the death sound

## Versions

Each Minecraft version has its own folder and its own Gradle wrapper:

| Minecraft | Folder | Notes |
|---|---|---|
| 1.21.11 | [`1.21.11`](1.21.11) | main version, most tested |
| 1.21.4 | [`1.21.4`](1.21.4) | same features, port, not tested in game yet |
| 26.2 | [`26.2`](26.2) | same features, port, not tested in game yet, needs Java 25+ (no obfuscation mappings, uses Mojang names) |

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

- Needs Fabric Loader and Fabric API. Works on 1.21.11, 1.21.4 and 26.2. The 1.21.4 and 26.2 builds are not tested in game yet, please report problems.
- The mod only changes what you see and hear on your own screen. It sends nothing to servers.
- Low impact on FPS.

Fan-made. Not affiliated with or endorsed by Mojang, Microsoft or the creators of *Jujutsu Kaisen*.

## License

MIT, see [LICENSE](LICENSE). Sound files may be subject to their own rights.
