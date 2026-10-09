package pl.neontext.client.core;

import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.holo.Hologram;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Answers "which part of the UI does this text belong to?" for every text draw.
 *
 * <p>Chat, the tab list, the HUD and screens all funnel into the same font and GUI pipeline, so the
 * mixins around each extraction push their {@link AnimTarget} here and
 * {@link pl.neontext.client.mixin.GuiRenderStateMixin} stamps every text run with the target that was
 * active when it was added. Text is prepared later, during {@code GuiRenderer.prepare}, so the stamp
 * has to travel with the text run rather than be read from a global.
 *
 * <p>World text (nameplates and holograms) never goes through {@code GuiRenderState}, so it falls
 * back to the ambient target; holograms are additionally recognised by identity because we create
 * their character sequence ourselves.
 */
public final class NeonContext {

    private static final ThreadLocal<AnimTarget> CURRENT = ThreadLocal.withInitial(() -> AnimTarget.GUI);

    /**
     * Target per GUI text run. Keys are only held strongly by the render state for the current
     * frame, so weak keys keep this from growing across the session.
     */
    private static final Map<GuiTextRenderState, AnimTarget> STAMPS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Hologram text runs, keyed by the text run itself.
     *
     * <p>The registration happens when the run is added to the render state rather than when it is
     * drawn, because text is prepared a phase later - by then the temporary marker sequence we used
     * to recognise it is long gone.
     */
    private static final Map<GuiTextRenderState, Hologram> HOLOGRAM_TEXT =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Handle on the private {@code text} field of a GUI text run, resolved once and cached. */
    private static VarHandle textHandle;
    private static boolean textHandleResolved;

    /** When true, only text belonging to the local player animates (the "only me" option). */
    private static final ThreadLocal<Boolean> LOCAL_PLAYER_ONLY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private NeonContext() {
    }

    // ------------------------------------------------------------- ambient

    public static AnimTarget current() {
        return CURRENT.get();
    }

    public static void push(AnimTarget target) {
        CURRENT.set(target);
    }

    public static void pop() {
        CURRENT.set(AnimTarget.GUI);
    }

    public static void setLocalPlayerOnly(boolean value) {
        LOCAL_PLAYER_ONLY.set(value);
    }

    public static boolean localPlayerOnly() {
        return LOCAL_PLAYER_ONLY.get();
    }

    // -------------------------------------------------------------- stamps

    public static void stamp(GuiTextRenderState text, AnimTarget target) {
        FormattedCharSequence sequence = textOf(text);
        if (sequence instanceof pl.neontext.client.holo.HologramText holoText) {
            HOLOGRAM_TEXT.put(text, holoText.hologram());
            STAMPS.put(text, AnimTarget.HOLOGRAM);
            return;
        }
        if (sequence instanceof pl.neontext.client.holo.NeonTextSequence tagged) {
            // carries its own style - a hologram or a GUI live preview
            TAGGED.put(text, tagged);
            STAMPS.put(text, AnimTarget.OTHER);
            return;
        }
        STAMPS.put(text, target);
    }

    /** Text runs that carry their own style, keyed by the run itself. */
    private static final Map<GuiTextRenderState, pl.neontext.client.holo.NeonTextSequence> TAGGED =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** The hologram a GUI text run belongs to, or null for ordinary text. */
    public static Hologram hologramOf(GuiTextRenderState text) {
        return HOLOGRAM_TEXT.get(text);
    }

    /**
     * The hologram a character sequence belongs to, or null. World / font-pipeline callers pass the
     * sequence itself; hologram sequences carry their owner as a {@link pl.neontext.client.holo.HologramText}.
     */
    public static Hologram hologramFor(Object textSequence) {
        return textSequence instanceof pl.neontext.client.holo.HologramText holoText
                ? holoText.hologram()
                : null;
    }

    /** The self-tagged style a GUI text run carries, or null when the ambient target decides. */
    public static pl.neontext.client.holo.NeonTextSequence taggedOf(GuiTextRenderState text) {
        return TAGGED.get(text);
    }

    public static AnimTarget stampOf(GuiTextRenderState text) {
        AnimTarget t = STAMPS.get(text);
        return t == null ? current() : t;
    }

    /** The character sequence a GUI text run will draw, read through a cached VarHandle. */
    public static FormattedCharSequence textOf(GuiTextRenderState text) {
        VarHandle h = textHandle();
        if (h == null) {
            return null;
        }
        try {
            return (FormattedCharSequence) h.get(text);
        } catch (Throwable t) {
            return null;
        }
    }

    private static VarHandle textHandle() {
        if (!textHandleResolved) {
            textHandleResolved = true;
            try {
                java.lang.reflect.Field f = GuiTextRenderState.class.getDeclaredField("text");
                f.setAccessible(true);
                textHandle = MethodHandles.lookup().unreflectVarHandle(f);
            } catch (Throwable t) {
                textHandle = null;
            }
        }
        return textHandle;
    }

    // ---------------------------------------------------------- holograms

    public static void clearHolograms() {
        HOLOGRAM_TEXT.clear();
    }

    // ------------------------------------------------------------ resolve

    /**
     * The style a text sequence should be drawn with, or null when it must stay vanilla.
     *
     * @param textSequence the sequence being prepared
     * @param target       the target it belongs to; null means "work it out from context"
     */
    public static AnimStyle resolve(Object textSequence, AnimTarget target) {
        return resolve(textSequence, target, null);
    }

    /**
     * @param hologram the hologram this text belongs to, when it is hologram text; holograms carry
     *                 their own style and ignore the ambient target
     */
    public static AnimStyle resolve(Object textSequence, AnimTarget target, Hologram hologram) {
        return resolve(textSequence, target, hologram, null);
    }

    /**
     * @param tagged a self-tagged run (hologram or GUI preview) that carries its own style
     */
    public static AnimStyle resolve(Object textSequence, AnimTarget target, Hologram hologram,
                                    pl.neontext.client.holo.NeonTextSequence tagged) {
        if (tagged != null) {
            return NeonRuntime.masterEnabled() && tagged.style().animates() ? tagged.style() : null;
        }
        if (hologram != null) {
            if (!NeonRuntime.masterEnabled() || !hologram.enabled
                    || !NeonRuntime.config().target(AnimTarget.HOLOGRAM).enabled) {
                return null;
            }
            return hologram.style.animates() ? hologram.style : NeonRuntime.style(AnimTarget.HOLOGRAM);
        }

        AnimTarget t = target == null ? current() : target;
        if (!NeonRuntime.isActive(t)) {
            return null;
        }
        if (localPlayerOnly() && NeonRuntime.style(t).onlyMe && !isLocalPlayerText(textSequence)) {
            return null;
        }
        return NeonRuntime.style(t);
    }

    /** Convenience overload that uses the ambient target (world text, untracked GUI text). */
    public static AnimStyle resolve(Object textSequence) {
        return resolve(textSequence, null);
    }

    private static boolean isLocalPlayerText(Object textSequence) {
        String me = NeonRuntime.localPlayerName();
        if (me == null || !(textSequence instanceof FormattedCharSequence fcs)) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        fcs.accept((index, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.indexOf(me) >= 0;
    }
}
