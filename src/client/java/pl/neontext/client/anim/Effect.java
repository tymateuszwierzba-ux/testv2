package pl.neontext.client.anim;

import java.util.Locale;

/**
 * Every animation NeonText can apply to text. An effect is a pure function of
 * {@code (glyphIndex, glyphCount, x, y, time)}; it never touches rendering itself.
 */
public enum Effect {

    NONE("None", "Static text, no animation"),
    RAINBOW("Rainbow", "Classic full-spectrum RGB cycle"),
    GRADIENT("Gradient", "Smooth blend across your palette"),
    WAVE("Wave", "A bright band sweeping through the text"),
    BOUNCE("Bounce", "Letters jump one after another"),
    PULSE("Pulse", "The whole string breathes in and out"),
    FLICKER("Flicker", "Broken neon sign"),
    GLITCH("Glitch", "RGB split and datamosh shake"),
    SLIDE("Slide", "Gentle side to side sway"),
    SPIN("Spin", "Each glyph rocks around its centre"),
    FADE("Fade", "Letters ghost in and out in sequence"),
    STROBE("Strobe", "Hard colour cuts, no blending"),
    STATIC("Static", "TV-snow colour noise"),
    FIRE("Fire", "Burning text with licks of flame"),
    ICE("Ice", "Frozen blue shimmer"),
    GOLD("Gold", "Bullion with a travelling highlight"),
    MATRIX("Matrix", "Green terminal phosphor rain"),
    VAPOR("Vaporwave", "A E S T H E T I C pastel drift"),
    GLOW("Glow", "Neon tube with a white hot core"),
    RAINBOW_BOUNCE("Rainbow Bounce", "Colour plus movement - the flex preset"),
    BUBBLE("Bubble", "Letters rise and pop"),
    SCRAMBLE("Scramble", "Text decodes itself on a loop"),
    HEARTBEAT("Heartbeat", "Lub-dub thump with a red flash"),
    CANDY("Candy", "Marching stripes"),
    INVERT("Invert", "Negative colours pulsing"),
    JELLY("Jelly", "Squash and stretch wobble"),
    LIGHTNING("Lightning", "Random storm flashes"),
    AURORA("Aurora", "Slow, calm northern lights");

    private final String displayName;
    private final String description;

    Effect(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Effect fromId(String id) {
        if (id == null) {
            return NONE;
        }
        try {
            return Effect.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    public Effect next() {
        Effect[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public Effect prev() {
        Effect[] all = values();
        return all[(ordinal() - 1 + all.length) % all.length];
    }
}
