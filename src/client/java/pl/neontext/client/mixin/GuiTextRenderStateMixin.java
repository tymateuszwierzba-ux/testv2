package pl.neontext.client.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimatedPreparedText;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.core.NeonContext;
import pl.neontext.client.core.NeonDebug;
import pl.neontext.client.core.TextAnimator;

/**
 * Animates every GUI text run: chat, tab list, HUD, tooltips, screens.
 *
 * <p>{@code ensurePrepared} is where a text run turns into glyphs, and it caches the result across
 * frames. Wrapping the return value is ideal for us: the cache keeps the expensive layout, while
 * our wrapper recomputes colours and transforms on every {@code visit}, so cached text still
 * animates.
 *
 * <p>The wrap is redone here even when {@link FontMixin} already wrapped the builder during
 * {@code ensurePrepared}: that earlier wrap used the ambient target, while the run's stamped target
 * (chat, tab, hologram) is the truth. We unwrap and rewrap with the stamped style - that mismatch
 * was why per-target settings looked broken in game.
 */
@Mixin(GuiTextRenderState.class)
public abstract class GuiTextRenderStateMixin {

    @Inject(method = "ensurePrepared", at = @At("RETURN"), cancellable = true)
    private void neontext$animate(CallbackInfoReturnable<Font.PreparedText> cir) {
        Font.PreparedText raw = cir.getReturnValue();
        if (raw == null) {
            return;
        }
        AnimatedPreparedText existing = raw instanceof AnimatedPreparedText a ? a : null;
        Font.PreparedText prepared = existing != null ? existing.unwrap() : raw;
        if (prepared instanceof AnimatedPreparedText) {
            return;
        }

        GuiTextRenderState self = (GuiTextRenderState) (Object) this;
        AnimTarget target = NeonContext.stampOf(self);
        AnimStyle style = NeonContext.resolve(NeonContext.textOf(self), target,
                NeonContext.hologramOf(self), NeonContext.taggedOf(self));
        if (style == null) {
            if (existing != null) {
                // strip the ambient wrap: this run must stay vanilla
                cir.setReturnValue(prepared);
                NeonDebug.guiSkipped++;
            }
            return;
        }

        pl.neontext.client.holo.NeonTextSequence tagged = NeonContext.taggedOf(self);
        int seed = tagged != null ? tagged.seed() : TextAnimator.seedOf(NeonContext.textOf(self));
        Font.PreparedText animated = TextAnimator.animate(prepared, NeonContext.textOf(self), style, seed);
        if (animated != null) {
            NeonDebug.guiWraps++;
            if (NeonContext.hologramOf(self) != null) {
                NeonDebug.holoStampsFrame++;
            } else if (target == AnimTarget.CHAT) {
                NeonDebug.chatStampsFrame++;
            }
            cir.setReturnValue(animated);
        } else if (existing != null) {
            cir.setReturnValue(prepared);
            NeonDebug.guiSkipped++;
        }
    }
}
