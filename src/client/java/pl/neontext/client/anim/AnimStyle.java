package pl.neontext.client.anim;

/**
 * Everything that defines how one piece of text animates. Plain mutable bean with no Minecraft
 * types, so it can be stored in JSON and edited from the GUI.
 *
 * <p>Values are unit-less: {@code speed} is roughly "cycles per second" and {@code amplitude} is
 * GUI pixels; world renderers scale it to their own coordinate system.
 */
public final class AnimStyle {

    /** Which effect to run. Stored as the enum name so the JSON stays readable. */
    public String effect = Effect.RAINBOW.name();

    /** Animation speed multiplier. 0 effectively freezes the effect. */
    public float speed = 1.0f;

    /** How far glyphs travel / how much they scale. 0 = colour only. */
    public float amplitude = 1.0f;

    /** Colour spread: how much of the palette fits inside the whole string. */
    public float spread = 1.0f;

    /** Saturation multiplier applied to the palette. */
    public float saturation = 1.0f;

    /** Brightness multiplier applied to the produced colour. */
    public float brightness = 1.0f;

    /** Uniform glyph scale, 1.0 = vanilla size. */
    public float scale = 1.0f;

    /** Draw the vanilla drop shadow under the text. */
    public boolean shadow = true;

    /** Draw an extra translucent copy behind the text to fake a neon bloom. */
    public boolean glow = false;

    /** Only animate the local player's own name (nameplates / tab list). */
    public boolean onlyMe = false;

    /** Colours as #RRGGBB strings so they stay hand-editable in the config file. */
    public String[] colors = defaultColors();

    /** Human readable preset name, shown in the GUI. Empty means "custom". */
    public transient String presetName = "";

    public static String[] defaultColors() {
        return new String[]{"#FF0000", "#FF8000", "#FFFF00", "#00FF00", "#00FFFF", "#0040FF", "#8000FF", "#FF00FF"};
    }

    public Effect effect() {
        return Effect.fromId(effect);
    }

    public void setEffect(Effect e) {
        this.effect = (e == null ? Effect.NONE : e).name();
    }

    public Palette palette() {
        if (colors == null || colors.length == 0) {
            return new Palette(new int[]{0xFFFFFFFF});
        }
        int[] parsed = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            parsed[i] = ColorUtil.parseHex(colors[i], 0xFFFFFFFF);
        }
        return new Palette(parsed);
    }

    public void setPalette(Palette p) {
        int[] c = p.colors();
        colors = new String[c.length];
        for (int i = 0; i < c.length; i++) {
            colors[i] = ColorUtil.toHex(c[i]);
        }
    }

    public boolean animates() {
        return effect() != Effect.NONE;
    }

    public AnimStyle copy() {
        AnimStyle s = new AnimStyle();
        s.effect = effect;
        s.speed = speed;
        s.amplitude = amplitude;
        s.spread = spread;
        s.saturation = saturation;
        s.brightness = brightness;
        s.scale = scale;
        s.shadow = shadow;
        s.glow = glow;
        s.onlyMe = onlyMe;
        s.presetName = presetName;
        s.colors = colors == null ? defaultColors() : colors.clone();
        return s;
    }

    /** Clamps every value so a hand-edited config can never crash the renderer. */
    public AnimStyle sanitize() {
        speed = ColorUtil.clamp(speed, 0.0f, 10.0f);
        amplitude = ColorUtil.clamp(amplitude, 0.0f, 5.0f);
        spread = ColorUtil.clamp(spread, 0.05f, 8.0f);
        saturation = ColorUtil.clamp(saturation, 0.0f, 3.0f);
        brightness = ColorUtil.clamp(brightness, 0.0f, 3.0f);
        scale = ColorUtil.clamp(scale, 0.25f, 4.0f);
        if (colors == null || colors.length == 0) {
            colors = defaultColors();
        }
        effect = effect().name();
        return this;
    }
}
