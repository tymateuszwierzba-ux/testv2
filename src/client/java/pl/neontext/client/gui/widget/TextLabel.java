package pl.neontext.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import pl.neontext.client.gui.NeonGui;

/**
 * A tiny non-interactive text label that panels can park in their render-only list. Used wherever a
 * caption belongs to the layout but not to the scrolling widget machinery.
 */
public class TextLabel implements Renderable {

    private final Font font;
    private final String text;
    private int x;
    private int y;
    private int color = NeonGui.TEXT_FAINT;
    private boolean divider;

    public TextLabel(Font font, String text, int x, int y) {
        this.font = font;
        this.text = text;
        this.x = x;
        this.y = y;
    }

    public TextLabel color(int color) {
        this.color = color;
        return this;
    }

    /** Draws a thin divider line from the end of the text to the label width. */
    public TextLabel divider(boolean divider) {
        this.divider = divider;
        return this;
    }

    public TextLabel at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.text(font, text, x, y, color);
        if (divider) {
            int start = x + font.width(text) + 4;
            g.fill(start, y + 4, start + 24, y + 5, 0xFF1B2740);
        }
    }
}
