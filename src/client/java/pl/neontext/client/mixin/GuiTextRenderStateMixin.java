package pl.neontext.client.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.core.NeonContext;
import pl.neontext.client.core.TextAnimator;

/**
 * Animates every GUI text run: chat, tab list, HUD, tooltips, screens.
 *
 * <p>{@code ensurePrepared} is where a text run turns into glyphs, and it caches the result across
 * frames. Wrapping the return value is ideal for us: the cache keeps the expensive layout, while our
 * wrapper recomputes colours and transforms on every {@code visit}, so cached text still animates.
 */
@Mixin(GuiTextRenderState.class)
public abstract class GuiTextRenderStateMixin {

    @Inject(method = "ensurePrepared", at = @At("RETURN"), cancellable = true)
    private void neontext$animate(CallbackInfoReturnable<Font.PreparedText> cir) {
        Font.PreparedText prepared = cir.getReturnValue();
        if (prepared instanceof pl.neontext.client.anim.AnimatedPreparedText || prepared == null) {
            return;
        }
        GuiTextRenderState self = (GuiTextRenderState) (Object) this;
        AnimTarget target = NeonContext.stampOf(self);
        AnimStyle style = NeonContext.resolve(NeonContext.textOf(self), target,
                NeonContext.hologramOf(self), NeonContext.taggedOf(self));
        if (style == null) {
            return;
        }
        int seed = NeonContext.taggedOf(self) != null ? NeonContext.taggedOf(self).seed()
                : TextAnimator.seedOf(NeonContext.textOf(self));
        Font.PreparedText animated = TextAnimator.animate(prepared, NeonContext.textOf(self), style, seed);
        if (animated != null) {
            cir.setReturnValue(animated);
        }
    }
}
