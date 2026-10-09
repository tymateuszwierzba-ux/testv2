package pl.neontext.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import pl.neontext.client.anim.Effect;
import pl.neontext.client.gui.NeonGui;

import java.util.function.Consumer;

/**
 * Previous / next arrows around the current effect name, plus the effect's one-line description.
 * Clicking either half cycles through the whole {@link Effect} list, which is much faster than a
 * dropdown with 28 entries.
 */
public class EffectPicker extends AbstractWidget {

    private final Font font;
    private java.util.function.Supplier<Effect> getter;
    private final Consumer<Effect> setter;

    public EffectPicker(Font font, int x, int y, int width, int height,
                        java.util.function.Supplier<Effect> getter, Consumer<Effect> setter) {
        super(x, y, width, height, Component.literal("Effect"));
        this.font = font;
        this.getter = getter;
        this.setter = setter;
    }

    public Effect effect() {
        return getter.get();
    }

    private void cycle(int direction) {
        Effect current = getter.get();
        Effect next = direction > 0 ? current.next() : current.prev();
        setter.accept(next);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (!isActive() || !visible || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        int half = getX() + 74;
        cycle(event.x() < half ? -1 : 1);
        playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isActive() || !visible || !isFocused()) {
            return false;
        }
        if (event.key() == 263 || event.key() == 262) {
            cycle(event.key() == 263 ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        Effect effect = getter.get();
        boolean hover = isHoveredOrFocused();

        g.fill(x, y, x + w, y + h, hover ? NeonGui.SLOT_HOVER : NeonGui.PANEL_ALT);
        g.fill(x, y, x + w, y + 1, hover ? NeonGui.ACCENT_DIM : 0xFF1B2740);

        // left arrow zone
        g.fill(x, y, x + 74, y + h, mouseX >= x && mouseX < x + 74 && mouseY >= y && mouseY < y + h
                ? NeonGui.SLOT_ACTIVE : 0x00000000);
        g.fill(x + 74, y, x + w, y + h, mouseX >= x + 74 && mouseX < x + w && mouseY >= y && mouseY < y + h
                ? NeonGui.SLOT_ACTIVE : 0x00000000);

        g.text(font, "<", x + 8, y + (h - 8) / 2, hover ? NeonGui.ACCENT : NeonGui.TEXT_DIM);
        String name = effect.displayName();
        g.centeredText(font, name, x + 74 + (w - 74) / 2, y + 3, NeonGui.TEXT);
        g.centeredText(font, NeonGui.ellipsis(font, effect.description(), w - 90),
                x + 74 + (w - 74) / 2, y + h - 10, NeonGui.TEXT_FAINT);
        g.text(font, ">", x + w - 14, y + (h - 8) / 2, hover ? NeonGui.ACCENT : NeonGui.TEXT_DIM);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                Component.literal("Effect: " + getter.get().displayName()));
    }
}
