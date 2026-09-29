package com.maseffectsplus.combat;

import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;

/**
 * Unstable-SMP style death sound: whenever a death message shows up in chat, the death sound plays for
 * the player, no matter how far away the death happened.
 */
public class DeathSoundHandler {
    private static final SoundEvent DEATH_SOUND = SoundEvent.of(Identifier.of("maseffectsplus", "death.unstable"));
    private static long lastPlayed = 0L;

    public static void onGameMessage(Text message, boolean overlay) {
        try {
            if (overlay || message == null) return;
            if (!(message.getContent() instanceof TranslatableTextContent content)) return;
            if (!content.getKey().startsWith("death.")) return;

            ModConfig config = ModConfig.get();
            if (!config.effectsEnabled || !config.deathSoundEnabled) return;

            // several messages in the same instant (e.g. a team wipe) play the sound once
            long now = System.currentTimeMillis();
            if (now - lastPlayed < 1000L) return;
            lastPlayed = now;

            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> client.getSoundManager().play(
                    PositionedSoundInstance.ui(DEATH_SOUND, 1.0f, config.deathSoundVolume)));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
