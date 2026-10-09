package pl.neontext.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.gui.NeonGui;
import pl.neontext.client.holo.NeonTextSequence;

import java.util.function.Supplier;

/**
 * The live preview box.
 *
 * <p>It draws its sample through the exact same pipeline the game uses (a {@link NeonTextSequence}
 * handed to the GUI extractor), so the box shows the real effect with the real timing - not an
 * approximation - and it keeps animating even when that target is switched off.
 */
public class PreviewText implements Renderable {

    private final Font font;
    private final Supplier<AnimStyle> style;
    private final Supplier<String> sample;
    private int x, y, width, height = 46;
    private String title = "PREVIEW";
    private float scale = 1.6f;

    public PreviewText(Font font, int x, int y, int width, Supplier<AnimStyle> style, Supplier<String> sample) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.style = style;
        this.sample = sample;
    }

    public PreviewText height(int h) {
        this.height = h;
        return this;
    }

    public PreviewText scale(float s) {
        this.scale = s;
        return this;
    }

    public PreviewText title(String title) {
        this.title = title;
        return this;
    }

    public void setBounds(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        AnimStyle s = style.get();

        // frame
        g.fill(x, y, x + width, y + height, 0xFF070C16);
        g.fill(x, y, x + width, y + 1, NeonGui.OUTLINE);
        g.fill(x, y + height - 1, x + width, y + height, NeonGui.OUTLINE);
        g.fill(x, y, x + 1, y + height, NeonGui.OUTLINE);
        g.fill(x + width - 1, y, x + width, y + height, NeonGui.OUTLINE);

        g.text(font, title, x + 6, y + 4, NeonGui.TEXT_FAINT);
        g.text(font, s.effect().displayName(), x + width - 6 - font.width(s.effect().displayName()), y + 4,
                NeonGui.ACCENT_DIM);

        String text = sample.get();
        if (text == null || text.isEmpty()) {
            text = " ";
        }

        // fit the sample into the box, but never shrink below readable
        FormattedCharSequence plain = Component.literal(text).getVisualOrderText();
        float textWidth = font.width(plain) * scale;
        float fit = textWidth > width - 16 ? (width - 16) / Math.max(1.0f, font.width(plain)) : scale;
        float textHeight = font.lineHeight * fit;
        float tx = x + (width - font.width(plain) * fit) * 0.5f;
        float ty = y + 16 + (height - 20 - textHeight) * 0.5f;

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        try {
            pose.translate(tx, ty);
            pose.scale(fit, fit);
            g.centeredText(font, new NeonTextSequence(plain, s, text.hashCode()),
                    (int) (font.width(plain) * 0.5f), 0, 0xFFFFFF);
        } finally {
            pose.popMatrix();
        }
    }
}
