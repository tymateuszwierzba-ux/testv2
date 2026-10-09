package pl.neontext.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.neontext.client.NeonTextClient;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.anim.Effect;
import pl.neontext.client.gui.widget.NeonButton;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The NeonText control centre. One screen holds every editor: per-target animation panels, the
 * hologram manager, global settings and the creators page.
 *
 * <p>The screen is pure chrome: a sidebar with navigation and a big content rectangle in which the
 * active {@link Panel} lives. Panels are cached per tab and repositioned on resize, so unsaved
 * typing (preview text, hex fields) survives switching tabs back and forth.
 */
public final class NeonScreen extends Screen {

    /** Sidebar destinations. The {@code key} is what {@code /neon gui <tab>} accepts. */
    public enum Tab {
        NAMEPLATE("Nameplates", "nameplate", AnimTarget.NAMEPLATE, true),
        TAB_LIST("Tab list", "tab", AnimTarget.TAB, true),
        CHAT("Chat", "chat", AnimTarget.CHAT, true),
        HOLOGRAM_STYLE("Hologram style", "hologram", AnimTarget.HOLOGRAM, true),
        GUI("GUI & HUD", "gui", AnimTarget.GUI, true),
        WORLD("My holograms", "holograms", null, false),
        SETTINGS("Settings", "settings", null, false),
        CREATORS("Creators", "creators", null, false);

        public final String label;
        public final String key;
        public final AnimTarget target;
        public final boolean animateSection;

        Tab(String label, String key, AnimTarget target, boolean animateSection) {
            this.label = label;
            this.key = key;
            this.target = target;
            this.animateSection = animateSection;
        }

        static Tab from(String raw) {
            if (raw == null) {
                return NAMEPLATE;
            }
            String s = raw.trim().toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
            return switch (s) {
                case "nameplate", "nameplates", "name" -> NAMEPLATE;
                case "tab", "tablist", "tab list", "playerlist" -> TAB_LIST;
                case "chat" -> CHAT;
                case "hologram", "holo style", "hologram style" -> HOLOGRAM_STYLE;
                case "gui", "hud", "gui & hud", "interface" -> GUI;
                case "holograms", "holos", "my holograms", "world" -> WORLD;
                case "settings", "config", "options", "global" -> SETTINGS;
                case "creators", "credits", "about", "team" -> CREATORS;
                default -> NAMEPLATE;
            };
        }
    }

    /** The animated wordmark style used in the sidebar. */
    private static AnimStyle logoStyle() {
        AnimStyle s = new AnimStyle();
        s.setEffect(Effect.GRADIENT);
        s.speed = 0.45f;
        s.spread = 1.3f;
        s.brightness = 1.1f;
        s.colors = new String[]{"#00E5FF", "#7A5CFF", "#FF3DCB", "#00E5FF"};
        s.shadow = true;
        return s;
    }

    private final String focusHologram;
    private final Map<Tab, Panel> panels = new EnumMap<>(Tab.class);

    private Tab activeTab;

    public NeonScreen(String tab) {
        this(tab, null);
    }

    public NeonScreen(String tab, String focusHologram) {
        super(Component.literal("NeonText"));
        this.activeTab = Tab.from(tab);
        this.focusHologram = focusHologram;
    }

    // ---------------------------------------------------------------- layout

    private int contentX() {
        return NeonGui.SIDEBAR_WIDTH + 12;
    }

    private int contentY() {
        return 12;
    }

    private int contentWidth() {
        return Math.max(120, width - NeonGui.SIDEBAR_WIDTH - 24);
    }

    private int contentHeight() {
        return Math.max(80, height - 24);
    }

    @Override
    protected void init() {
        buildSidebar();

        Panel panel = panels.computeIfAbsent(activeTab, this::createPanel);
        panel.visible = true;
        panel.reposition(contentX(), contentY(), contentWidth(), contentHeight());
        addRenderableWidget(panel);
    }

    @Override
    protected void repositionElements() {
        // full rebuild keeps the sidebar and the panel in step with the new window size
        rebuildWidgets();
    }

    private Panel createPanel(Tab tab) {
        int x = contentX();
        int y = contentY();
        int w = contentWidth();
        int h = contentHeight();
        if (tab.target != null) {
            return new TargetPanel(font, tab.target, x, y, w, h);
        }
        return switch (tab) {
            case WORLD -> new HologramsPanel(font, focusHologram, x, y, w, h);
            case SETTINGS -> new GlobalPanel(font, x, y, w, h);
            case CREATORS -> new CreatorsPanel(font, x, y, w, h);
            default -> new GlobalPanel(font, x, y, w, h);
        };
    }

    private void buildSidebar() {
        int x = 10;
        int y = 42;
        for (Tab tab : Tab.values()) {
            if (tab == Tab.WORLD) {
                y += 10; // gap before the MOD section
            }
            NeonButton button = new NeonButton(font, x, y, NeonGui.SIDEBAR_WIDTH - 20, 22, tab.label,
                    b -> setTab(tab));
            button.selected(tab == activeTab);
            button.accent(tab == activeTab ? NeonGui.ACCENT
                    : tab.animateSection ? NeonGui.TEXT_DIM : NeonGui.MAGENTA);
            addRenderableWidget(button);
            y += 26;
        }
    }

    private void setTab(Tab tab) {
        if (tab == activeTab) {
            return;
        }
        activeTab = tab;
        rebuildWidgets();
    }

    // -------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, NeonGui.BG);
        g.fill(0, 0, NeonGui.SIDEBAR_WIDTH, height, NeonGui.SIDEBAR);
        g.fill(NeonGui.SIDEBAR_WIDTH, 0, NeonGui.SIDEBAR_WIDTH + 1, height, NeonGui.OUTLINE);

        // content frame
        int x = contentX() - 4;
        int y = contentY() - 4;
        int w = contentWidth() + 8;
        int h = contentHeight() + 8;
        g.fill(x, y, x + w, y + 1, NeonGui.OUTLINE);
        g.fill(x, y + h - 1, x + w, y + h, NeonGui.OUTLINE);
        g.fill(x, y, x + 1, y + h, NeonGui.OUTLINE);
        g.fill(x + w - 1, y, x + w, y + h, NeonGui.OUTLINE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        drawChrome(g);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private void drawChrome(GuiGraphicsExtractor g) {
        // animated wordmark - drawn through the tagged-sequence path so it always animates
        NeonGui.drawPreviewCentered(g, font, "NEONTEXT", logoStyle(), NeonGui.SIDEBAR_WIDTH / 2, 10, 0xFFFFFF);
        NeonGui.text(g, font, "v" + NeonTextClient.VERSION + " \u00B7 MC 26.3", 10, 28, NeonGui.TEXT_FAINT);

        // section captions
        int y = 42;
        for (Tab tab : Tab.values()) {
            if (tab == Tab.NAMEPLATE) {
                NeonGui.text(g, font, "ANIMATE", 10, y - 2, NeonGui.TEXT_FAINT);
            }
            if (tab == Tab.WORLD) {
                NeonGui.text(g, font, "MOD", 10, y + 2, NeonGui.TEXT_FAINT);
            }
            y += 26;
        }

        // footer hints
        NeonGui.text(g, font, "K or /neon \u2013 open \u00B7 ESC \u2013 close", 10, height - 14, NeonGui.TEXT_FAINT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
