package pl.neontext.client.holo;

import pl.neontext.client.cfg.NeonConfig;
import pl.neontext.client.core.NeonContext;
import pl.neontext.client.core.NeonRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the live hologram list and the identity registrations that let the text pipeline recognise
 * hologram text.
 */
public final class HologramManager {

    private static final List<Hologram> LIVE = new ArrayList<>();

    private HologramManager() {
    }

    /** Re-syncs from the config, e.g. after a reload or an edit in the GUI. */
    public static void loadFrom(NeonConfig config) {
        NeonContext.clearHolograms();
        LIVE.clear();
        if (config == null) {
            return;
        }
        LIVE.addAll(config.holograms);
    }

    public static List<Hologram> all() {
        return LIVE;
    }

    public static void add(Hologram h) {
        if (NeonRuntime.config().hologram(h.id) == null) {
            NeonRuntime.config().holograms.add(h);
        }
        if (!LIVE.contains(h)) {
            LIVE.add(h);
        }
        NeonRuntime.save();
    }

    public static void remove(Hologram h) {
        LIVE.remove(h);
        NeonRuntime.config().holograms.remove(h);
        NeonRuntime.save();
    }

    /** Holograms that should be drawn for a camera at the given position. */
    public static List<Hologram> visibleFrom(double x, double y, double z) {
        List<Hologram> out = new ArrayList<>();
        for (Hologram h : LIVE) {
            if (h.visibleFrom(x, y, z)) {
                out.add(h);
            }
        }
        return out;
    }
}
