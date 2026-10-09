package pl.neontext.client.anim;

/**
 * Mutable per-glyph output produced by {@link EffectEngine} and consumed by the renderers.
 * One instance is reused per frame to keep the GC quiet (chat can be hundreds of glyphs).
 *
 * @param dx       horizontal offset in pixels (screen space) or world units
 * @param dy       vertical offset, positive = down
 * @param scale    uniform glyph scale, 1.0 = normal
 * @param rotation Z rotation in degrees, positive = clockwise
 * @param color    packed ARGB
 * @param shearX   horizontal skew, used by jelly/italic effects
 */
public record GlyphStyle(float dx, float dy, float scale, float rotation, int color, float shearX) {

    public static final GlyphStyle IDENTITY = new GlyphStyle(0.0f, 0.0f, 1.0f, 0.0f, 0xFFFFFFFF, 0.0f);

    public GlyphStyle withColor(int newColor) {
        return new GlyphStyle(dx, dy, scale, rotation, newColor, shearX);
    }

    public GlyphStyle withAlpha(float alpha) {
        return new GlyphStyle(dx, dy, scale, rotation, ColorUtil.multiplyAlpha(color, alpha), shearX);
    }

    /** Identity transform but keeping the colour. */
    public GlyphStyle flat() {
        return new GlyphStyle(0.0f, 0.0f, 1.0f, 0.0f, color, 0.0f);
    }

    public boolean isIdentity() {
        return dx == 0.0f && dy == 0.0f && scale == 1.0f && rotation == 0.0f && shearX == 0.0f;
    }
}
