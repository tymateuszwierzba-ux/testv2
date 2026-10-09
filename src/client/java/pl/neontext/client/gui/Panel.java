package pl.neontext.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrollable, self-drawing section of the NeonText GUI.
 *
 * <p>Extending {@link AbstractWidget} gives us position, visibility and the new
 * {@code extractWidgetRenderState} hook; implementing {@link ContainerEventHandler} gives us the
 * vanilla child dispatch for the new {@code MouseButtonEvent}/{@code KeyEvent} input types, so every
 * widget inside behaves exactly like a vanilla one (focus, tab order, drag, typing).
 *
 * <p>Scrolling is done by translating the pose and scissoring to the panel rectangle, which means
 * children keep their logical coordinates and hit-testing stays correct for free.
 */
public abstract class Panel extends AbstractWidget implements ContainerEventHandler {

    protected final Font font;
    protected final List<AbstractWidget> widgets = new ArrayList<>();
    protected final List<Renderable> renderOnly = new ArrayList<>();

    private double scroll;
    private GuiEventListener focused;
    private boolean dragging;

    protected Panel(Font font, int x, int y, int width, int height, String title) {
        super(x, y, width, height, Component.literal(title));
        this.font = font;
    }

    /** Builds (or rebuilds) the widget list. Called on open and whenever the section changes. */
    public abstract void build(int x, int y, int width);

    /** Height of everything inside, used to decide whether a scrollbar is needed. */
    protected abstract int contentHeight();

    public void rebuild() {
        widgets.clear();
        renderOnly.clear();
        build(getX(), getY(), getWidth());
        clampScroll();
    }

    public void reposition(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        rebuild();
    }

    // ------------------------------------------------------------ children

    protected <T extends AbstractWidget & GuiEventListener & NarratableEntry> T add(T widget) {
        widgets.add(widget);
        return widget;
    }

    protected <T extends Renderable> T addRenderOnly(T renderable) {
        renderOnly.add(renderable);
        return renderable;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return widgets;
    }

    @Override
    public GuiEventListener getFocused() {
        return focused;
    }

    @Override
    public void setFocused(GuiEventListener listener) {
        this.focused = listener;
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    // -------------------------------------------------------------- render

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        NeonGui.panel(g, getX(), getY(), getWidth(), getHeight());

        int content = contentHeight();
        int maxScroll = Math.max(0, content - (getHeight() - 10));
        g.enableScissor(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1);
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        try {
            pose.translate(0, (float) -scroll);
            drawHeader(g, mouseX, mouseY);
            for (Renderable r : renderOnly) {
                r.extractRenderState(g, mouseX, mouseY + (int) scroll, partialTick);
            }
            for (AbstractWidget w : widgets) {
                if (w.visible) {
                    w.extractRenderState(g, mouseX, mouseY + (int) scroll, partialTick);
                }
            }
        } finally {
            pose.popMatrix();
            g.disableScissor();
        }

        if (maxScroll > 0) {
            int trackY = getY() + 3;
            int trackH = getHeight() - 6;
            int barH = Math.max(14, trackH * trackH / Math.max(1, content));
            int barY = trackY + (int) ((trackH - barH) * (scroll / maxScroll));
            int barX = getX() + getWidth() - 4;
            g.fill(barX, trackY, barX + 2, trackY + trackH, 0xFF131C2E);
            g.fill(barX, barY, barX + 2, barY + barH, NeonGui.ACCENT_DIM);
        }
    }

    /** Optional title block drawn at the top of the scrolled content. */
    protected void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
    }

    // --------------------------------------------------------------- input

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scroll -= vertical * 12.0;
        clampScroll();
        return true;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        if (!isMouseOver(event.x(), event.y())) {
            return false;
        }
        return ContainerEventHandler.super.mouseClicked(event, doubled);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight();
    }

    protected void clampScroll() {
        double max = Math.max(0, contentHeight() - (getHeight() - 10));
        scroll = Math.max(0, Math.min(scroll, max));
    }

    public void resetScroll() {
        scroll = 0;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
    }

    // ------------------------------------------------------------- helpers

    /** Lays a control out on the next row and returns its y, advancing the cursor. */
    protected static int row(int y) {
        return y;
    }
}
