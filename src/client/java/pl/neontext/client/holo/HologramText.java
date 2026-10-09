package pl.neontext.client.holo;

import net.minecraft.util.FormattedCharSequence;

/**
 * The character sequence handed to the GUI pipeline for one hologram.
 *
 * <p>It exists purely as an identity token: {@link pl.neontext.client.core.NeonContext} recognises
 * the instance and therefore knows this text is a hologram, which hologram it is, and what style to
 * animate it with - even though it travels through exactly the same code path as a tooltip.
 */
public final class HologramText extends NeonTextSequence {

    private final Hologram hologram;

    public HologramText(FormattedCharSequence delegate, Hologram hologram) {
        super(delegate, hologram.style, hologram.seed());
        this.hologram = hologram;
    }

    public Hologram hologram() {
        return hologram;
    }

    /** Character count, so callers do not have to walk the sequence again. */
    public int length() {
        int[] n = {0};
        delegate().accept((index, style, cp) -> {
            n[0]++;
            return true;
        });
        return n[0];
    }

    @Override
    public String toString() {
        return "HologramText[" + hologram.id + "]";
    }
}
