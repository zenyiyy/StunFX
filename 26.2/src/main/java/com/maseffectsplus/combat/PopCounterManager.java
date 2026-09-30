package com.maseffectsplus.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PopCounterManager {
    /** A player's count stays until they die, or until they haven't popped for this long. */
    private static final long IDLE_EXPIRE_MS = 5 * 60_000L;

    public static class PopEntry {
        public final String playerName;
        public int count;
        public long lastPopTime;

        public PopEntry(String playerName) {
            this.playerName = playerName;
            this.count = 1;
            this.lastPopTime = System.currentTimeMillis();
        }
    }

    private static final Map<String, PopEntry> POP_MAP = new LinkedHashMap<>();

    public static synchronized void recordPop(String playerName) {
        if (playerName == null || playerName.isEmpty()) return;
        PopEntry entry = POP_MAP.get(playerName);
        if (entry != null) {
            entry.count++;
            entry.lastPopTime = System.currentTimeMillis();
        } else {
            POP_MAP.put(playerName, new PopEntry(playerName));
        }
    }

    public static synchronized void resetPlayer(String playerName) {
        if (playerName != null) {
            POP_MAP.remove(playerName);
        }
    }

    public static synchronized void clearAll() {
        POP_MAP.clear();
    }

    /** Entries sorted by pop count (highest first), most recent pop first on ties. */
    public static synchronized List<PopEntry> getSortedEntries() {
        long now = System.currentTimeMillis();
        POP_MAP.entrySet().removeIf(e -> (now - e.getValue().lastPopTime) > IDLE_EXPIRE_MS);
        List<PopEntry> list = new ArrayList<>(POP_MAP.values());
        list.sort(Comparator.<PopEntry>comparingInt(e -> -e.count).thenComparingLong(e -> -e.lastPopTime));
        return list;
    }

    public static synchronized Map<String, PopEntry> getEntries() {
        long now = System.currentTimeMillis();
        POP_MAP.entrySet().removeIf(e -> (now - e.getValue().lastPopTime) > IDLE_EXPIRE_MS);
        return new LinkedHashMap<>(POP_MAP);
    }
}
