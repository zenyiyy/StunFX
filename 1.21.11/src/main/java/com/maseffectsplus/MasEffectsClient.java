package com.maseffectsplus;

import com.maseffectsplus.combat.CombatTracker;
import com.maseffectsplus.combat.ComboTracker;
import com.maseffectsplus.combat.DeathSoundHandler;
import com.maseffectsplus.combat.PopCounterManager;
import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.gui.MainConfigScreen;
import com.maseffectsplus.render.EffectManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MasEffectsClient implements ClientModInitializer {
    public static final String MOD_ID = "maseffectsplus";

    // Shown in Options > Controls > Key Binds under its own "MasEffects+" heading
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

    private static KeyBinding openConfigKey;
    private static KeyBinding togglePopCounterKey;
    private static KeyBinding toggleDeathSoundKey;
    private static KeyBinding toggleStunslamModeKey;
    private static KeyBinding resetPopCounterKey;
    private static volatile boolean pendingOpenScreen = false;

    private static KeyBinding register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, key, CATEGORY));
    }

    private static void actionBar(MinecraftClient client, String text) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(text), true);
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static void togglePopCounter(MinecraftClient client) {
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

    private static void testStunslam(MinecraftClient client) {
        ModConfig config = ModConfig.get();
        ComboTracker.onStunslam(); // so the combo counter can be tried out: run the test several times quickly
        EffectManager.spawnPreviewEffect(config.stunslam);
        if (config.stunslam.soundEnabled) {
            CombatTracker.playCustomSound(Identifier.of(MOD_ID, "stunslam.black_flash"),
                    config.stunslam.volume * 2.0f, config.stunslam.pitch);
        }
        actionBar(client, "Test: Stunslam");
    }

    private static void testRing(MinecraftClient client) {
        EffectManager.spawnPreviewEffect(ModConfig.get().bigDamage);
        actionBar(client, "Test: Mace ring");
    }

    private static void testTotem(MinecraftClient client) {
        EffectManager.spawnPreviewEffect(ModConfig.get().totemPop);
        actionBar(client, "Test: Totem pop");
    }

    private static void testKill(MinecraftClient client) {
        EffectManager.spawnPreviewEffect(ModConfig.get().kill);
        boolean on = DeathSoundHandler.play(true);
        actionBar(client, on ? "Test: Kill + death sound" : "Test: Kill (death sound is switched off)");
    }

    private static void testDeathSound(MinecraftClient client) {
        boolean on = DeathSoundHandler.play(true);
        actionBar(client, on ? "Test: Death sound" : "Death sound is switched off");
    }

    /** Runs every effect one after another so you can see and hear them all. */
    private static void testAll(MinecraftClient client) {
        actionBar(client, "Test: Stunslam, Mace ring, Totem pop, Kill");
        testStunslam(client);
        later(50, () -> testRing(client));
        later(90, () -> testTotem(client));
        later(130, () -> testKill(client));
    }

    private static void resetPopCounter(MinecraftClient client) {
        PopCounterManager.clearAll();
        actionBar(client, "Totem pop counter reset");
    }

    private static void toggleDeathSound(MinecraftClient client) {
        ModConfig config = ModConfig.get();
        config.deathSoundEnabled = !config.deathSoundEnabled;
        ModConfig.save();
        actionBar(client, "Death sound: " + onOff(config.deathSoundEnabled));
    }

    private static void toggleStunslamMode(MinecraftClient client) {
        ModConfig config = ModConfig.get();
        config.stunslamAnyWeapon = !config.stunslamAnyWeapon;
        ModConfig.save();
        actionBar(client, "Stunslam trigger: " + (config.stunslamAnyWeapon ? "Any weapon" : "Mace only"));
    }

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        new Thread(CombatTracker::preloadSound, "maseffects-sound-preload").start();

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
            dispatcher.register(ClientCommandManager.literal(name)
                    .executes(context -> {
                        pendingOpenScreen = true;
                        return 1;
                    })
                    // /maseffects test           -> demo of everything, one after another
                    // /maseffects test <what>    -> stunslam | ring | totem | kill | sound
                    .then(ClientCommandManager.literal("test")
                            .executes(context -> {
                                testAll(MinecraftClient.getInstance());
                                return 1;
                            })
                            .then(ClientCommandManager.literal("stunslam").executes(context -> {
                                testStunslam(MinecraftClient.getInstance());
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("ring").executes(context -> {
                                testRing(MinecraftClient.getInstance());
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("totem").executes(context -> {
                                testTotem(MinecraftClient.getInstance());
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("kill").executes(context -> {
                                testKill(MinecraftClient.getInstance());
                                return 1;
                            }))
                            .then(ClientCommandManager.literal("sound").executes(context -> {
                                testDeathSound(MinecraftClient.getInstance());
                                return 1;
                            }))));
            }

            dispatcher.register(ClientCommandManager.literal("popeffects")
                    .executes(context -> {
                        pendingOpenScreen = true;
                        return 1;
                    }));

            // /popcounter toggles the counter, /popcounter reset clears all counted pops
            dispatcher.register(ClientCommandManager.literal("popcounter")
                    .executes(context -> {
                        togglePopCounter(MinecraftClient.getInstance());
                        return 1;
                    })
                    .then(ClientCommandManager.literal("reset")
                            .executes(context -> {
                                resetPopCounter(MinecraftClient.getInstance());
                                return 1;
                            })));

            dispatcher.register(ClientCommandManager.literal("deathsound")
                    .executes(context -> {
                        toggleDeathSound(MinecraftClient.getInstance());
                        return 1;
                    }));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingOpenScreen) {
                pendingOpenScreen = false;
                client.setScreen(new MainConfigScreen(client.currentScreen));
            }

            while (openConfigKey.wasPressed()) {
                client.setScreen(new MainConfigScreen(client.currentScreen));
            }
            while (togglePopCounterKey.wasPressed()) {
                togglePopCounter(client);
            }
            while (toggleDeathSoundKey.wasPressed()) {
                toggleDeathSound(client);
            }
            while (toggleStunslamModeKey.wasPressed()) {
                toggleStunslamMode(client);
            }
            while (resetPopCounterKey.wasPressed()) {
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
