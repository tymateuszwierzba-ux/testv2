package pl.neontext.client.mixin;

import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.core.NeonContext;

/**
 * Stamps every GUI text run with the target that was extracting at the time it was added.
 *
 * <p>Chat, the tab list and the HUD all extract into the same {@link GuiRenderState}, and the text
 * is only prepared later during {@code GuiRenderer.prepare}. Recording the target here - instead of
 * reading a global at prepare time - keeps each run correctly categorised no matter what order the
 * extraction happens in.
 */
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {

    @Inject(method = "addText", at = @At("HEAD"))
    private void neontext$stampTarget(GuiTextRenderState text, CallbackInfo ci) {
        NeonContext.stamp(text, NeonContext.current());
    }
}
