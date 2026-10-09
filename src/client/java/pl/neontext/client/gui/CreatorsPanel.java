package pl.neontext.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.Effect;
import pl.neontext.client.gui.widget.PreviewText;
import pl.neontext.client.gui.widget.TextLabel;

/**
 * The creators page - a small animated hall of fame.
 *
 * <p>Every name is drawn through the real animation pipeline (a tagged {@link
 * pl.neontext.client.holo.NeonTextSequence}), so the credits show off the engine while telling you
 * who made it. Add or edit entries in {@link #CREATORS}.
 */
public class CreatorsPanel extends Panel {

    /** One credited person or group. */
    public record Creator(String name, String role, String handle, String blurb,
                          Effect effect, String[] colors, int accent) {

        AnimStyle style() {
            AnimStyle s = new AnimStyle();
            s.setEffect(effect);
            s.speed = 0.9f;
            s.amplitude = 0.9f;
            s.spread = 1.1f;
            s.glow = true;
            s.colors = colors.clone();
            return s;
        }
    }

    /** The credits list. Edit freely - the GUI reads this array. */
    public static final Creator[] CREATORS = {
            new Creator("Tymoteusz", "Owner & Lead Developer", "github.com/tymateuszwierzba-ux",
                    "The idea, the code and the whole neon engine behind NeonText.",
                    Effect.RAINBOW_BOUNCE,
                    new String[]{"#FF0040", "#FF8000", "#FFFF00", "#00FF90", "#00E5FF", "#FF3DCB"},
                    NeonGui.MAGENTA),

            new Creator("NeonText Team", "Effects & Design", "28 animations and counting",
                    "Every wave, glitch, jelly wobble, fire lick and glow in the mod.",
                    Effect.VAPOR,
                    new String[]{"#FF71CE", "#01CDFE", "#05FFA1", "#B967FF"},
                    NeonGui.ACCENT),

            new Creator("Fabric Community", "Modding Foundation", "fabricmc.net",
                    "Loom, Fabric API and the friendliest modding docs around.",
                    Effect.GLOW,
                    new String[]{"#00FFF2", "#00A2FF", "#9D00FF", "#FFFF00C8"},
                    NeonGui.GREEN),

            new Creator("You", "Player & Tester", "thanks for playing <3",
                    "Now go make that nick shine - the engine is all yours.",
                    Effect.GOLD,
                    new String[]{"#7A5A00", "#E0B100", "#FFF0A0"},
                    NeonGui.GOLD),
    };

    private static AnimStyle heroStyle() {
        AnimStyle s = new AnimStyle();
        s.setEffect(Effect.GRADIENT);
        s.speed = 0.4f;
        s.spread = 1.4f;
        s.brightness = 1.15f;
        s.glow = true;
        s.colors = new String[]{"#00E5FF", "#7A5CFF", "#FF3DCB", "#00E5FF"};
        return s;
    }

    private int contentBottom;

    public CreatorsPanel(Font font, int x, int y, int width, int height) {
        super(font, x, y, width, height, "Creators");
        rebuild();
    }

    @Override
    public void build(int x, int y, int width) {
        int pad = 10;
        int w = width - pad * 2 - 8;
        int cx = x + pad;
        int cy = y + 34;

        // ---- hero ----------------------------------------------------------
        PreviewText hero = new PreviewText(font, cx, cy, w, CreatorsPanel::heroStyle, () -> "NeonText");
        hero.height(54).scale(2.1f).title("CREATED BY");
        addRenderOnly(hero);
        cy += 54 + 6;

        final int taglineY = cy;
        addRenderOnly((g, mx, my, pt) ->
                g.centeredText(font, "Client-side animated text engine for Minecraft 26.3",
                        cx + w / 2, taglineY, NeonGui.TEXT_DIM));
        cy += 14;
        final int featureY = cy;
        addRenderOnly((g, mx, my, pt) ->
                g.centeredText(font, "nameplates \u00B7 tab list \u00B7 chat \u00B7 holograms \u00B7 GUI",
                        cx + w / 2, featureY, NeonGui.TEXT_FAINT));
        cy += 20;

        // ---- creator cards -------------------------------------------------
        addRenderOnly(new TextLabel(font, "THE CREW", cx, cy).divider(true));
        cy += 14;

        int cardH = 64;
        for (Creator creator : CREATORS) {
            drawCard(cx, cy, w, cardH, creator);
            cy += cardH + 8;
        }

        // ---- footer --------------------------------------------------------
        addRenderOnly(new TextLabel(font, "THANKS", cx, cy).divider(true));
        cy += 14;
        String[] thanks = {
                "Thanks for using NeonText!",
                "Report ideas and bugs on GitHub: github.com/tymateuszwierzba-ux/testv2",
                "Made with the same animation engine that powers this page.",
        };
        for (String line : thanks) {
            final String text = line;
            final int rowY = cy;
            addRenderOnly((g, mx, my, pt) ->
                    g.text(font, NeonGui.ellipsis(font, text, w), cx, rowY, NeonGui.TEXT_DIM));
            cy += 12;
        }
        cy += 6;

        contentBottom = cy - y;
    }

    private void drawCard(int x, int y, int width, int height, Creator creator) {
        AnimStyle style = creator.style();
        addRenderOnly((g, mx, my, pt) -> {
            boolean hover = mx >= x && mx < x + width && my >= y && my < y + height;
            NeonGui.card(g, x, y, width, height, hover ? NeonGui.SLOT_HOVER : NeonGui.PANEL_ALT);
            // accent rail + role
            g.fill(x, y, x + 3, y + height, creator.accent());
            NeonGui.drawPreview(g, font, creator.name(), style, x + 12, y + 8, 0xFFFFFF);
        });

        int tx = x + 12;
        String role = creator.role();
        String handle = creator.handle();
        String blurb = creator.blurb();
        int accent = creator.accent();
        addRenderOnly((g, mx, my, pt) -> {
            g.text(font, role, tx, y + 26, accent);
            g.text(font, handle, tx, y + 38, NeonGui.TEXT_DIM);
            g.text(font, NeonGui.ellipsis(font, blurb, width - 24), tx, y + 50, NeonGui.TEXT_FAINT);
        });
    }

    @Override
    protected int contentHeight() {
        return contentBottom + 34;
    }

    @Override
    protected void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, "CREATORS", getX() + 10, getY() + 10, NeonGui.MAGENTA);
        g.text(font, "The people behind the glow", getX() + 10, getY() + 21, NeonGui.TEXT_FAINT);
    }
}
