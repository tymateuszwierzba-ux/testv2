package pl.neontext.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.cfg.NeonConfig;
import pl.neontext.client.cfg.Presets;
import pl.neontext.client.core.NeonRuntime;
import pl.neontext.client.gui.widget.EffectPicker;
import pl.neontext.client.gui.widget.NeonButton;
import pl.neontext.client.gui.widget.NeonSlider;
import pl.neontext.client.gui.widget.NeonToggle;
import pl.neontext.client.gui.widget.PreviewText;
import pl.neontext.client.gui.widget.TextLabel;
import pl.neontext.client.holo.Hologram;
import pl.neontext.client.holo.HologramManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manager for the client-side world holograms: add one at your position, select it, edit its text,
 * placement, scale, visibility and animation style. Everything is saved to the config instantly and
 * never touches the server.
 */
public class HologramsPanel extends Panel {

    private final String focusHologram;
    private String selectedId;

    private String newId = "holo";
    private String newText = "Hello world";

    private int contentBottom;

    public HologramsPanel(Font font, String focusHologram, int x, int y, int width, int height) {
        super(font, x, y, width, height, "My holograms");
        this.focusHologram = focusHologram;
        this.selectedId = findFocus();
        rebuild();
    }

    private String findFocus() {
        if (focusHologram != null && NeonRuntime.config().hologram(focusHologram) != null) {
            return focusHologram;
        }
        List<Hologram> all = NeonRuntime.config().holograms;
        return all.isEmpty() ? null : all.get(0).id;
    }

    private Hologram selected() {
        return selectedId == null ? null : NeonRuntime.config().hologram(selectedId);
    }

    private void changed() {
        NeonRuntime.save();
    }

    /** Creates a hologram at the player's position and selects it. */
    private void addHologram() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String id = newId == null || newId.isBlank() ? "holo" : newId.trim();
        if (NeonRuntime.config().hologram(id) != null) {
            selectedId = id; // already exists - just select it
            rebuild();
            return;
        }
        Hologram h = new Hologram(id, newText == null || newText.isEmpty() ? "Hello world" : newText,
                mc.player.getX(), mc.player.getY() + 2.2, mc.player.getZ());
        h.style = NeonRuntime.config().style(pl.neontext.client.anim.AnimTarget.HOLOGRAM).copy();
        HologramManager.add(h);
        selectedId = id;
        rebuild();
    }

    // ------------------------------------------------------------------ build

    @Override
    public void build(int x, int y, int width) {
        int pad = 10;
        int w = width - pad * 2 - 8;
        int cx = x + pad;
        int cy = y + 34;

        // ---- live preview of the selected hologram -------------------------
        PreviewText preview = new PreviewText(font, cx, cy, w,
                () -> {
                    Hologram h = selected();
                    return h != null ? h.style : NeonRuntime.config().style(pl.neontext.client.anim.AnimTarget.HOLOGRAM);
                },
                () -> {
                    Hologram h = selected();
                    return h == null ? "HOLOGRAM PREVIEW" : h.text;
                });
        preview.height(48).scale(1.6f).title("LIVE PREVIEW");
        addRenderOnly(preview);
        cy += 48 + 8;

        // ---- add -----------------------------------------------------------
        addRenderOnly(new TextLabel(font, "ADD HOLOGRAM", cx, cy).divider(true));
        cy += 12;

        EditBox idBox = new EditBox(font, cx, cy, (int) (w * 0.36f), 20, Component.literal("Id"));
        idBox.setValue(newId);
        idBox.setMaxLength(24);
        idBox.setResponder(value -> newId = value.trim());
        add(idBox);

        EditBox textBox = new EditBox(font, cx + (int) (w * 0.36f) + 6, cy,
                w - (int) (w * 0.36f) - 6, 20, Component.literal("Text"));
        textBox.setValue(newText);
        textBox.setMaxLength(96);
        textBox.setResponder(value -> newText = value);
        add(textBox);
        cy += 20 + 6;

        boolean inWorld = Minecraft.getInstance().player != null;
        NeonButton addButton = new NeonButton(font, cx, cy, w, 22,
                inWorld ? "Add hologram at my position" : "Join a world to place holograms",
                b -> addHologram());
        addButton.active = inWorld;
        addButton.accent(NeonGui.GREEN);
        add(addButton);
        cy += 22 + 12;

        // ---- list ----------------------------------------------------------
        List<Hologram> all = new ArrayList<>(NeonRuntime.config().holograms);
        addRenderOnly(new TextLabel(font, "YOUR HOLOGRAMS (" + all.size() + ")", cx, cy).divider(true));
        cy += 12;

        if (all.isEmpty()) {
            final int emptyY = cy;
            addRenderOnly((g, mx, my, pt) ->
                    g.text(font, "Nothing here yet - add one above, it is client-side only.", cx, emptyY,
                            NeonGui.TEXT_FAINT));
            cy += 14;
        }

        for (Hologram h : all) {
            cy = addHologramRow(cx, cy, w, h);
        }
        cy += 8;

        // ---- editor --------------------------------------------------------
        Hologram sel = selected();
        if (sel != null) {
            addRenderOnly(new TextLabel(font, "EDITING: " + sel.id, cx, cy).color(NeonGui.ACCENT).divider(true));
            cy += 12;

            EditBox editText = new EditBox(font, cx, cy, w, 20, Component.literal("Hologram text"));
            editText.setValue(sel.text);
            editText.setMaxLength(96);
            editText.setResponder(value -> {
                Hologram h = selected();
                if (h != null) {
                    h.text = value;
                    changed();
                }
            });
            add(editText);
            cy += 20 + 6;

            // placement
            add(new NeonButton(font, cx, cy, 110, 20, "Move to me", b -> {
                Hologram h = selected();
                var p = Minecraft.getInstance().player;
                if (h != null && p != null) {
                    h.x = p.getX();
                    h.y = p.getY() + 2.2;
                    h.z = p.getZ();
                    changed();
                    rebuild();
                }
            }).accent(NeonGui.ACCENT));
            final String coords = String.format(Locale.ROOT, "%.1f, %.1f, %.1f", sel.x, sel.y, sel.z);
            final int coordsY = cy + 6;
            addRenderOnly((g, mx, my, pt) -> g.text(font, coords, cx + 120, coordsY, NeonGui.TEXT_DIM));
            cy += 20 + 6;

            cy = addSlider(cx, cy, w, "Scale", 0.2, 8.0, sel.scale, 2, "x",
                    v -> {
                        Hologram h = selected();
                        if (h != null) {
                            h.scale = v.floatValue();
                            changed();
                        }
                    },
                    () -> {
                        Hologram h = selected();
                        return h == null ? 1.0 : h.scale;
                    });
            cy = addSlider(cx, cy, w, "Visible distance", 8, 256, sel.maxDistance, 0, " blocks",
                    v -> {
                        Hologram h = selected();
                        if (h != null) {
                            h.maxDistance = v.intValue();
                            changed();
                        }
                    },
                    () -> {
                        Hologram h = selected();
                        return h == null ? 64 : h.maxDistance;
                    });

            add(new NeonToggle(font, cx, cy, w, 22, "Enabled", "Show this hologram in the world",
                    () -> {
                        Hologram h = selected();
                        return h != null && h.enabled;
                    },
                    on -> {
                        Hologram h = selected();
                        if (h != null) {
                            h.enabled = on;
                            changed();
                        }
                    }));
            cy += 22 + 4;
            add(new NeonToggle(font, cx, cy, w, 22, "Billboard", "Always face the camera",
                    () -> {
                        Hologram h = selected();
                        return h != null && h.billboard;
                    },
                    on -> {
                        Hologram h = selected();
                        if (h != null) {
                            h.billboard = on;
                            changed();
                        }
                    }));
            cy += 22 + 4;
            add(new NeonToggle(font, cx, cy, w, 22, "Background plate", "Semi-transparent plate behind the text",
                    () -> {
                        Hologram h = selected();
                        return h != null && h.background != 0;
                    },
                    on -> {
                        Hologram h = selected();
                        if (h != null) {
                            h.background = on ? 0x90000000 : 0;
                            changed();
                        }
                    }));
            cy += 22 + 10;

            // style
            addRenderOnly(new TextLabel(font, "STYLE", cx, cy).divider(true));
            cy += 12;
            add(new EffectPicker(font, cx, cy, w, 30,
                    () -> {
                        Hologram h = selected();
                        return h == null ? pl.neontext.client.anim.Effect.NONE : h.style.effect();
                    },
                    e -> {
                        Hologram h = selected();
                        if (h != null) {
                            h.style.setEffect(e);
                            h.style.presetName = "";
                            changed();
                        }
                    }));
            cy += 30 + 6;

            cy = addSlider(cx, cy, w, "Speed", 0, 5, sel.style.speed, 2, "x",
                    v -> {
                        Hologram h = selected();
                        if (h != null) {
                            h.style.speed = v.floatValue();
                            changed();
                        }
                    },
                    () -> {
                        Hologram h = selected();
                        return h == null ? 1.0 : h.style.speed;
                    });

            // quick presets for the hologram style
            int perRow = 3;
            int bw = (w - (perRow - 1) * 4) / perRow;
            int i = 0;
            for (Presets.Preset p : Presets.ALL) {
                if (i >= 6) {
                    break;
                }
                final Presets.Preset preset = p;
                int col = i % perRow;
                int row = i / perRow;
                int px = cx + col * (bw + 4);
                int py = cy + row * 22;
                add(new NeonButton(font, px, py, bw, 20, p.name(), b -> {
                    Hologram h = selected();
                    if (h != null) {
                        h.style = preset.create();
                        h.style.presetName = preset.name();
                        changed();
                        rebuild();
                    }
                }).accent(NeonGui.TEXT_DIM));
                i++;
            }
            cy += ((i + perRow - 1) / perRow) * 22 + 8;

            add(new NeonButton(font, cx, cy, (w - 6) / 2, 22, "Copy default style", b -> {
                Hologram h = selected();
                if (h != null) {
                    h.style = NeonRuntime.config().style(pl.neontext.client.anim.AnimTarget.HOLOGRAM).copy();
                    changed();
                    rebuild();
                }
            }).accent(NeonGui.ACCENT));
            add(new NeonButton(font, cx + (w - 6) / 2 + 6, cy, (w - 6) / 2, 22, "Delete hologram", b -> {
                Hologram h = selected();
                if (h != null) {
                    HologramManager.remove(h);
                    selectedId = null;
                    rebuild();
                }
            }).accent(NeonGui.RED));
            cy += 22 + 12;
        }

        contentBottom = cy - y;
    }

    private int addHologramRow(int cx, int cy, int w, Hologram h) {
        final String id = h.id;
        boolean isSelected = id.equals(selectedId);
        String label = h.id + "  \u00B7  " + String.format(Locale.ROOT, "%.0f, %.0f, %.0f", h.x, h.y, h.z);

        NeonButton select = new NeonButton(font, cx, cy, w - 108, 20, label, b -> {
            selectedId = id;
            rebuild();
        });
        select.selected(isSelected);
        select.accent(isSelected ? NeonGui.ACCENT : NeonGui.TEXT_DIM);
        add(select);

        NeonButton toggle = new NeonButton(font, cx + w - 104, cy, 52, 20, h.enabled ? "ON" : "OFF", b -> {
            Hologram target = NeonRuntime.config().hologram(id);
            if (target != null) {
                target.enabled = !target.enabled;
                changed();
                rebuild();
            }
        });
        toggle.accent(h.enabled ? NeonGui.GREEN : NeonGui.RED);
        add(toggle);

        add(new NeonButton(font, cx + w - 48, cy, 44, 20, "X", b -> {
            Hologram target = NeonRuntime.config().hologram(id);
            if (target != null) {
                HologramManager.remove(target);
                if (id.equals(selectedId)) {
                    selectedId = null;
                }
                rebuild();
            }
        }).accent(NeonGui.RED));

        return cy + 20 + 4;
    }

    private int addSlider(int x, int y, int width, String label, double min, double max, double value,
                          int decimals, String suffix, java.util.function.Consumer<Double> onChange,
                          java.util.function.DoubleSupplier external) {
        add(new NeonSlider(font, x, y, width, 24, label, min, max, value, decimals, suffix,
                v -> {
                    onChange.accept(v);
                    changed();
                }, external));
        return y + 24 + 4;
    }

    @Override
    protected int contentHeight() {
        return contentBottom + 34;
    }

    @Override
    protected void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, "MY HOLOGRAMS", getX() + 10, getY() + 10, NeonGui.GREEN);
        g.text(font, "Client-side text signs in the world", getX() + 10, getY() + 21, NeonGui.TEXT_FAINT);
    }
}
