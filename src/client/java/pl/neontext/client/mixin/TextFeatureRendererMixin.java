package pl.neontext.client.mixin;

import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.core.NeonContext;

/**
 * Marks the ambient target as "world text" while in-world text is turned into glyphs.
 *
 * <p>Nameplates, holograms and any other text drawn in the level all arrive here, and they never go
 * through {@code GuiRenderState}, so there is no stamp to read - the ambient target is what
 * {@link FontMixin} will resolve against.
 */
@Mixin(TextFeatureRenderer.class)
public abstract class TextFeatureRendererMixin {

    @Inject(method = "buildGroup", at = @At("HEAD"))
    private void neontext$enterWorldText(FeatureFrameContext context, java.util.List<?> submits, CallbackInfo ci) {
        NeonContext.push(AnimTarget.NAMEPLATE);
    }

    @Inject(method = "buildGroup", at = @At("RETURN"))
    private void neontext$leaveWorldText(FeatureFrameContext context, java.util.List<?> submits, CallbackInfo ci) {
        NeonContext.pop();
    }
}
