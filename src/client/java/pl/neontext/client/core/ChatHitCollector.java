package pl.neontext.client.core;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * Answers "is the cursor over any chat text?" using vanilla's own hit testing
 * ({@link ActiveTextCollector#findElementUnderCursor}), so animated/transformed glyphs count.
 * Used by the chat screen click hook that selects the chat target and swaps its preset.
 */
public final class ChatHitCollector implements ActiveTextCollector {

    private final Font font;
    private final int testX;
    private final int testY;
    private Parameters parameters = new Parameters(new org.joml.Matrix3x2f());
    private boolean hit;

    public ChatHitCollector(Font font, int testX, int testY) {
        this.font = font;
        this.testX = testX;
        this.testY = testY;
    }

    public boolean isHit() {
        return hit;
    }

    @Override
    public Parameters defaultParameters() {
        return parameters;
    }

    @Override
    public void defaultParameters(Parameters newParameters) {
        this.parameters = newParameters;
    }

    @Override
    public void accept(TextAlignment alignment, int anchorX, int y, Parameters parameters, net.minecraft.util.FormattedCharSequence text) {
        if (hit) {
            return;
        }
        int leftX = alignment.calculateLeft(anchorX, this.font, text);
        GuiTextRenderState state = new GuiTextRenderState(this.font, text, parameters.pose(), leftX, y,
                ARGB.white(parameters.opacity()), 0, true, true, parameters.scissor());
        ActiveTextCollector.findElementUnderCursor(state, this.testX, this.testY, style -> this.hit = true);
    }

    @Override
    public void acceptScrolling(Component message, int centerX, int left, int right, int top, int bottom, Parameters parameters) {
        defaultScrollingHelper(message, centerX, left, right, top, bottom, this.font.width(message),
                this.font.lineHeight, parameters);
    }
}
