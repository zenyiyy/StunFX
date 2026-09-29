package com.maseffectsplus;

import com.maseffectsplus.combat.CombatTracker;
import com.maseffectsplus.combat.DeathSoundHandler;
import com.maseffectsplus.combat.PopCounterManager;
import com.maseffectsplus.config.ModConfig;
import com.maseffectsplus.gui.MainConfigScreen;
import com.maseffectsplus.hud.PopCounterHud;
import com.maseffectsplus.render.EffectManager;
import com.maseffectsplus.render.EffectRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class MasEffectsClient implements ClientModInitializer {
    public static final String MOD_ID = "maseffectsplus";

    // Shown in Options > Controls > Key Binds under its own "MasEffects+" heading
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static KeyMapping openConfigKey;
    private static KeyMapping togglePopCounterKey;
    private static KeyMapping toggleDeathSoundKey;
    private static KeyMapping toggleStunslamModeKey;
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

        // World rendering (Black Flash, rings...) and HUD (pop counter)
        LevelRenderEvents.COLLECT_SUBMITS.register(EffectRenderer::submit);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "pop_counter"), new PopCounterHud());

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
            dispatcher.register(ClientCommands.literal("maseffects")
                    .executes(context -> {
                        pendingOpenScreen = true;
                        return 1;
                    })
                    .then(ClientCommands.literal("test")
                            .executes(context -> {
                                ModConfig config = ModConfig.get();
                                EffectManager.spawnPreviewEffect(config.stunslam);
                                CombatTracker.playCustomSound(
                                        Identifier.fromNamespaceAndPath(MOD_ID, "stunslam.black_flash"),
                                        config.stunslam.volume * 2.0f, config.stunslam.pitch);
                                return 1;
                            })));

            dispatcher.register(ClientCommands.literal("popeffects")
                    .executes(context -> {
                        pendingOpenScreen = true;
                        return 1;
                    }));

            dispatcher.register(ClientCommands.literal("popcounter")
                    .executes(context -> {
                        togglePopCounter(Minecraft.getInstance());
                        return 1;
                    }));

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

            try {
                EffectManager.tick();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
