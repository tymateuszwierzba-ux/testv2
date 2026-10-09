package pl.neontext.client.holo;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import pl.neontext.client.anim.AnimStyle;

/**
 * A character sequence tagged with the animation style it must be drawn with.
 *
 * <p>The render pipeline recognises the instance when the text run is registered and uses
 * {@link #style()} instead of whatever the ambient target says. Holograms and the GUI live preview
 * both use this, which is why the preview shows the effect even while that effect is switched off.
 */
public class NeonTextSequence implements FormattedCharSequence {

    private final FormattedCharSequence delegate;
    private final AnimStyle style;
    private final int seed;

    public NeonTextSequence(FormattedCharSequence delegate, AnimStyle style, int seed) {
        this.delegate = delegate;
        this.style = style;
        this.seed = seed;
    }

    public AnimStyle style() {
        return style;
    }

    public int seed() {
        return seed;
    }

    public FormattedCharSequence delegate() {
        return delegate;
    }

    @Override
    public boolean accept(FormattedCharSink sink) {
        return delegate.accept(sink);
    }

    @Override
    public String toString() {
        return "NeonTextSequence";
    }
}
