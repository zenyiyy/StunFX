package com.maseffectsplus;

import com.maseffectsplus.combat.CombatTracker;
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
    private static final String CATEGORY = "key.categories.maseffectsplus";

    private static KeyBinding openConfigKey;
    private static KeyBinding togglePopCounterKey;
    private static KeyBinding toggleDeathSoundKey;
    private static KeyBinding toggleStunslamModeKey;
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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PopCounterManager.clearAll());

        // Key binds (Options > Controls > MasEffects+)
        openConfigKey = register("key.maseffectsplus.open_gui", GLFW.GLFW_KEY_RIGHT_SHIFT);
        togglePopCounterKey = register("key.maseffectsplus.toggle_pop_counter", GLFW.GLFW_KEY_UNKNOWN);
        toggleDeathSoundKey = register("key.maseffectsplus.toggle_death_sound", GLFW.GLFW_KEY_UNKNOWN);
        toggleStunslamModeKey = register("key.maseffectsplus.toggle_stunslam_mode", GLFW.GLFW_KEY_UNKNOWN);

        // Commands: /maseffects, /maseffects test, /popeffects, /popcounter, /deathsound
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("maseffects")
                    .executes(context -> {
                        pendingOpenScreen = true;
                        return 1;
                    })
                    .then(ClientCommandManager.literal("test")
                            .executes(context -> {
                                MinecraftClient client = MinecraftClient.getInstance();
                                ModConfig config = ModConfig.get();
                                EffectManager.spawnPreviewEffect(config.stunslam);
                                CombatTracker.playCustomSound(
                                        Identifier.of(MOD_ID, "stunslam.black_flash"),
                                        config.stunslam.volume * 2.0f, config.stunslam.pitch);
                                return 1;
                            })));

            dispatcher.register(ClientCommandManager.literal("popeffects")
                    .executes(context -> {
                        pendingOpenScreen = true;
                        return 1;
                    }));

            dispatcher.register(ClientCommandManager.literal("popcounter")
                    .executes(context -> {
                        togglePopCounter(MinecraftClient.getInstance());
                        return 1;
                    }));

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

            try {
                EffectManager.tick();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
