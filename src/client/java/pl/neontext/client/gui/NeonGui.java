package pl.neontext.client.gui;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.holo.NeonTextSequence;

/**
 * Theme constants and small drawing helpers shared by every NeonText screen.
 *
 * <p>The GUI is deliberately built from plain rectangles and text rather than vanilla sprites: it
 * keeps the dark-neon look consistent and avoids depending on sprite ids that move between versions.
 */
public final class NeonGui {

    // palette
    public static final int BG = 0xE8070B14;
    public static final int PANEL = 0xF2101828;
    public static final int PANEL_ALT = 0xF216203A;
    public static final int SIDEBAR = 0xF50B1120;
    public static final int OUTLINE = 0xFF2A3B63;
    public static final int ACCENT = 0xFF00E5FF;
    public static final int ACCENT_DIM = 0xFF0A6C86;
    public static final int MAGENTA = 0xFFFF3DCB;
    public static final int TEXT = 0xFFE8F1FF;
    public static final int TEXT_DIM = 0xFF8CA0C4;
    public static final int TEXT_FAINT = 0xFF5A6C8C;
    public static final int SLOT = 0xFF0A1220;
    public static final int SLOT_HOVER = 0xFF16233C;
    public static final int SLOT_ACTIVE = 0xFF10293F;
    public static final int GREEN = 0xFF3DFF9E;
    public static final int RED = 0xFFFF5C7A;
    public static final int GOLD = 0xFFFFC75A;

    // metrics
    public static final int SIDEBAR_WIDTH = 132;
    public static final int MARGIN = 10;
    public static final int ROW_HEIGHT = 20;
    public static final int ROW_GAP = 5;
    public static final int CONTROL_HEIGHT = 20;

    private NeonGui() {
    }

    // ------------------------------------------------------------- drawing

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, OUTLINE);
        g.fill(x, y + h - 1, x + w, y + h, OUTLINE);
        g.fill(x, y, x + 1, y + h, OUTLINE);
        g.fill(x + w - 1, y, x + w, y + h, OUTLINE);
    }

    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
        g.fill(x, y, x + w, y + 1, 0xFF223052);
    }

    /** Accent bar used to mark the selected sidebar entry. */
    public static void accentBar(GuiGraphicsExtractor g, int x, int y, int h) {
        g.fill(x, y, x + 2, y + h, ACCENT);
    }

    public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.text(font, s, x, y, color);
    }

    public static void centered(GuiGraphicsExtractor g, Font font, String s, int centerX, int y, int color) {
        g.centeredText(font, s, centerX, y, color);
    }

    public static void rightAligned(GuiGraphicsExtractor g, Font font, String s, int right, int y, int color) {
        g.text(font, s, right - font.width(s), y, color);
    }

    /** Truncates with an ellipsis so long values never spill out of a row. */
    public static String ellipsis(Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && font.width(cut + "..") > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    /**
     * Draws a sample of the text in the given style using the real animation pipeline, so what you
     * see in the GUI is exactly what you get in game.
     */
    public static void drawPreview(GuiGraphicsExtractor g, Font font, String sample, AnimStyle style,
                                   int x, int y, int color) {
        FormattedCharSequence plain = Component.literal(sample).getVisualOrderText();
        NeonTextSequence tagged = new NeonTextSequence(plain, style, sample.hashCode());
        g.text(font, tagged, x, y, color, style.shadow);
    }

    /** Same, but centred inside a box. */
    public static void drawPreviewCentered(GuiGraphicsExtractor g, Font font, String sample, AnimStyle style,
                                           int centerX, int y, int color) {
        FormattedCharSequence plain = Component.literal(sample).getVisualOrderText();
        NeonTextSequence tagged = new NeonTextSequence(plain, style, sample.hashCode());
        g.centeredText(font, tagged, centerX, y, color);
    }

    /**
     * Free-form text through the text collector, which is how a custom {@code Renderable} draws
     * labels in 26.3 - the old {@code GuiGraphics.drawString} is gone.
     */
    public static void collect(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color,
                               boolean shadow) {
        ActiveTextCollector collector = g.textRenderer();
        ActiveTextCollector.Parameters params = collector.defaultParameters();
        collector.accept(TextAlignment.LEFT, x, y, params, Component.literal(s));
    }

    /** A matrix that scales around a point, used by the preview boxes. */
    public static Matrix3x2f scaled(float scale, float pivotX, float pivotY) {
        Matrix3x2f m = new Matrix3x2f();
        m.translate(pivotX, pivotY);
        m.scale(scale, scale);
        m.translate(-pivotX, -pivotY);
        return m;
    }

    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }
}
