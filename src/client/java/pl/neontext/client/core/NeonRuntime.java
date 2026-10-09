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

    private NeonRuntime() {
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
