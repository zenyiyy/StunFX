package com.maseffectsplus.combat;

import com.maseffectsplus.config.ModConfig;

/**
 * Counts stunslams in a row. The combo does NOT end with time: it only ends when a stunslam attempt fails
 * (you break a shield but the follow-up doesn't land in time / isn't a valid stunslam), when you die or leave.
 * The on-screen number is only shown for a few seconds after each stunslam (a setting), the count keeps going.
 */
public class ComboTracker {
    /** How long after breaking a shield the follow-up hit has to land (same as the stunslam window). */
    private static final long ATTEMPT_MS = 700L;

    private static int count = 0;
    private static long lastStunslam = 0L;
    private static long attemptDeadline = 0L; // 0 = no shield break of ours is waiting for its follow-up

    /** How long the number stays on screen after a stunslam. */
    public static long displayMs() {
        return ModConfig.get().comboWindowSeconds * 1000L;
    }

    public static synchronized void onStunslam() {
        count++;
        lastStunslam = System.currentTimeMillis();
        attemptDeadline = 0L;
    }

    /** You broke a shield: the stunslam has to follow within the attempt window, otherwise the combo is lost. */
    public static synchronized void onShieldBrokenByYou() {
        attemptDeadline = System.currentTimeMillis() + ATTEMPT_MS + 150L; // small grace for latency
    }

    /** True while a shield break of yours is waiting for its stunslam follow-up. */
    public static synchronized boolean hasAttempt() {
        return attemptDeadline != 0L;
    }

    /** The shield break we expected never happened: forget the attempt without ending the combo. */
    public static synchronized void cancelAttempt() {
        attemptDeadline = 0L;
    }

    /** A stunslam attempt failed (invalid follow-up hit or too late): the combo ends. */
    public static synchronized void fail() {
        count = 0;
        attemptDeadline = 0L;
    }

    /** Your own death or leaving the server. */
    public static synchronized void reset() {
        fail();
    }

    /** Called every client tick: a shield break without a follow-up in time fails the combo. */
    public static synchronized void tick() {
        if (attemptDeadline != 0L && System.currentTimeMillis() > attemptDeadline) {
            fail();
        }
    }

    public static synchronized int getCount() {
        return count;
    }

    /** Milliseconds since the last successful stunslam. */
    public static synchronized long getAgeMs() {
        return System.currentTimeMillis() - lastStunslam;
    }
}
