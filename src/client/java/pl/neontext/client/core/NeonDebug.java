package pl.neontext.client.core;

/**
 * Cheap diagnostic counters so {@code /neon debug} can show what the render hooks actually did.
 * Every counter is a plain static long incremented in hot paths - no allocation, no locking.
 */
public final class NeonDebug {

    /** Font-level wraps (nameplates, world text, direct draws). */
    public static long fontWraps;
    /** GUI text runs wrapped through the stamped target. */
    public static long guiWraps;
    /** GUI text runs where the stamped target disagreed with the ambient one (the classic bug). */
    public static long stampOverrides;
    /** GUI text runs that got no style at all. */
    public static long guiSkipped;
    /** Glyphs handed to the animating renderer this frame. */
    public static volatile long glyphWrapsFrame;
    /** Quads repainted this frame. */
    public static volatile long recolorsFrame;
    /** Chat text runs matched to a stamped hologram this frame. */
    public static volatile long holoStampsFrame;
    /** Chat text runs matched to a stamped chat target this frame. */
    public static volatile long chatStampsFrame;
    public static volatile String lastText = "";
    public static volatile String lastResolved = "";

    private NeonDebug() {
    }

    /** Resets the per-frame counters. Called once per client tick. */
    public static void resetFrame() {
        glyphWrapsFrame = 0;
        recolorsFrame = 0;
        holoStampsFrame = 0;
        chatStampsFrame = 0;
    }

    public static void glyphWrap() {
        glyphWrapsFrame++;
    }

    public static void recolor() {
        recolorsFrame++;
    }
}
