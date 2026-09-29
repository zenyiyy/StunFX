# MasEffects+

A **client-side** Fabric mod for mace PvP. It adds visual and audio feedback for stunslams and mace hits, a totem pop counter and a death sound. Everything is configurable in-game.

## Features

- **Black Flash** (Jujutsu Kaisen style) when you land a stunslam: break a shield, then hit within 0.7 seconds. Choose whether only a mace smash (falling or elytra) or any weapon counts.
- **Shockwave ring** on every mace hit
- **Totem pop counter** in the top right
- **Death sound** on every death message (Unstable SMP style)
- In-game settings: size, colours, timing, sound

## Controls

- Open settings: `Right Shift` or `/maseffects` (rebindable under Controls > MasEffects+)
- `/popcounter` and `/deathsound` toggle the counter and the death sound
- `/maseffects test` previews the Black Flash

## Supported versions

| Minecraft | Folder |
|---|---|
| 1.21.11 | project root |
| 1.21.4 | `versions/1_21_4` |
| 26.2 | `versions/26_2` (needs Java 25+ and its own Gradle wrapper) |

Requires Fabric Loader and Fabric API.

## Building

```
./gradlew build
```

The jar is written to `build/libs`. For the other versions run the wrapper with `-p versions/<folder>` (26.2 uses the wrapper inside its own folder).
`gradle.properties` contains a local `org.gradle.java.home` path. Change or remove it to match your own JDK.

## Sounds
The sound files (Black Flash and death sound) are not part of this repository. Put your own `black_flash.ogg`, `black_flash.wav` and `unstable_death.ogg` into `src/main/resources/assets/maseffectsplus/sounds/` to get audio. Without them the mod still runs, only the sounds are missing.

## Notes

Fan-made visual mod. Not affiliated with or endorsed by Mojang, Microsoft or the creators of *Jujutsu Kaisen*.
Large parts of the code were written with the help of an AI coding assistant (Claude).

## License

MIT, see [LICENSE](LICENSE). Sound files may be subject to their own rights.
