package pl.neontext.client.holo;

import pl.neontext.client.anim.AnimStyle;

/**
 * A client-side text hologram. Nothing is sent to the server: the hologram only exists in this
 * client's config and is drawn by the world text pipeline, which means it inherits every NeonText
 * animation for free.
 */
public final class Hologram {

    public String id = "holo";
    public String text = "Hello world";
    public double x;
    public double y;
    public double z;
    /** 1.0 is roughly nameplate size. */
    public float scale = 1.0f;
    public boolean shadow = true;
    /** Semi-transparent black plate behind the text, 0 = off. */
    public int background = 0x90000000;
    /** Billboard towards the camera, otherwise the text keeps a fixed orientation. */
    public boolean billboard = true;
    /** Hide the hologram when the player is further away than this (blocks). */
    public int maxDistance = 64;
    public boolean enabled = true;
    public AnimStyle style = new AnimStyle();

    public Hologram() {
    }

    public Hologram(String id, String text, double x, double y, double z) {
        this.id = id;
        this.text = text;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Hologram copy() {
        Hologram h = new Hologram();
        h.id = id;
        h.text = text;
        h.x = x;
        h.y = y;
        h.z = z;
        h.scale = scale;
        h.shadow = shadow;
        h.background = background;
        h.billboard = billboard;
        h.maxDistance = maxDistance;
        h.enabled = enabled;
        h.style = style.copy();
        return h;
    }

    public boolean visibleFrom(double px, double py, double pz) {
        if (!enabled) {
            return false;
        }
        double dx = x - px, dy = y - py, dz = z - pz;
        return dx * dx + dy * dy + dz * dz <= (double) maxDistance * maxDistance;
    }

    public double distanceSq(double px, double py, double pz) {
        double dx = x - px, dy = y - py, dz = z - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    // --- screen-space cache, refreshed by the HUD renderer, used by click-to-select ---
    public transient boolean onScreen;
    public transient float screenLeft;
    public transient float screenTop;
    public transient float screenRight;
    public transient float screenBottom;

    /** Stable per-hologram seed so glitch/flicker patterns differ between holograms. */
    public int seed() {
        return id.hashCode();
    }
}
