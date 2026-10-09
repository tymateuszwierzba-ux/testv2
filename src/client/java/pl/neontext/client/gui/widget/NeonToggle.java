package pl.neontext.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import pl.neontext.client.gui.NeonGui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Row with a label on the left and an ON/OFF pill on the right. Reads and writes through suppliers so
 * it always reflects the config, even when something else changed it.
 */
public class NeonToggle extends AbstractButton {

    private final Font font;
    private final String label;
    private final String hint;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public NeonToggle(Font font, int x, int y, int width, int height, String label, String hint,
                      BooleanSupplier getter, Consumer<Boolean> setter) {
        super(x, y, width, height, Component.literal(label));
        this.font = font;
        this.label = label;
        this.hint = hint;
        this.getter = getter;
        this.setter = setter;
    }

    public boolean value() {
        return getter.getAsBoolean();
    }

    @Override
    public void onPress(InputWithModifiers input) {
        setter.accept(!getter.getAsBoolean());
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean on = value();
        boolean hover = isHoveredOrFocused();

        g.fill(x, y, x + w, y + h, hover ? NeonGui.SLOT_HOVER : NeonGui.PANEL_ALT);

        g.text(font, label, x + 6, y + (h - 8) / 2 - (hint == null ? 0 : 1), on ? NeonGui.TEXT : NeonGui.TEXT_DIM);
        if (hint != null) {
            g.text(font, NeonGui.ellipsis(font, hint, w - 76), x + 6, y + h - 10, NeonGui.TEXT_FAINT);
        }

        int pillW = 30, pillH = 12;
        int px = x + w - pillW - 8;
        int py = y + (h - pillH) / 2;
        g.fill(px, py, px + pillW, py + pillH, on ? NeonGui.withAlpha(NeonGui.GREEN, 70) : 0xFF1A2438);
        int knob = on ? px + pillW - pillH : px;
        g.fill(knob, py, knob + pillH, py + pillH, on ? NeonGui.GREEN : NeonGui.TEXT_FAINT);
        g.fill(px, py, px + pillW, py + 1, on ? NeonGui.GREEN : 0xFF2A3B63);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
