package pl.neontext.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pl.neontext.client.NeonTextClient;
import pl.neontext.client.cfg.NeonConfig;
import pl.neontext.client.core.NeonRuntime;
import pl.neontext.client.gui.widget.NeonButton;
import pl.neontext.client.gui.widget.NeonSlider;
import pl.neontext.client.gui.widget.NeonToggle;
import pl.neontext.client.gui.widget.TextLabel;

/**
 * Global settings: the master switch, the GUI animation switch, the global clock speed and config
 * maintenance (reload / reset). Everything writes straight through to the live config.
 */
public class GlobalPanel extends Panel {

    private int contentBottom;

    public GlobalPanel(Font font, int x, int y, int width, int height) {
        super(font, x, y, width, height, "Settings");
        rebuild();
    }

    private NeonConfig config() {
        return NeonRuntime.config();
    }

    private void changed() {
        NeonRuntime.save();
    }

    @Override
    public void build(int x, int y, int width) {
        int pad = 10;
        int w = width - pad * 2 - 8;
        int cx = x + pad;
        int cy = y + 34;

        // ---- master --------------------------------------------------------
        add(new NeonToggle(font, cx, cy, w, 26, "Enable NeonText",
                "Master switch for every animation in the mod",
                () -> config().enabled, on -> {
            config().enabled = on;
            changed();
        }));
        cy += 26 + 6;

        add(new NeonToggle(font, cx, cy, w, 26, "Animate GUI & HUD text",
                "Tooltips, buttons and screens - off keeps menus readable",
                () -> config().animateGui, on -> {
            config().animateGui = on;
            changed();
        }));
        cy += 26 + 6;

        add(new NeonToggle(font, cx, cy, w, 26, "Welcome message on join",
                "Print a one-line hello in chat the next time you join",
                () -> config().announce, on -> {
            config().announce = on;
            changed();
        }));
        cy += 26 + 10;

        // ---- clock ---------------------------------------------------------
        add(new NeonSlider(font, cx, cy, w, 24, "Global animation speed", 0.1, 3.0,
                config().timeScale, 2, "x",
                v -> {
                    config().timeScale = v.floatValue();
                    changed();
                },
                () -> config().timeScale));
        cy += 24 + 12;

        // ---- config --------------------------------------------------------
        addRenderOnly(new TextLabel(font, "CONFIG FILE", cx, cy).divider(true));
        cy += 12;
        final int pathY = cy;
        addRenderOnly((g, mx, my, pt) ->
                g.text(font, NeonGui.ellipsis(font, NeonConfig.defaultFile().toString(), w),
                        cx, pathY, NeonGui.TEXT_DIM));
        cy += 14;

        add(new NeonButton(font, cx, cy, (w - 6) / 2, 22, "Reload from disk", b -> {
            NeonRuntime.setConfig(NeonConfig.load(NeonConfig.defaultFile()));
            pl.neontext.client.holo.HologramManager.loadFrom(NeonRuntime.config());
            rebuild();
        }).accent(NeonGui.ACCENT));
        add(new NeonButton(font, cx + (w - 6) / 2 + 6, cy, (w - 6) / 2, 22, "Reset all styles", b -> {
            config().resetStyles();
            changed();
            rebuild();
        }).accent(NeonGui.RED));
        cy += 22 + 12;

        // ---- about ---------------------------------------------------------
        addRenderOnly(new TextLabel(font, "ABOUT", cx, cy).divider(true));
        cy += 12;
        String[] about = {
                "NeonText v" + NeonTextClient.VERSION + " \u2013 client-side animated text engine",
                "Minecraft 26.3 \u00B7 Fabric Loader \u00B7 Fabric API",
                "Targets: nameplates, tab list, chat, holograms, GUI & HUD",
                "Open this screen with K or /neon \u00B7 everything saves instantly",
        };
        for (String line : about) {
            final String text = line;
            final int rowY = cy;
            addRenderOnly((g, mx, my, pt) ->
                    g.text(font, NeonGui.ellipsis(font, text, w), cx, rowY, NeonGui.TEXT_FAINT));
            cy += 12;
        }
        cy += 8;

        contentBottom = cy - y;
    }

    @Override
    protected int contentHeight() {
        return contentBottom + 34;
    }

    @Override
    protected void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, "SETTINGS", getX() + 10, getY() + 10, NeonGui.ACCENT);
        g.text(font, "Global switches and config maintenance", getX() + 10, getY() + 21, NeonGui.TEXT_FAINT);
    }
}
