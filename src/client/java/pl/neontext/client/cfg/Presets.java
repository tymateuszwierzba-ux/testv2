package pl.neontext.client.cfg;

import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.Effect;
import pl.neontext.client.anim.Palette;

/**
 * One-click looks for the GUI preset browser. Each preset is a fully populated {@link AnimStyle}
 * so applying one never leaves a half-configured effect behind.
 */
public final class Presets {

    /** A preset is just a name, a description and a factory. */
    public record Preset(String name, String description, String tag, java.util.function.Supplier<AnimStyle> factory) {

        public AnimStyle create() {
            return factory.get().sanitize();
        }
    }

    private Presets() {
    }

    private static AnimStyle base(Effect effect, float speed, float amplitude, float spread, int[] palette) {
        AnimStyle s = new AnimStyle();
        s.setEffect(effect);
        s.speed = speed;
        s.amplitude = amplitude;
        s.spread = spread;
        s.setPalette(new Palette(palette));
        return s;
    }

    public static final Preset[] ALL = {
            new Preset("Rainbow Flex", "RGB cycle with a bounce - the classic client look",
                    "Hype", () -> base(Effect.RAINBOW_BOUNCE, 1.15f, 1.0f, 1.0f, Palette.RAINBOW)),

            new Preset("Neon Sign", "Cyan-magenta tube glow with a slow flicker",
                    "Neon", () -> {
                AnimStyle s = base(Effect.GLOW, 0.9f, 1.0f, 0.6f, Palette.NEON);
                s.glow = true;
                return s;
            }),

            new Preset("Vaporwave", "Pastel drift, slight squash, heavy aesthetics",
                    "Chill", () -> {
                AnimStyle s = base(Effect.VAPOR, 0.7f, 0.8f, 1.2f, Palette.VAPOR);
                s.glow = true;
                return s;
            }),

            new Preset("Datamosh", "Aggressive glitch with RGB splitting",
                    "Hype", () -> {
                AnimStyle s = base(Effect.GLITCH, 1.6f, 1.3f, 1.0f, Palette.NEON);
                s.shadow = false;
                return s;
            }),

            new Preset("Molten", "Burning text that licks upwards",
                    "Neon", () -> base(Effect.FIRE, 1.25f, 1.0f, 0.9f, Palette.FIRE)),

            new Preset("Frostbite", "Frozen shimmer, cool and calm",
                    "Chill", () -> base(Effect.ICE, 0.6f, 0.7f, 1.1f, Palette.ICE)),

            new Preset("Bullion", "Gold gradient with a travelling specular highlight",
                    "Clean", () -> base(Effect.GOLD, 0.8f, 0.6f, 1.0f, Palette.GOLD)),

            new Preset("Terminal", "Green phosphor rain, straight out of the Matrix",
                    "Neon", () -> {
                AnimStyle s = base(Effect.MATRIX, 1.1f, 1.0f, 1.0f, Palette.MATRIX);
                s.shadow = false;
                return s;
            }),

            new Preset("Aurora", "Very slow northern lights - subtle enough for survival",
                    "Clean", () -> base(Effect.AURORA, 0.45f, 0.5f, 1.4f, Palette.NEON)),

            new Preset("Heartbeat", "Lub-dub thump with a red flash",
                    "Hype", () -> base(Effect.HEARTBEAT, 1.0f, 1.2f, 0.4f, Palette.BLOOD)),

            new Preset("Candy Cane", "Marching pastel stripes",
                    "Clean", () -> base(Effect.CANDY, 0.9f, 0.6f, 1.0f, Palette.CANDY)),

            new Preset("Jelly", "Squash and stretch wobble",
                    "Hype", () -> base(Effect.JELLY, 1.3f, 1.0f, 1.0f, Palette.VAPOR)),

            new Preset("Decrypting", "Text scrambles and decodes on a loop",
                    "Neon", () -> base(Effect.SCRAMBLE, 0.9f, 0.9f, 0.8f, Palette.MATRIX)),

            new Preset("Bubbles", "Letters float up and pop",
                    "Chill", () -> base(Effect.BUBBLE, 0.8f, 1.0f, 1.0f, Palette.ICE)),

            new Preset("Storm", "Random lightning flashes over dark text",
                    "Hype", () -> base(Effect.LIGHTNING, 1.4f, 1.1f, 0.5f,
                    new int[]{0xFF2A3A6B, 0xFF7FB2FF, 0xFFFFFFFF})),

            new Preset("Stealth", "Barely-there white pulse. For when you want to be subtle.",
                    "Clean", () -> base(Effect.PULSE, 0.35f, 0.25f, 0.3f, Palette.MONO)),

            new Preset("Owner", "Big gold gradient, glow on - rank flex",
                    "Rank", () -> {
                AnimStyle s = base(Effect.GRADIENT, 0.55f, 0.6f, 1.0f, Palette.GOLD);
                s.scale = 1.15f;
                s.glow = true;
                return s;
            }),

            new Preset("Admin", "Cold blue wave, authoritative",
                    "Rank", () -> {
                AnimStyle s = base(Effect.WAVE, 0.8f, 0.8f, 1.0f, Palette.ICE);
                s.scale = 1.05f;
                return s;
            }),

            new Preset("VIP", "Candy stripes with a little hop",
                    "Rank", () -> {
                AnimStyle s = base(Effect.CANDY, 1.0f, 0.8f, 1.0f, Palette.CANDY);
                s.scale = 1.05f;
                return s;
            }),

            new Preset("YouTube", "Red-white-red strobe, loud on purpose",
                    "Rank", () -> base(Effect.STROBE, 0.8f, 0.5f, 1.0f,
                    new int[]{0xFFFF0000, 0xFFFFFFFF, 0xFFCC0000})),
    };

    public static Preset byName(String name) {
        for (Preset p : ALL) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public static String[] tags() {
        return new String[]{"All", "Hype", "Neon", "Chill", "Clean", "Rank"};
    }
}
