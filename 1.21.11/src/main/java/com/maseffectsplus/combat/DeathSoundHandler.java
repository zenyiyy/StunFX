package com.maseffectsplus.combat;

import com.maseffectsplus.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;

/**
 * Unstable-SMP style death sound. It plays when a death message shows up in chat (heard no matter how far away
 * the death happened) and when a player dies near you (works on servers or bots without death messages).
 */
public class DeathSoundHandler {
    private static final SoundEvent DEATH_SOUND = SoundEvent.of(Identifier.of("maseffectsplus", "death.unstable"));
    private static long lastPlayed = 0L;

    public static void onGameMessage(Text message, boolean overlay) {
        try {
            if (overlay || message == null) return;
            if (!(message.getContent() instanceof TranslatableTextContent content)) return;
            if (!content.getKey().startsWith("death.")) return;
            play(false);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Plays the death sound if it is switched on. Several triggers for the same death (chat message + death event)
     * play it only once, unless {@code test} is true (used by /maseffects test).
     *
     * @return false if the sound is switched off
     */
    public static boolean play(boolean test) {
        try {
            ModConfig config = ModConfig.get();
            if (!config.deathSoundEnabled) return false;

            long now = System.currentTimeMillis();
            if (!test && now - lastPlayed < 1500L) return true;
            lastPlayed = now;

            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> client.getSoundManager().play(
                    PositionedSoundInstance.ui(DEATH_SOUND, 1.0f, Math.min(1.0f, config.deathSoundVolume))));
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }
}
