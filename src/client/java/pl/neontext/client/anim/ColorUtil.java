package pl.neontext.client.anim;

/**
 * Colour helpers: HSV/RGB conversion, palette sampling and gradient blending.
 * Everything is static and allocation-free so it can run per glyph per frame.
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static int argb(float a, float r, float g, float b) {
        return ((int) (clamp01(a) * 255.0f) << 24)
                | ((int) (clamp01(r) * 255.0f) << 16)
                | ((int) (clamp01(g) * 255.0f) << 8)
                | ((int) (clamp01(b) * 255.0f));
    }

    public static int rgb(float r, float g, float b) {
        return argb(1.0f, r, g, b);
    }

    /** Hue is wrapped, so 1.7f and 0.7f are the same colour. */
    public static int hsv(float h, float s, float v) {
        return hsv(h, s, v, 1.0f);
    }

    public static int hsv(float h, float s, float v, float a) {
        h = wrap01(h);
        s = clamp01(s);
        v = clamp01(v);

        float i = (float) Math.floor(h * 6.0f);
        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);

        return switch (((int) i) % 6) {
            case 0 -> argb(a, v, t, p);
            case 1 -> argb(a, q, v, p);
            case 2 -> argb(a, p, v, t);
            case 3 -> argb(a, p, q, v);
            case 4 -> argb(a, t, p, v);
            default -> argb(a, v, p, q);
        };
    }

    public static float[] toHsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;

        float h;
        if (d == 0.0f) {
            h = 0.0f;
        } else if (max == r) {
            h = ((g - b) / d) % 6.0f;
        } else if (max == g) {
            h = ((b - r) / d) + 2.0f;
        } else {
            h = ((r - g) / d) + 4.0f;
        }
        h /= 6.0f;
        if (h < 0.0f) {
            h += 1.0f;
        }
        float s = max == 0.0f ? 0.0f : d / max;
        return new float[]{h, s, max};
    }

    public static int lerp(int a, int b, float t) {
        t = clamp01(t);
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24)
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | ((int) (ab + (bb - ab) * t));
    }

    public static int multiplyAlpha(int argb, float m) {
        int a = (argb >> 24) & 0xFF;
        int na = (int) (clamp01(a / 255.0f * clamp01(m)) * 255.0f);
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    public static int alpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    public static int withAlpha(int argb, int a) {
        return (clamp(a, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    public static int brighten(int argb, float amount) {
        float[] hsv = toHsv(argb);
        return hsv(hsv[0], hsv[1], Math.min(1.0f, hsv[2] + amount), ((argb >> 24) & 0xFF) / 255.0f);
    }

    public static int darken(int argb, float amount) {
        return brighten(argb, -amount);
    }

    /** Saturation boost used by the "vivid" slider. */
    public static int saturate(int argb, float s) {
        float[] hsv = toHsv(argb);
        return hsv(hsv[0], clamp01(hsv[1] * s), hsv[2], ((argb >> 24) & 0xFF) / 255.0f);
    }

    /** Deterministic hash noise in [0,1) - stable across frames for the same input. */
    public static float noise(int seed, int index) {
        int h = seed * 374761393 + index * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return (h & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    /** Same noise but re-rolled every {@code bucket} milliseconds, for flicker/glitch. */
    public static float noiseAt(int seed, int index, long timeMs, int bucketMs) {
        return noise(seed ^ (int) (timeMs / Math.max(1, bucketMs)), index);
    }

    public static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(v, 1.0f);
    }

    public static float wrap01(float v) {
        v = v % 1.0f;
        return v < 0.0f ? v + 1.0f : v;
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Smooth 0..1..0 triangle-ish wave, cheaper than sin and looks fine for sweeps. */
    public static float tri(float t) {
        float w = wrap01(t);
        return w < 0.5f ? w * 2.0f : (1.0f - w) * 2.0f;
    }

    /** Normalised sine in [0,1]. */
    public static float sin01(float t) {
        return 0.5f + 0.5f * (float) Math.sin(t * Math.PI * 2.0f);
    }

    /** Parses {@code #RRGGBB}, {@code RRGGBB} or {@code #RGB}. Returns fallback on error. */
    public static int parseHex(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        String t = s.trim();
        if (t.startsWith("#")) {
            t = t.substring(1);
        }
        try {
            if (t.length() == 3) {
                int r = Integer.parseInt(t.substring(0, 1), 16) * 17;
                int g = Integer.parseInt(t.substring(1, 2), 16) * 17;
                int b = Integer.parseInt(t.substring(2, 3), 16) * 17;
                return 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            if (t.length() == 6) {
                return 0xFF000000 | Integer.parseInt(t, 16);
            }
            if (t.length() == 8) {
                return (int) Long.parseLong(t, 16);
            }
        } catch (NumberFormatException ignored) {
            // fall through to fallback
        }
        return fallback;
    }

    public static String toHex(int argb) {
        return String.format("#%06X", argb & 0xFFFFFF);
    }

    public static String toHexA(int argb) {
        return String.format("#%08X", argb);
    }
}
