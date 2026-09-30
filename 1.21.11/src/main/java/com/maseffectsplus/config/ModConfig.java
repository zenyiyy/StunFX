package com.maseffectsplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "maseffectsplus.json");

    private static ModConfig INSTANCE;

    public boolean effectsEnabled = true;
    public boolean onYourself = true;
    public boolean onPlayers = true;
    public boolean onMobs = true;
    public boolean popCounterEnabled = true;

    // Pop counter layout: corner 0 = top right, 1 = top left, 2 = bottom right, 3 = bottom left
    public int popCorner = 0;
    public float popScale = 1.0f;
    public boolean popCounterSound = false;
    public float popCounterSoundVolume = 0.8f;

    // Unstable-SMP style death sound (Wither spawn) on every death message
    public boolean deathSoundEnabled = true;
    public float deathSoundVolume = 1.0f;

    // Stunslam trigger: false = shield break + mace smash, true = shield break + a follow-up hit with any weapon
    public boolean stunslamAnyWeapon = false;

    public double range = 48.0;
    public int maxEffects = 32;

    // Effect specific configurations
    public EffectConfig stunslam = EffectConfig.createDefaultStunslam();
    public EffectConfig totemPop = EffectConfig.createDefaultTotemPop();
    public EffectConfig bigDamage = EffectConfig.createDefaultBigDamage();
    public EffectConfig kill = EffectConfig.createDefaultKill();

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE != null) {
                    INSTANCE.validate();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        INSTANCE = new ModConfig();
        save();
    }

    public static void save() {
        if (INSTANCE == null) return;
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void validate() {
        if (stunslam == null) stunslam = EffectConfig.createDefaultStunslam();
        // The settings pages depend on the effect's style; older configs could have another style saved
        stunslam.style = EffectStyle.BLACK_FLASH;
        bigDamage.style = EffectStyle.RING;
        totemPop.style = EffectStyle.SHOCKWAVE;
        kill.style = EffectStyle.DOME;
        popCorner = Math.max(0, Math.min(3, popCorner));
        if (popScale < 0.5f || popScale > 3.0f) popScale = 1.0f;
        if (totemPop == null) totemPop = EffectConfig.createDefaultTotemPop();
        if (bigDamage == null) bigDamage = EffectConfig.createDefaultBigDamage();
        if (kill == null) kill = EffectConfig.createDefaultKill();
    }
}
