package pl.neontext.client.anim;

import java.util.ArrayList;
import java.util.List;

/**
 * An ordered colour list. Effects sample it either as a hard index (strobe / candy) or as a
 * smooth gradient (gradient / fire / vaporwave).
 */
public final class Palette {

    public static final int[] RAINBOW = {
            0xFFFF0000, 0xFFFF8000, 0xFFFFFF00, 0xFF00FF00,
            0xFF00FFFF, 0xFF0040FF, 0xFF8000FF, 0xFFFF00FF
    };
    public static final int[] NEON = {0xFF00FFF2, 0xFF00A2FF, 0xFF9D00FF, 0xFFFF00C8};
    public static final int[] FIRE = {0xFFFF2D00, 0xFFFF7A00, 0xFFFFC400, 0xFFFFF2A8};
    public static final int[] ICE = {0xFF8FE3FF, 0xFF3FA9F5, 0xFF1B4FD8, 0xFFD6F5FF};
    public static final int[] GOLD = {0xFF7A5A00, 0xFFE0B100, 0xFFFFF0A0, 0xFFB98B00};
    public static final int[] MATRIX = {0xFF003B00, 0xFF00C853, 0xFF9CFFC0, 0xFF006400};
    public static final int[] VAPOR = {0xFFFF71CE, 0xFF01CDFE, 0xFF05FFA1, 0xFFB967FF, 0xFFFFFB96};
    public static final int[] CANDY = {0xFFFF4D6D, 0xFFFFFFFF, 0xFF4DC9FF, 0xFFFFFFFF};
    public static final int[] BLOOD = {0xFF3D0000, 0xFF8A0303, 0xFFD10000, 0xFFFF4D4D};
    public static final int[] MONO = {0xFFFFFFFF};

    private final int[] colors;

    public Palette(int[] colors) {
        this.colors = (colors == null || colors.length == 0) ? MONO.clone() : colors.clone();
    }

    public static Palette rainbow() {
        return new Palette(RAINBOW);
    }

    public static Palette of(String... hex) {
        List<Integer> out = new ArrayList<>();
        for (String s : hex) {
            out.add(ColorUtil.parseHex(s, 0xFFFFFFFF));
        }
        return new Palette(out.stream().mapToInt(Integer::intValue).toArray());
    }

    public int[] colors() {
        return colors;
    }

    public int size() {
        return colors.length;
    }

    /** Hard pick, index wraps. */
    public int at(int index) {
        return colors[Math.floorMod(index, colors.length)];
    }

    /**
     * Smooth sample. {@code t} wraps, so the gradient loops seamlessly instead of snapping when
     * the scroll reaches the end.
     */
    public int sample(float t) {
        if (colors.length == 1) {
            return colors[0];
        }
        float w = ColorUtil.wrap01(t) * colors.length;
        int i = (int) w;
        float f = w - i;
        return ColorUtil.lerp(colors[i % colors.length], colors[(i + 1) % colors.length], f);
    }

    public Palette copy() {
        return new Palette(colors);
    }

    public Palette saturated(float amount) {
        if (amount == 1.0f) {
            return this;
        }
        int[] out = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            out[i] = ColorUtil.saturate(colors[i], amount);
        }
        return new Palette(out);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Palette[");
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ColorUtil.toHex(colors[i]));
        }
        return sb.append(']').toString();
    }
}
