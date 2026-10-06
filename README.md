# Stun FX

Client-side mace PvP effects for Fabric: a **Black Flash** on stunslams, a combo counter, shockwave rings on mace smashes, a totem pop counter and a death sound. Everything is configurable in one compact in-game screen.

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

## Notes

- Needs Fabric Loader and Fabric API. Works on 1.21.11, 1.21.4 and 26.2. The 1.21.4 and 26.2 builds are not tested in game yet, please report problems.
- The mod only changes what you see and hear on your own screen. It sends nothing to servers.
- Low impact on FPS.

Source code and issues: https://github.com/zenyiyy/StunFX

Fan-made. Not affiliated with or endorsed by Mojang, Microsoft or the creators of Jujutsu Kaisen.
