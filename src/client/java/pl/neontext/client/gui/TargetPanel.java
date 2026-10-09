package pl.neontext.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.anim.Effect;
import pl.neontext.client.cfg.NeonConfig;
import pl.neontext.client.cfg.Presets;
import pl.neontext.client.core.NeonRuntime;
import pl.neontext.client.gui.widget.EffectPicker;
import pl.neontext.client.gui.widget.NeonButton;
import pl.neontext.client.gui.widget.NeonSlider;
import pl.neontext.client.gui.widget.NeonToggle;
import pl.neontext.client.gui.widget.PaletteEditor;
import pl.neontext.client.gui.widget.PreviewText;

import java.util.ArrayList;
import java.util.List;

/**
 * The main editor: one instance per animation target (nameplates, tab list, chat, holograms, GUI).
 * Every control reads and writes the live config and saves on change, so the game behind the screen
 * updates while you drag a slider.
 */
public class TargetPanel extends Panel {

    private final AnimTarget target;
    private PreviewText preview;
    private PaletteEditor palette;
    private EditBox hexField;
    private final List<NeonSlider> sliders = new ArrayList<>();
    private String sampleText = "";

    public TargetPanel(Font font, AnimTarget target, int x, int y, int width, int height) {
        super(font, x, y, width, height, target.displayName());
        this.target = target;
        this.sampleText = defaultSample(target);
        rebuild();
    }

    public AnimTarget target() {
        return target;
    }

    public static String defaultSample(AnimTarget target) {
        return switch (target) {
            case NAMEPLATE -> "[OWNER] Steve";
            case TAB -> "Steve";
            case CHAT -> "<Steve> neon text goes brrr";
            case HOLOGRAM -> "WELCOME TO THE SERVER";
            case GUI -> "NeonText";
            case OTHER -> "NeonText";
        };
    }

    private NeonConfig config() {
        return NeonRuntime.config();
    }

    private NeonConfig.TargetConfig tc() {
        return config().target(target);
    }

    private AnimStyle style() {
        return tc().style;
    }

    private void changed() {
        style().sanitize();
        NeonRuntime.save();
    }

    @Override
    public void build(int x, int y, int width) {
        sliders.clear();
        int pad = 10;
        int w = width - pad * 2 - 8; // leave room for the scrollbar
        int cx = x + pad;
        int cy = y + 34;

        // ---- live preview -------------------------------------------------
        preview = new PreviewText(font, cx, cy, w, this::style, () -> sampleText);
        preview.height(52).scale(1.7f).title("LIVE PREVIEW");
        addRenderOnly(preview);
        cy += 52 + 8;

        // ---- master switch for this target --------------------------------
        add(new NeonToggle(font, cx, cy, w, 26, "Animate " + target.displayName().toLowerCase(java.util.Locale.ROOT),
                target.description(), tc()::isEnabled, on -> {
            tc().enabled = on;
            changed();
        }));
        cy += 26 + 8;

        // ---- effect -------------------------------------------------------
        add(new EffectPicker(font, cx, cy, w, 30, () -> style().effect(), e -> {
            style().setEffect(e);
            style().presetName = "";
            changed();
            syncSliders();
        }));
        cy += 30 + 8;

        // ---- sliders ------------------------------------------------------
        cy = addSlider(cx, cy, w, "Speed", 0.0, 5.0, style().speed, 2, "x",
                v -> style().speed = (float) (double) v, () -> style().speed);
        cy = addSlider(cx, cy, w, "Amplitude", 0.0, 3.0, style().amplitude, 2, "",
                v -> style().amplitude = (float) (double) v, () -> style().amplitude);
        cy = addSlider(cx, cy, w, "Colour spread", 0.1, 4.0, style().spread, 2, "",
                v -> style().spread = (float) (double) v, () -> style().spread);
        cy = addSlider(cx, cy, w, "Size", 0.5, 2.5, style().scale, 2, "x",
                v -> style().scale = (float) (double) v, () -> style().scale);
        cy = addSlider(cx, cy, w, "Saturation", 0.0, 2.0, style().saturation, 2, "",
                v -> style().saturation = (float) (double) v, () -> style().saturation);
        cy = addSlider(cx, cy, w, "Brightness", 0.2, 2.0, style().brightness, 2, "",
                v -> style().brightness = (float) (double) v, () -> style().brightness);

        // ---- toggles ------------------------------------------------------
        add(new NeonToggle(font, cx, cy, w, 22, "Drop shadow", "The dark outline under each letter",
                () -> style().shadow, on -> {
            style().shadow = on;
            changed();
        }));
        cy += 22 + 4;
        add(new NeonToggle(font, cx, cy, w, 22, "Glow", "Extra bloom pass, looks great with neon colours",
                () -> style().glow, on -> {
            style().glow = on;
            changed();
        }));
        cy += 22 + 4;

        if (target == AnimTarget.NAMEPLATE || target == AnimTarget.TAB) {
            add(new NeonToggle(font, cx, cy, w, 22, "Only my own name",
                    "Leave other players vanilla - useful if a server dislikes animated names",
                    () -> style().onlyMe, on -> {
                style().onlyMe = on;
                changed();
            }));
            cy += 22 + 4;
        }

        // ---- palette ------------------------------------------------------
        cy += 4;
        palette = new PaletteEditor(font, cx, cy, w, 62, this::style, colors -> changed());
        palette.select(0);
        add(palette);
        cy += 62 + 6;

        hexField = new EditBox(font, cx, cy, w - 132, 20, net.minecraft.network.chat.Component.literal("Colour"));
        hexField.setValue(palette.selectedHex());
        hexField.setMaxLength(9);
        hexField.setResponder(value -> {
            palette.setSelectedHex(value);
            changed();
        });
        add(hexField);

        int bx = cx + w - 128;
        add(new NeonButton(font, bx, cy, 40, 20, "+", b -> {
            palette.addStop();
            hexField.setValue(palette.selectedHex());
            changed();
        }).accent(NeonGui.GREEN));
        add(new NeonButton(font, bx + 44, cy, 40, 20, "-", b -> {
            palette.removeStop();
            hexField.setValue(palette.selectedHex());
            changed();
        }).accent(NeonGui.RED));
        add(new NeonButton(font, bx + 88, cy, 40, 20, "RGB", b -> {
            palette.fillRainbow();
            hexField.setValue(palette.selectedHex());
            changed();
        }).accent(NeonGui.MAGENTA));
        cy += 20 + 10;

        // ---- sample text --------------------------------------------------
        add(new SectionLabel(font, cx, cy, w, "PREVIEW TEXT"));
        cy += 12;
        EditBox sample = new EditBox(font, cx, cy, w, 20,
                net.minecraft.network.chat.Component.literal("Sample"));
        sample.setValue(sampleText);
        sample.setMaxLength(64);
        sample.setResponder(value -> sampleText = value);
        add(sample);
        cy += 20 + 10;

        // ---- quick presets ------------------------------------------------
        add(new SectionLabel(font, cx, cy, w, "QUICK PRESETS"));
        cy += 12;
        int perRow = 3;
        int bw = (w - (perRow - 1) * 4) / perRow;
        int i = 0;
        for (Presets.Preset p : Presets.ALL) {
            int col = i % perRow;
            int row = i / perRow;
            int px = cx + col * (bw + 4);
            int py = cy + row * 22;
            add(new NeonButton(font, px, py, bw, 20, p.name(), b -> {
                config().applyPreset(target, p);
                changed();
                rebuild();
            }).accent(presetColor(p.tag())));
            i++;
        }
        cy += ((i + perRow - 1) / perRow) * 22 + 10;

        add(new NeonButton(font, cx, cy, w, 22, "Reset this target to defaults", b -> {
            NeonConfig fresh = new NeonConfig();
            tc().style = fresh.target(target).style;
            tc().enabled = fresh.target(target).enabled;
            changed();
            rebuild();
        }).accent(NeonGui.RED));
        cy += 22 + 12;

        contentBottom = cy - y;
    }

    private int contentBottom;

    private int presetColor(String tag) {
        return switch (tag) {
            case "Hype" -> NeonGui.MAGENTA;
            case "Neon" -> NeonGui.ACCENT;
            case "Chill" -> 0xFF7FE3C0;
            case "Rank" -> NeonGui.GOLD;
            default -> NeonGui.TEXT_DIM;
        };
    }

    private int addSlider(int x, int y, int width, String label, double min, double max, double value,
                          int decimals, String suffix, java.util.function.Consumer<Double> onChange,
                          java.util.function.DoubleSupplier external) {
        NeonSlider slider = new NeonSlider(font, x, y, width, 24, label, min, max, value, decimals, suffix,
                v -> {
                    onChange.accept(v);
                    changed();
                }, external);
        sliders.add(slider);
        add(slider);
        return y + 24 + 4;
    }

    /** Pulls slider positions back from the config after a preset or a reset replaced the style. */
    public void syncSliders() {
        for (NeonSlider s : sliders) {
            s.sync();
        }
        if (palette != null) {
            palette.select(palette.selected());
        }
        if (hexField != null) {
            hexField.setValue(palette == null ? "#FFFFFF" : palette.selectedHex());
        }
    }

    @Override
    protected int contentHeight() {
        return contentBottom + 34;
    }

    @Override
    protected void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, target.displayName().toUpperCase(java.util.Locale.ROOT), getX() + 10, getY() + 10, NeonGui.ACCENT);
        g.text(font, target.description(), getX() + 10, getY() + 21, NeonGui.TEXT_FAINT);
    }

    /** Keeps the palette hex field in step with the swatch the user clicked. */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        boolean handled = super.mouseClicked(event, doubled);
        if (palette != null && hexField != null && !hexField.isFocused()) {
            hexField.setValue(palette.selectedHex());
        }
        return handled;
    }

    /** Small non-interactive caption. */
    public static final class SectionLabel extends net.minecraft.client.gui.components.AbstractWidget {
        private final Font font;
        private final String text;

        public SectionLabel(Font font, int x, int y, int width, String text) {
            super(x, y, width, 10, net.minecraft.network.chat.Component.literal(text));
            this.font = font;
            this.text = text;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            g.text(font, text, getX(), getY(), NeonGui.TEXT_FAINT);
            g.fill(getX() + font.width(text) + 4, getY() + 4, getX() + getWidth(), getY() + 5, 0xFF1B2740);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
            return false;
        }
    }

    /** Exposed so the parent screen can nudge sliders after external config changes. */
    public void refresh() {
        syncSliders();
    }

    public Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
