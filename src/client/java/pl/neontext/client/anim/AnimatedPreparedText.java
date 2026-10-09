package pl.neontext.client.anim;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;

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

    /** The vanilla prepared text underneath - used to rewrap with a different target style. */
    public Font.PreparedText unwrap() {
        return delegate;
    }

    @Override
    public void visit(Font.GlyphVisitor visitor) {
        // one shared counter keeps the glyph index aligned with the character index: glyphs and
        // empty areas (spaces) advance it, decorative effects do not
        final int[] index = {0};
        delegate.visit(new Font.GlyphVisitor() {

            @Override
            public void acceptGlyph(TextRenderable.Styled glyph) {
                visitor.acceptGlyph(new AnimatedRenderable(glyph, style, index[0], total, seed, clock));
                index[0]++;
            }

            @Override
            public void acceptEffect(TextRenderable effect) {
                // underline/strikethrough stay vanilla - and so must the chat background plate,
                // which also arrives here and must never move or change colour
                visitor.acceptEffect(effect);
            }

            @Override
            public void acceptEmptyArea(EmptyArea empty) {
                visitor.acceptEmptyArea(empty);
                index[0]++;
            }

            @Override
            public void acceptRenderable(TextRenderable renderable) {
                if (renderable instanceof TextRenderable.Styled styled) {
                    acceptGlyph(styled);
                } else {
                    acceptEffect(renderable);
                }
            }
        });
    }
}
