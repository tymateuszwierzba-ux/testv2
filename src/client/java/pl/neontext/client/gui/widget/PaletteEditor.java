package pl.neontext.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.ColorUtil;
import pl.neontext.client.gui.NeonGui;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Palette strip: one swatch per colour, click to select, then edit the hex in the field beside it.
 * Also supports adding and removing stops, and a rainbow quick-fill.
 */
public class PaletteEditor extends AbstractWidget {

    private static final int SWATCH = 18;
    private static final int GAP = 3;

    private final Font font;
    private final Supplier<AnimStyle> style;
    private final Consumer<String[]> onChange;
    private int selected;

    public PaletteEditor(Font font, int x, int y, int width, int height,
                         Supplier<AnimStyle> style, Consumer<String[]> onChange) {
        super(x, y, width, height, Component.literal("Palette"));
        this.font = font;
        this.style = style;
        this.onChange = onChange;
    }

    public int selected() {
        return selected;
    }

    public void select(int index) {
        String[] colors = style.get().colors;
        this.selected = colors == null || colors.length == 0 ? 0 : ColorUtil.clamp(index, 0, colors.length - 1);
    }

    public String selectedHex() {
        String[] colors = style.get().colors;
        return colors == null || colors.length == 0 ? "#FFFFFF" : colors[selected % colors.length];
    }

    public void setSelectedHex(String hex) {
        AnimStyle s = style.get();
        if (s.colors == null || s.colors.length == 0) {
            return;
        }
        String[] copy = s.colors.clone();
        copy[selected % copy.length] = hex.startsWith("#") ? hex : "#" + hex;
        s.colors = copy;
        onChange.accept(copy);
    }

    public void addStop() {
        AnimStyle s = style.get();
        String[] old = s.colors == null ? new String[0] : s.colors;
        if (old.length >= 12) {
            return;
        }
        String[] copy = new String[old.length + 1];
        System.arraycopy(old, 0, copy, 0, old.length);
        copy[old.length] = "#FFFFFF";
        s.colors = copy;
        selected = old.length;
        onChange.accept(copy);
    }

    public void removeStop() {
        AnimStyle s = style.get();
        String[] old = s.colors;
        if (old == null || old.length <= 1) {
            return;
        }
        String[] copy = new String[old.length - 1];
        int removed = selected % old.length;
        int k = 0;
        for (int i = 0; i < old.length; i++) {
            if (i != removed) {
                copy[k++] = old[i];
            }
        }
        s.colors = copy;
        selected = Math.min(removed, copy.length - 1);
        onChange.accept(copy);
    }

    public void fillRainbow() {
        AnimStyle s = style.get();
        s.colors = new String[]{"#FF0000", "#FF8000", "#FFFF00", "#00FF00", "#00FFFF", "#0040FF", "#8000FF", "#FF00FF"};
        selected = 0;
        onChange.accept(s.colors);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (!isActive() || !visible) {
            return false;
        }
        int index = swatchAt(event.x(), event.y());
        if (index < 0) {
            return false;
        }
        select(index);
        return true;
    }

    private int swatchAt(double mx, double my) {
        String[] colors = style.get().colors;
        if (colors == null) {
            return -1;
        }
        int rowY = getY() + 14;
        for (int i = 0; i < colors.length; i++) {
            int sx = getX() + 4 + i * (SWATCH + GAP);
            if (sx + SWATCH > getX() + getWidth()) {
                break;
            }
            if (mx >= sx && mx < sx + SWATCH && my >= rowY && my < rowY + SWATCH) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        AnimStyle s = style.get();
        String[] colors = s.colors == null ? new String[0] : s.colors;

        g.fill(x, y, x + w, y + h, NeonGui.PANEL_ALT);
        g.text(font, "PALETTE", x + 5, y + 3, NeonGui.TEXT_FAINT);

        int rowY = y + 14;
        for (int i = 0; i < colors.length; i++) {
            int sx = x + 4 + i * (SWATCH + GAP);
            if (sx + SWATCH > x + w - 4) {
                break;
            }
            int rgb = ColorUtil.parseHex(colors[i], 0xFFFFFFFF);
            g.fill(sx, rowY, sx + SWATCH, rowY + SWATCH, rgb | 0xFF000000);
            if (i == selected % Math.max(1, colors.length)) {
                g.fill(sx - 1, rowY - 1, sx + SWATCH + 1, rowY, NeonGui.TEXT);
                g.fill(sx - 1, rowY + SWATCH, sx + SWATCH + 1, rowY + SWATCH + 1, NeonGui.TEXT);
                g.fill(sx - 1, rowY, sx, rowY + SWATCH, NeonGui.TEXT);
                g.fill(sx + SWATCH, rowY, sx + SWATCH + 1, rowY + SWATCH, NeonGui.TEXT);
            }
        }

        // gradient preview of the whole palette
        int gy = rowY + SWATCH + 5;
        int gw = w - 8;
        for (int i = 0; i < gw; i++) {
            int c = s.palette().sample(i / (float) gw);
            g.fill(x + 4 + i, gy, x + 5 + i, gy + 4, c | 0xFF000000);
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                Component.literal("Palette, " + (style.get().colors == null ? 0 : style.get().colors.length) + " colours"));
    }
}
