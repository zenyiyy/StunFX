package com.maseffectsplus;

import com.maseffectsplus.combat.CombatTracker;
import com.maseffectsplus.combat.ComboTracker;
import com.maseffectsplus.combat.DeathSoundHandler;
import com.maseffectsplus.combat.PopCounterManager;
import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.gui.MainConfigScreen;
import com.maseffectsplus.render.EffectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import com.maseffectsplus.hud.ComboHud;
import com.maseffectsplus.hud.PopCounterHud;
import com.maseffectsplus.render.EffectRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class MasEffectsClient implements ClientModInitializer {
    public static final String MOD_ID = "maseffectsplus";

    // Shown in Options > Controls > Key Binds under its own "MasEffects+" heading
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static KeyMapping openConfigKey;
    private static KeyMapping togglePopCounterKey;
    private static KeyMapping toggleDeathSoundKey;
    private static KeyMapping toggleStunslamModeKey;
    private static KeyMapping resetPopCounterKey;
    private static volatile boolean pendingOpenScreen = false;

    private static KeyMapping register(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(name, InputConstants.Type.KEYSYM, key, CATEGORY));
    }

    private static void actionBar(Minecraft client, String text) {
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal(text));
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static void togglePopCounter(Minecraft client) {
        ModConfig config = ModConfig.get();
        config.popCounterEnabled = !config.popCounterEnabled;
        ModConfig.save();
        actionBar(client, "Totem pop counter: " + onOff(config.popCounterEnabled));
    }

    // ---- /maseffects test ----

    private static final java.util.List<Object[]> DELAYED = new java.util.ArrayList<>(); // {ticksLeft, Runnable}

    private static void later(int ticks, Runnable task) {
        synchronized (DELAYED) {
            DELAYED.add(new Object[]{ticks, task});
        }
    }

    private static void runDelayedTasks() {
        java.util.List<Runnable> due = new java.util.ArrayList<>();
        synchronized (DELAYED) {
            java.util.Iterator<Object[]> it = DELAYED.iterator();
            while (it.hasNext()) {
                Object[] entry = it.next();
                int left = (Integer) entry[0] - 1;
                if (left <= 0) {
                    due.add((Runnable) entry[1]);
                    it.remove();
                } else {
                    entry[0] = left;
                }
            }
        }
        due.forEach(Runnable::run);
    }

    private static void testStunslam(Minecraft client) {
        ModConfig config = ModConfig.get();
        ComboTracker.onStunslam(); // so the combo counter can be tried out: run the test several times quickly
        EffectManager.spawnPreviewEffect(config.stunslam);
        if (config.stunslam.soundEnabled) {
            CombatTracker.playCustomSound(Identifier.fromNamespaceAndPath(MOD_ID, "stunslam.black_flash"),
                    config.stunslam.volume * 2.0f, config.stunslam.pitch);
        }
        actionBar(client, "Test: Stunslam");
    }

    private static void testRing(Minecraft client) {
        EffectManager.spawnPreviewEffect(ModConfig.get().bigDamage);
        actionBar(client, "Test: Mace ring");
    }

    private static void testTotem(Minecraft client) {
        EffectManager.spawnPreviewEffect(ModConfig.get().totemPop);
        actionBar(client, "Test: Totem pop");
    }

    private static void testKill(Minecraft client) {
        EffectManager.spawnPreviewEffect(ModConfig.get().kill);
        boolean on = DeathSoundHandler.play(true);
        actionBar(client, on ? "Test: Kill + death sound" : "Test: Kill (death sound is switched off)");
    }

    private static void testDeathSound(Minecraft client) {
        boolean on = DeathSoundHandler.play(true);
        actionBar(client, on ? "Test: Death sound" : "Death sound is switched off");
    }

    /** Runs every effect one after another so you can see and hear them all. */
    private static void testAll(Minecraft client) {
        actionBar(client, "Test: Stunslam, Mace ring, Totem pop, Kill");
        testStunslam(client);
        later(50, () -> testRing(client));
        later(90, () -> testTotem(client));
        later(130, () -> testKill(client));
    }

    private static void resetPopCounter(Minecraft client) {
        PopCounterManager.clearAll();
        actionBar(client, "Totem pop counter reset");
    }

    private static void toggleDeathSound(Minecraft client) {
        ModConfig config = ModConfig.get();
        config.deathSoundEnabled = !config.deathSoundEnabled;
        ModConfig.save();
        actionBar(client, "Death sound: " + onOff(config.deathSoundEnabled));
    }

    private static void toggleStunslamMode(Minecraft client) {
        ModConfig config = ModConfig.get();
        config.stunslamAnyWeapon = !config.stunslamAnyWeapon;
        ModConfig.save();
        actionBar(client, "Stunslam trigger: " + (config.stunslamAnyWeapon ? "Any weapon" : "Mace only"));
    }

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        new Thread(CombatTracker::preloadSound, "maseffects-sound-preload").start();

        // World rendering (effects) and HUD (pop counter, combo) through Fabric's events
        LevelRenderEvents.COLLECT_SUBMITS.register(EffectRenderer::submit);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "pop_counter"), new PopCounterHud());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "combo"), new ComboHud());

        // Unstable-SMP style death sound
        ClientReceiveMessageEvents.GAME.register(DeathSoundHandler::onGameMessage);

        // Fresh pop counter for every server / world
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PopCounterManager.clearAll();
            ComboTracker.reset();
        });

        // Key binds (Options > Controls > MasEffects+)
        openConfigKey = register("key.maseffectsplus.open_gui", GLFW.GLFW_KEY_RIGHT_SHIFT);
        togglePopCounterKey = register("key.maseffectsplus.toggle_pop_counter", GLFW.GLFW_KEY_UNKNOWN);
        toggleDeathSoundKey = register("key.maseffectsplus.toggle_death_sound", GLFW.GLFW_KEY_UNKNOWN);
        toggleStunslamModeKey = register("key.maseffectsplus.toggle_stunslam_mode", GLFW.GLFW_KEY_UNKNOWN);
        resetPopCounterKey = register("key.maseffectsplus.reset_pop_counter", GLFW.GLFW_KEY_UNKNOWN);

        // Commands: /stunfx (settings, test), /popcounter, /deathsound
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /stunfx, /maseffects and /macefx all do the same
            for (String name : new String[]{"stunfx", "maseffects", "macefx"}) {
            dispatcher.register(ClientCommands.literal(name)
                    .executes(context -> {
                        pendingOpenScreen = true;
                        return 1;
                    })
                    // /maseffects test           -> demo of everything, one after another
                    // /maseffects test <what>    -> stunslam | ring | totem | kill | sound
                    .then(ClientCommands.literal("test")
                            .executes(context -> {
                                testAll(Minecraft.getInstance());
                                return 1;
                            })
                            .then(ClientCommands.literal("stunslam").executes(context -> {
                                testStunslam(Minecraft.getInstance());
                                return 1;
                            }))
                            .then(ClientCommands.literal("ring").executes(context -> {
                                testRing(Minecraft.getInstance());
                                return 1;
                            }))
                            .then(ClientCommands.literal("totem").executes(context -> {
                                testTotem(Minecraft.getInstance());
                                return 1;
                            }))
                            .then(ClientCommands.literal("kill").executes(context -> {
                                testKill(Minecraft.getInstance());
                                return 1;
                            }))
                            .then(ClientCommands.literal("sound").executes(context -> {
                                testDeathSound(Minecraft.getInstance());
                                return 1;
                            }))));
            }

            dispatcher.register(ClientCommands.literal("popeffects")
                    .executes(context -> {
                        pendingOpenScreen = true;
                        return 1;
                    }));

            // /popcounter toggles the counter, /popcounter reset clears all counted pops
            dispatcher.register(ClientCommands.literal("popcounter")
                    .executes(context -> {
                        togglePopCounter(Minecraft.getInstance());
                        return 1;
                    })
                    .then(ClientCommands.literal("reset")
                            .executes(context -> {
                                resetPopCounter(Minecraft.getInstance());
                                return 1;
                            })));

            dispatcher.register(ClientCommands.literal("deathsound")
                    .executes(context -> {
                        toggleDeathSound(Minecraft.getInstance());
                        return 1;
                    }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingOpenScreen) {
                pendingOpenScreen = false;
                client.setScreenAndShow(new MainConfigScreen(client.gui.screen()));
            }

            while (openConfigKey.consumeClick()) {
                client.setScreenAndShow(new MainConfigScreen(client.gui.screen()));
            }
            while (togglePopCounterKey.consumeClick()) {
                togglePopCounter(client);
            }
            while (toggleDeathSoundKey.consumeClick()) {
                toggleDeathSound(client);
            }
            while (toggleStunslamModeKey.consumeClick()) {
                toggleStunslamMode(client);
            }
            while (resetPopCounterKey.consumeClick()) {
                resetPopCounter(client);
            }

            runDelayedTasks();
            ComboTracker.tick(); // a shield break without a stunslam in time ends the combo

            try {
                EffectManager.tick();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
