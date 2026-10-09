package pl.neontext.client.core;

import net.minecraft.client.Minecraft;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.cfg.NeonConfig;

/**
 * Holds the live config and answers the two questions every render hook asks:
 * "should this text animate?" and "with what style?".
 */
public final class NeonRuntime {

    private static NeonConfig config = new NeonConfig();

    /** Monotonic animation clock in milliseconds, scaled by the global time scale. */
    private static long animTimeMs;
    private static long lastRealMs;

    // --- command/click selection: which thing the edit commands act on ---
    /** Selected regular target (nameplate/tab/chat/...), or null. */
    public static AnimTarget selectedTarget;
    /** Selected hologram id, or null. Takes precedence over {@link #selectedTarget}. */
    public static String selectedHolo;
    /** Click a hologram / chat text to select it and swap its preset. */
    public static boolean clickSelect = true;
    /** Where {@code /neon next} continues for the current selection. */
    static int lastPresetIndex = -1;

    private NeonRuntime() {
    }

    public static void select(AnimTarget target) {
        selectedTarget = target;
        selectedHolo = null;
        lastPresetIndex = -1;
    }

    public static void selectHolo(String id) {
        selectedHolo = id;
        selectedTarget = null;
        lastPresetIndex = -1;
    }

    public static void selectNone() {
        selectedTarget = null;
        selectedHolo = null;
        lastPresetIndex = -1;
    }

    public static boolean hasSelection() {
        return selectedTarget != null || selectedHolo != null;
    }

    public static NeonConfig config() {
        return config;
    }

    public static void setConfig(NeonConfig cfg) {
        config = cfg == null ? new NeonConfig() : cfg;
    }

    /** Called once per client tick so the animation clock follows timeScale without drifting. */
    public static void tick() {
        long now = System.currentTimeMillis();
        if (lastRealMs == 0L) {
            lastRealMs = now;
        }
        long delta = Math.min(250L, now - lastRealMs);
        lastRealMs = now;
        animTimeMs += (long) (delta * config.timeScale);
    }

    public static long time() {
        return animTimeMs;
    }

    public static boolean masterEnabled() {
        return config.enabled;
    }

    /** True when text of this target should be animated right now. */
    public static boolean isActive(AnimTarget target) {
        if (!config.enabled) {
            return false;
        }
        if (target == AnimTarget.GUI && !config.animateGui) {
            return false;
        }
        NeonConfig.TargetConfig tc = config.target(target);
        return tc.enabled && tc.style.animates();
    }

    public static AnimStyle style(AnimTarget target) {
        return config.style(target);
    }

    /** Local player name, or null when not in a world yet. Used by the "only me" option. */
    public static String localPlayerName() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.player != null ? mc.player.getName().getString() : null;
    }

    public static void save() {
        config.save();
    }
}
