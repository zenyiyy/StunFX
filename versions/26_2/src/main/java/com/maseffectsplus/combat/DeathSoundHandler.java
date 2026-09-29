package com.maseffectsplus.combat;

import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Unstable-SMP style death sound: whenever a death message shows up in chat, the death sound plays for
 * the player, no matter how far away the death happened.
 */
public class DeathSoundHandler {
    private static final SoundEvent DEATH_SOUND =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("maseffectsplus", "death.unstable"));
    private static long lastPlayed = 0L;

    public static void onGameMessage(Component message, boolean overlay) {
        try {
            if (overlay || message == null) return;
            if (!(message.getContents() instanceof TranslatableContents content)) return;
            if (!content.getKey().startsWith("death.")) return;

            ModConfig config = ModConfig.get();
            if (!config.effectsEnabled || !config.deathSoundEnabled) return;

            // several messages in the same instant (e.g. a team wipe) play the sound once
            long now = System.currentTimeMillis();
            if (now - lastPlayed < 1000L) return;
            lastPlayed = now;

            Minecraft client = Minecraft.getInstance();
            client.execute(() -> client.getSoundManager().play(
                    SimpleSoundInstance.forUI(DEATH_SOUND, 1.0f, config.deathSoundVolume)));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
