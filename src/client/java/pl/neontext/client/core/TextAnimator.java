package pl.neontext.client.core;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimatedPreparedText;

import java.util.ArrayList;
import java.util.List;

/**
 * The one place where the mod turns Minecraft text into animated text.
 *
 * <p>Everything routes through {@link AnimatedPreparedText}: the GUI pipeline and the world
 * pipeline both render by asking the font to prepare a sequence and then visiting the glyphs, so a
 * single wrapper at that seam animates nameplates, the tab list, chat, tooltips and holograms.
 */
public final class TextAnimator {

    private TextAnimator() {
    }

    /**
     * @return an animated replacement for {@code prepared}, or null when the text must stay vanilla
     */
    public static Font.PreparedText animate(Font.PreparedText prepared, FormattedCharSequence source,
                                            AnimStyle style, int seed) {
        if (prepared == null || style == null || !style.animates() || !NeonRuntime.masterEnabled()) {
            return null;
        }
        int glyphs = count(source);
        return new AnimatedPreparedText(prepared, style, seed, NeonRuntime.time(), glyphs);
    }

    /** Character count, used as the "total" for normalised effects like the fade sweep. */
    public static int count(FormattedCharSequence sequence) {
        if (sequence == null) {
            return 0;
        }
        int[] n = {0};
        sequence.accept((index, style, cp) -> {
            n[0]++;
            return true;
        });
        return n[0];
    }

    /** Drains a sequence into codepoints, used to give each text a stable per-string seed. */
    public static int[] codepoints(FormattedCharSequence sequence) {
        if (sequence == null) {
            return new int[0];
        }
        List<Integer> out = new ArrayList<>();
        sequence.accept((index, style, cp) -> {
            out.add(cp);
            return true;
        });
        int[] result = new int[out.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = out.get(i);
        }
        return result;
    }

    /** Cheap, stable seed from the text itself so two players never glitch identically. */
    public static int seedOf(FormattedCharSequence sequence) {
        int[] cps = codepoints(sequence);
        int h = cps.length;
        for (int cp : cps) {
            h = h * 31 + cp;
        }
        return h;
    }

    /** A sequence that only exists so we can recognise our own holograms by identity. */
    public static final class MarkerSequence implements FormattedCharSequence {

        private final FormattedCharSequence delegate;

        public MarkerSequence(FormattedCharSequence delegate) {
            this.delegate = delegate;
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
            return "NeonHologramText";
        }
    }
}
