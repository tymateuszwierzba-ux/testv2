package pl.neontext.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import pl.neontext.client.anim.ColorUtil;
import pl.neontext.client.gui.NeonGui;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Labelled slider with a formatted read-out. Values are shown in the label so the user always sees
 * the exact number, which matters for things like speed where "a bit faster" is not good enough.
 */
public class NeonSlider extends AbstractSliderButton {

    private final Font font;
    private final String label;
    private final double min;
    private final double max;
    private final String suffix;
    private final int decimals;
    private final Consumer<Double> onChange;
    private final DoubleSupplier external;

    /**
     * @param value    current value, in the min..max range
     * @param external optional live value source, used to keep the slider in sync when the config is
     *                 changed from somewhere else (a preset, a command, the reset button)
     */
    public NeonSlider(Font font, int x, int y, int width, int height, String label,
                      double min, double max, double value, int decimals, String suffix,
                      Consumer<Double> onChange, DoubleSupplier external) {
        super(x, y, width, height, Component.literal(label), normalise(value, min, max));
        this.font = font;
        this.label = label;
        this.min = min;
        this.max = max;
        this.decimals = decimals;
        this.suffix = suffix == null ? "" : suffix;
        this.onChange = onChange;
        this.external = external;
        updateMessage();
    }

    private static double normalise(double value, double min, double max) {
        if (max <= min) {
            return 0.0;
        }
        return ColorUtil.clamp01((float) ((value - min) / (max - min)));
    }

    public double value() {
        return min + value * (max - min);
    }

    /** Pulls the value back from the config, so external changes are reflected without a rebuild. */
    public void sync() {
        if (external != null) {
            this.value = normalise(external.getAsDouble(), min, max);
            updateMessage();
        }
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + ": " + String.format(Locale.ROOT, "%." + decimals + "f", value()) + suffix));
    }

    @Override
    protected void applyValue() {
        if (onChange != null) {
            onChange.accept(value());
        }
    }

    // AbstractSliderButton widens this hook to public - the override must not narrow it
    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        g.fill(x, y, x + w, y + h, NeonGui.SLOT);
        g.fill(x, y, x + w, y + 1, 0xFF1B2740);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF1B2740);

        // track
        int trackY = y + h - 6;
        g.fill(x + 4, trackY, x + w - 4, trackY + 3, 0xFF1A2438);
        int filled = (int) ((w - 8) * value);
        g.fill(x + 4, trackY, x + 4 + filled, trackY + 3, isHoveredOrFocused() ? NeonGui.ACCENT : NeonGui.ACCENT_DIM);

        // handle
        int handleX = x + 4 + filled - 2;
        g.fill(handleX, y + 3, handleX + 4, y + h - 6, isHoveredOrFocused() ? NeonGui.TEXT : NeonGui.ACCENT);

        String text = NeonGui.ellipsis(font, getMessage().getString(), w - 8);
        g.centeredText(font, text, x + w / 2, y + 3, isHoveredOrFocused() ? NeonGui.TEXT : NeonGui.TEXT_DIM);
    }
}
