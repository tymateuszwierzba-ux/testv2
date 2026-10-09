package pl.neontext.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.cfg.Presets;
import pl.neontext.client.holo.Hologram;
import pl.neontext.client.holo.HologramManager;

/**
 * Click-to-edit: clicking a hologram in the world (crosshair over it) or clicking chat text selects
 * that thing and swaps it to the next preset - "klikam i to mi się zamienia". Precise editing is
 * done with {@code /neon} commands on the same selection.
 */
public final class NeonClick {

    private NeonClick() {
    }

    /** @return true when the click was consumed (a hologram was under the crosshair). */
    public static boolean clickHologram() {
        if (!NeonRuntime.clickSelect || !NeonRuntime.masterEnabled()) {
            return false;
        }
        Hologram best = pickUnderCrosshair();
        if (best == null) {
            return false;
        }
        NeonRuntime.selectHolo(best.id);
        cycleSelection();
        return true;
    }

    /** @return the hologram under the crosshair, or null. */
    public static Hologram pickUnderCrosshair() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.getWindow() == null) {
            return null;
        }
        float cx = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float cy = mc.getWindow().getGuiScaledHeight() / 2.0f;
        Hologram best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Hologram h : HologramManager.all()) {
            if (!h.enabled || !h.onScreen) {
                continue;
            }
            float pad = 12.0f;
            if (cx < h.screenLeft - pad || cx > h.screenRight + pad
                    || cy < h.screenTop - pad || cy > h.screenBottom + pad) {
                continue;
            }
            double d = h.distanceSq(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            if (d < bestDistance) {
                bestDistance = d;
                best = h;
            }
        }
        return best;
    }

    /** Selects the chat target and swaps its preset. Called when chat text is clicked. */
    public static void clickChat() {
        NeonRuntime.select(AnimTarget.CHAT);
        cycleSelection();
    }

    /** Applies the next preset to the current selection and prints the change. */
    public static void cycleSelection() {
        AnimStyle current;
        String label;
        boolean holo = NeonRuntime.selectedHolo != null;
        if (holo) {
            Hologram h = NeonRuntime.config().hologram(NeonRuntime.selectedHolo);
            if (h == null) {
                return;
            }
            current = h.style;
            label = "Hologram " + h.id;
        } else if (NeonRuntime.selectedTarget != null) {
            current = NeonRuntime.style(NeonRuntime.selectedTarget);
            label = targetLabel(NeonRuntime.selectedTarget);
        } else {
            return;
        }

        int start = indexOfPreset(current);
        int next = (start + 1) % Presets.ALL.length;
        if (NeonRuntime.lastPresetIndex >= 0) {
            next = (NeonRuntime.lastPresetIndex + 1) % Presets.ALL.length;
        }
        Presets.Preset preset = Presets.ALL[next];
        AnimStyle applied = preset.create();
        if (holo) {
            Hologram h = NeonRuntime.config().hologram(NeonRuntime.selectedHolo);
            h.style = applied;
        } else {
            NeonRuntime.config().target(NeonRuntime.selectedTarget).style = applied;
        }
        NeonRuntime.lastPresetIndex = next;
        NeonRuntime.save();
        feedback("§b[Neon] §f" + label + " §7→ §b" + preset.name() + " §8(§7" + applied.effect().name().toLowerCase() + "§8)");
    }

    /** @return the preset whose effect matches the current style, or -1. */
    static int indexOfPreset(AnimStyle style) {
        if (style == null) {
            return -1;
        }
        for (int i = 0; i < Presets.ALL.length; i++) {
            AnimStyle s = Presets.ALL[i].create();
            if (s.effect == style.effect) {
                return i;
            }
        }
        return -1;
    }

    public static String targetLabel(AnimTarget t) {
        return switch (t) {
            case NAMEPLATE -> "Nameplates";
            case TAB -> "Tab list";
            case CHAT -> "Chat";
            case HOLOGRAM -> "Hologram defaults";
            case GUI -> "GUI";
            case OTHER -> "Other";
        };
    }

    public static void feedback(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null) {
            mc.gui.chatListener().handleSystemMessage(Component.literal(message), false);
        }
    }
}
