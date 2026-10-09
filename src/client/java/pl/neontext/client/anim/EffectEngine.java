package pl.neontext.client.anim;

/**
 * The heart of the mod: turns {@code (glyphIndex, glyphCount, x, y, time)} into a
 * {@link GlyphStyle}. Pure math, no Minecraft classes, no allocation - so the same engine drives
 * nameplates, the tab list, chat, tooltips and world holograms.
 *
 * <p>Two conventions used everywhere below:
 * <ul>
 *   <li>{@code phase} advances one full cycle per second at {@code speed = 1}.</li>
 *   <li>movement values are in "GUI pixels"; world renderers multiply them by their own scale.</li>
 * </ul>
 */
public final class EffectEngine {

    /** Reused scratch object. Never cache it across calls. */
    private static final float GLYPH_H = 9.0f;

    private EffectEngine() {
    }

    /**
     * @param style  the configuration
     * @param index  0-based glyph index inside the string
     * @param count  total glyphs in the string (never 0 when called)
     * @param x      glyph left edge, used so waves travel along the text instead of along indices
     * @param y      glyph top edge
     * @param timeMs wall clock, animation is time based so it stays smooth at any FPS
     * @param seed   per-text seed, gives each player a different glitch pattern
     */
    public static GlyphStyle compute(AnimStyle style, int index, int count, float x, float y, long timeMs, int seed) {
        Effect effect = style.effect();
        if (effect == Effect.NONE) {
            return new GlyphStyle(0.0f, 0.0f, style.scale, 0.0f, 0xFFFFFFFF, 0.0f);
        }

        float speed = style.speed;
        float amp = style.amplitude;
        // phase in cycles; x/24 makes colour sweeps follow the text horizontally
        float phase = timeMs / 1000.0f * speed;
        float spread = style.spread;
        float posPhase = x / 24.0f * spread;
        float idxPhase = (count <= 1 ? 0.0f : index / (float) (count - 1)) * spread;
        Palette palette = style.palette().saturated(style.saturation);

        float dx = 0.0f;
        float dy = 0.0f;
        float scale = style.scale;
        float rot = 0.0f;
        float shear = 0.0f;
        int color = 0xFFFFFFFF;

        switch (effect) {
            case RAINBOW -> color = ColorUtil.hsv(phase * 0.25f + idxPhase * 0.6f, 1.0f, 1.0f);

            case GRADIENT -> color = palette.sample(phase * 0.15f + idxPhase);

            case WAVE -> {
                float w = ColorUtil.sin01(phase * 0.6f - idxPhase * 1.4f);
                color = ColorUtil.lerp(palette.at(0), palette.at(Math.min(1, palette.size() - 1)), w);
                dy = (w - 0.5f) * 2.2f * amp;
            }

            case BOUNCE -> {
                float t = ColorUtil.wrap01(phase * 0.9f - index * 0.09f);
                // asymmetric hop: fast up, slow down, flat for the second half
                float hop = t < 0.35f ? (float) Math.sin(t / 0.35f * Math.PI) : 0.0f;
                dy = -hop * 3.2f * amp;
                color = ColorUtil.lerp(palette.sample(idxPhase), 0xFFFFFFFF, hop * 0.55f);
                scale = style.scale * (1.0f + hop * 0.12f * amp);
            }

            case PULSE -> {
                float p = ColorUtil.sin01(phase * 1.1f);
                scale = style.scale * (1.0f + (p - 0.5f) * 0.30f * amp);
                color = ColorUtil.lerp(palette.sample(idxPhase * 0.5f), 0xFFFFFFFF, p * 0.35f);
            }

            case FLICKER -> {
                float n = ColorUtil.noiseAt(seed, index, timeMs, 90);
                float base = palette.sample(idxPhase * 0.4f + phase * 0.1f);
                // 25% of glyphs dim out at any moment, the rest stay lit
                color = n > 0.75f ? ColorUtil.multiplyAlpha(base, 0.15f + 0.2f * amp) : base;
            }

            case GLITCH -> {
                float n1 = ColorUtil.noiseAt(seed, index, timeMs, 70);
                float n2 = ColorUtil.noiseAt(seed + 991, index, timeMs, 50);
                boolean bad = n1 > 0.86f;
                dx = bad ? (n2 - 0.5f) * 4.0f * amp : (n2 - 0.5f) * 0.5f * amp;
                dy = bad ? (n1 - 0.5f) * 3.0f * amp : 0.0f;
                int c = palette.at((int) (n2 * palette.size()));
                // RGB split: push red and blue apart horizontally
                color = bad ? ColorUtil.lerp(c, 0xFF00FFFF, 0.5f) : c;
                shear = bad ? (n2 - 0.5f) * 0.35f * amp : 0.0f;
            }

            case SLIDE -> {
                float s = (float) Math.sin(phase * 2.0f + index * 0.35f);
                dx = s * 1.8f * amp;
                color = palette.sample(phase * 0.2f + idxPhase);
            }

            case SPIN -> {
                rot = (float) Math.sin(phase * 1.6f + index * 0.5f) * 22.0f * amp;
                color = palette.sample(phase * 0.25f + idxPhase);
            }

            case FADE -> {
                float a = ColorUtil.sin01(phase * 0.8f - index * 0.08f);
                color = ColorUtil.withAlpha(palette.sample(idxPhase * 0.5f), (int) (40 + 215 * a));
            }

            case STROBE -> {
                int step = (int) (phase * 6.0f + index * 0.5f);
                color = palette.at(step);
            }

            case STATIC -> {
                float n = ColorUtil.noiseAt(seed, index, timeMs, 60);
                color = ColorUtil.lerp(palette.at((int) (n * palette.size())), 0xFFFFFFFF, n * 0.4f);
                dy = (n - 0.5f) * 0.6f * amp;
            }

            case FIRE -> {
                float lick = ColorUtil.sin01(phase * 2.2f + index * 0.31f);
                color = palette.sample(0.15f + lick * 0.6f);
                dy = -lick * 1.3f * amp;
                scale = style.scale * (1.0f + lick * 0.06f * amp);
            }

            case ICE -> {
                float shimmer = ColorUtil.sin01(phase * 0.5f + idxPhase * 1.7f);
                color = ColorUtil.lerp(palette.sample(0.2f), 0xFFFFFFFF, shimmer * 0.55f);
            }

            case GOLD -> {
                // travelling specular highlight across a gold base
                float h = ColorUtil.tri(phase * 0.45f - idxPhase * 1.2f);
                color = ColorUtil.lerp(palette.sample(0.35f), 0xFFFFFBE0, (float) Math.pow(h, 3.0f));
            }

            case MATRIX -> {
                float fall = ColorUtil.wrap01(phase * 1.4f + index * 0.13f);
                float head = fall < 0.12f ? 1.0f : 0.0f;
                color = head > 0.5f ? 0xFFD6FFDD : palette.sample(0.25f + fall * 0.3f);
                color = ColorUtil.multiplyAlpha(color, 0.55f + 0.45f * (1.0f - fall));
            }

            case VAPOR -> {
                color = palette.sample(phase * 0.12f + idxPhase * 1.1f);
                float g = ColorUtil.sin01(phase * 0.4f + index * 0.2f);
                scale = style.scale * (1.0f + (g - 0.5f) * 0.08f * amp);
            }

            case GLOW -> {
                float p = ColorUtil.sin01(phase * 0.9f + idxPhase * 0.5f);
                color = ColorUtil.lerp(palette.sample(idxPhase * 0.3f), 0xFFFFFFFF, 0.35f + 0.4f * p);
            }

            case RAINBOW_BOUNCE -> {
                float t = ColorUtil.wrap01(phase * 1.0f - index * 0.08f);
                float hop = t < 0.4f ? (float) Math.sin(t / 0.4f * Math.PI) : 0.0f;
                dy = -hop * 3.6f * amp;
                color = ColorUtil.hsv(phase * 0.3f + idxPhase * 0.7f, 1.0f, 1.0f);
                scale = style.scale * (1.0f + hop * 0.15f * amp);
            }

            case BUBBLE -> {
                float t = ColorUtil.wrap01(phase * 0.7f + index * 0.11f);
                dy = -(t * 4.0f) * amp;
                float a = t < 0.75f ? 1.0f : 1.0f - (t - 0.75f) / 0.25f;
                color = ColorUtil.withAlpha(ColorUtil.lerp(palette.sample(idxPhase), 0xFFFFFFFF, t * 0.4f),
                        (int) (255 * a));
                scale = style.scale * (0.85f + t * 0.3f * amp);
            }

            case SCRAMBLE -> {
                // glyphs "decode" left to right on a loop
                float t = ColorUtil.wrap01(phase * 0.35f);
                float revealed = t * (count + 6);
                boolean settled = index < revealed;
                float n = ColorUtil.noiseAt(seed, index, timeMs, 80);
                color = settled ? palette.sample(idxPhase * 0.4f) : ColorUtil.lerp(0xFF00FF88, 0xFFFFFFFF, n);
                dy = settled ? 0.0f : (n - 0.5f) * 1.5f * amp;
            }

            case HEARTBEAT -> {
                // lub-dub: two quick beats then a pause
                float t = ColorUtil.wrap01(phase * 0.85f);
                float beat = (float) (Math.exp(-Math.pow((t - 0.10f) * 14.0f, 2.0f))
                        + 0.7f * Math.exp(-Math.pow((t - 0.30f) * 14.0f, 2.0f)));
                scale = style.scale * (1.0f + beat * 0.22f * amp);
                color = ColorUtil.lerp(palette.at(0), 0xFFFF3030, beat);
            }

            case CANDY -> {
                int step = (int) (phase * 3.0f);
                color = palette.at(index + step);
                dy = (float) Math.sin(phase * 3.0f + index) * 0.5f * amp;
            }

            case INVERT -> {
                float p = ColorUtil.sin01(phase * 0.7f + idxPhase * 0.3f);
                int base = palette.sample(idxPhase * 0.2f);
                color = p > 0.5f ? (0xFF000000 | (~base & 0xFFFFFF)) : base;
            }

            case JELLY -> {
                float t = phase * 2.4f + index * 0.42f;
                float sx = (float) Math.sin(t);
                float sy = (float) Math.cos(t * 1.3f);
                // squash and stretch: volume stays roughly constant
                scale = style.scale * (1.0f + sy * 0.16f * amp);
                shear = sx * 0.22f * amp;
                dy = sy * 0.9f * amp;
                color = palette.sample(phase * 0.18f + idxPhase);
            }

            case LIGHTNING -> {
                float n = ColorUtil.noiseAt(seed, 7, timeMs, 110);
                boolean strike = n > 0.90f;
                if (strike) {
                    float f = ColorUtil.noiseAt(seed, index, timeMs, 40);
                    color = f > 0.5f ? 0xFFFFFFFF : 0xFFBFD9FF;
                    dx = (f - 0.5f) * 1.5f * amp;
                } else {
                    color = ColorUtil.multiplyAlpha(palette.sample(idxPhase * 0.3f), 0.75f);
                }
            }

            case AURORA -> {
                float a = ColorUtil.sin01(phase * 0.18f + idxPhase * 0.9f);
                float b = ColorUtil.sin01(phase * 0.11f - idxPhase * 0.6f + 0.33f);
                color = ColorUtil.lerp(palette.sample(a), palette.sample(b + 0.5f), 0.5f);
                dy = (float) Math.sin(phase * 0.9f + index * 0.22f) * 0.5f * amp;
            }

            default -> color = 0xFFFFFFFF;
        }

        // global brightness, applied last so it affects every effect the same way
        if (style.brightness != 1.0f) {
            float[] hsv = ColorUtil.toHsv(color);
            color = ColorUtil.hsv(hsv[0], hsv[1], ColorUtil.clamp01(hsv[2] * style.brightness),
                    ((color >> 24) & 0xFF) / 255.0f);
        }

        return new GlyphStyle(dx, dy, scale, rot, color, shear);
    }

    /** Colour only, no transform - used where we can only change Style (e.g. components). */
    public static int computeColor(AnimStyle style, int index, int count, float x, long timeMs, int seed) {
        return compute(style, index, count, x, 0.0f, timeMs, seed).color();
    }

    /**
     * Builds a 2D affine transform that moves / scales / rotates / shears a glyph around its own
     * centre. The {@code base} matrix is not modified; the result is {@code base * anim}.
     */
    public static org.joml.Matrix3x2f transform2d(org.joml.Matrix3x2fc base, GlyphStyle g, float centerX, float centerY) {
        org.joml.Matrix3x2f m = new org.joml.Matrix3x2f(base);
        m.translate(centerX + g.dx(), centerY + g.dy());
        if (g.rotation() != 0.0f) {
            m.rotate((float) Math.toRadians(g.rotation()));
        }
        if (g.scale() != 1.0f) {
            m.scale(g.scale(), g.scale());
        }
        if (g.shearX() != 0.0f) {
            // column-major: (m00 m01 m10 m11 m20 m21) -> x' = x + shearX * y
            m.mul(new org.joml.Matrix3x2f(1.0f, 0.0f, g.shearX(), 1.0f, 0.0f, 0.0f));
        }
        m.translate(-centerX, -centerY);
        return m;
    }

    /**
     * The scramble effect needs to swap the actual character. Deterministic so it does not
     * strobe between the prepare and render passes of the same frame.
     */
    public static int scrambleCodepoint(int original, int index, long timeMs, int seed) {
        if (Character.isWhitespace(original)) {
            return original;
        }
        float n = ColorUtil.noiseAt(seed, index, timeMs, 55);
        char[] pool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#$%&@!?*".toCharArray();
        return pool[(int) (n * (pool.length - 1))];
    }
}
