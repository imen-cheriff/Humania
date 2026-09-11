package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight event bus for UI components to notify changes.
 */
public class EventBus {

    private static final List<Runnable> candidatureListeners = new ArrayList<>();

    public static void addCandidatureListener(Runnable r) {
        if (r == null) return;
        candidatureListeners.add(r);
    }

    public static void removeCandidatureListener(Runnable r) {
        candidatureListeners.remove(r);
    }

    public static void fireCandidatureChanged() {
        for (Runnable r : new ArrayList<>(candidatureListeners)) {
            try { r.run(); } catch (Exception ignored) { }
        }
    }
}
