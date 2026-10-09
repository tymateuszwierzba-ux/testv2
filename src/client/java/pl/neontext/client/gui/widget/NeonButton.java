package pl.neontext.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import pl.neontext.client.gui.NeonGui;

import java.util.function.Consumer;

/**
 * Flat neon button. Extends the vanilla {@link AbstractButton} so focus, narration, sounds and the
 * new {@code KeyEvent}/{@code MouseButtonEvent} input plumbing all keep working, and only replaces
 * how it looks.
 */
public class NeonButton extends AbstractButton {

    private final Consumer<NeonButton> action;
    private final Font font;
    private int accent = NeonGui.ACCENT;
    private boolean selected;

    public NeonButton(Font font, int x, int y, int width, int height, String label, Consumer<NeonButton> action) {
        super(x, y, width, height, Component.literal(label));
        this.font = font;
        this.action = action;
    }

    public NeonButton accent(int color) {
        this.accent = color;
        return this;
    }

    public NeonButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public void setLabel(String label) {
        setMessage(Component.literal(label));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (action != null) {
            action.accept(this);
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHoveredOrFocused();
        int bg = !active ? NeonGui.SLOT
                : selected ? NeonGui.SLOT_ACTIVE
                : hover ? NeonGui.SLOT_HOVER : NeonGui.SLOT;
        g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);

        int border = selected ? accent : hover ? NeonGui.OUTLINE : 0xFF1B2740;
        g.fill(getX(), getY(), getX() + getWidth(), getY() + 1, border);
        g.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);
        g.fill(getX(), getY(), getX() + 1, getY() + getHeight(), border);
        g.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), border);

        if (selected) {
            g.fill(getX(), getY(), getX() + 2, getY() + getHeight(), accent);
        }

        int color = !active ? NeonGui.TEXT_FAINT : selected ? accent : hover ? NeonGui.TEXT : NeonGui.TEXT_DIM;
        String label = NeonGui.ellipsis(font, getMessage().getString(), getWidth() - 10);
        g.centeredText(font, label, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
