package pl.neontext.client.anim;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/**
 * Decorates a vanilla {@link Font.PreparedText} so every glyph leaves {@link #visit} wrapped in an
 * {@link AnimatedRenderable}.
 *
 * <p>This is the single choke point of the whole mod. Both render pipelines in 26.3 draw text the
 * same way - prepare it with the font, then visit the glyphs - so wrapping the prepared text
 * animates nameplates, the tab list, chat, tooltips, screens and holograms with one hook.
 *
 * <p>{@code visit} runs once per frame while the prepared text itself can be cached across frames,
 * so all time dependent math lives here. Cached GUI text still animates smoothly.
 */
public final class AnimatedPreparedText implements Font.PreparedText {

    private final Font.PreparedText delegate;
    private final AnimStyle style;
    private final int seed;
    private final long timeMs;
    private final int total;
    /** Live animation clock - {@link #visit} runs every frame, so the time must not be frozen at build. */
    private final java.util.function.LongSupplier clock;

    public AnimatedPreparedText(Font.PreparedText delegate, AnimStyle style, int seed, long timeMs, int glyphCount) {
        this(delegate, style, seed, timeMs, glyphCount, () -> timeMs);
    }

    public AnimatedPreparedText(Font.PreparedText delegate, AnimStyle style, int seed, long timeMs, int glyphCount,
                                java.util.function.LongSupplier clock) {
        this.delegate = delegate;
        this.style = style;
        this.seed = seed;
        this.timeMs = timeMs;
        this.total = Math.max(1, glyphCount);
        this.clock = clock == null ? () -> timeMs : clock;
    }

    @Override
    public void visit(Font.GlyphVisitor visitor) {
        // one shared counter keeps the glyph index aligned with the character index, including
        // spaces (which arrive as empty areas) and underline/strikethrough effects
        final int[] index = {0};
        delegate.visit(new Font.GlyphVisitor() {

            @Override
            public void acceptGlyph(TextRenderable.Styled glyph) {
                visitor.acceptRenderable(wrap(glyph, index[0]++));
            }

            @Override
            public void acceptEffect(TextRenderable effect) {
                visitor.acceptRenderable(wrap(effect, index[0]++));
            }

            @Override
            public void acceptRenderable(TextRenderable renderable) {
                visitor.acceptRenderable(wrap(renderable, index[0]++));
            }

            @Override
            public void acceptEmptyArea(EmptyArea area) {
                index[0]++;
                visitor.acceptEmptyArea(area);
            }
        });
    }

    private TextRenderable wrap(TextRenderable renderable, int i) {
        GlyphStyle g = EffectEngine.compute(style, i, total, renderable.left(), renderable.top(),
                clock.getAsLong(), seed);
        return new AnimatedRenderable(renderable, g);
    }

    @Override
    public ScreenRectangle bounds() {
        return delegate.bounds();
    }

    public AnimStyle style() {
        return style;
    }
}
