package pl.neontext.client.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.neontext.client.anim.AnimStyle;
import pl.neontext.client.core.NeonContext;
import pl.neontext.client.core.NeonDebug;
import pl.neontext.client.core.TextAnimator;
import pl.neontext.client.holo.Hologram;

/**
 * The master hook. Every piece of text in the game - GUI, HUD, chat, tab list, world nameplates and
 * our own holograms - is drawn by preparing it through the font and then visiting the glyphs, so
 * wrapping the prepared text here animates all of them with one injection.
 */
@Mixin(Font.class)
public abstract class FontMixin {

    @Inject(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("RETURN"), cancellable = true)
    private void neontext$animateSequence(FormattedCharSequence text, float x, float y, int color,
                                          boolean dropShadow, boolean includeEmpty, int backgroundColor,
                                          CallbackInfoReturnable<Font.PreparedText> cir) {
        Font.PreparedText animated = neontext$wrap(cir.getReturnValue(), text);
        if (animated != null) {
            cir.setReturnValue(animated);
        }
    }

    /** {@code drawInBatch(String, ...)} prepares through the String overload; hook it too. */
    @Inject(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("RETURN"), cancellable = true)
    private void neontext$animateString(String text, float x, float y, int color,
                                        boolean dropShadow, int backgroundColor,
                                        CallbackInfoReturnable<Font.PreparedText> cir) {
        if (text == null) {
            return;
        }
        FormattedCharSequence seq = net.minecraft.network.chat.Component.literal(text).getVisualOrderText();
        Font.PreparedText animated = neontext$wrap(cir.getReturnValue(), seq);
        if (animated != null) {
            cir.setReturnValue(animated);
        }
    }

    /**
     * The 8x outline pass (used for the black border around nameplates) gets the same treatment so
     * the outline follows the animated glyphs instead of staying behind.
     */
    @Inject(method = "prepare8xTextOutline(Lnet/minecraft/util/FormattedCharSequence;FFI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("RETURN"), cancellable = true)
    private void neontext$animateOutline(FormattedCharSequence text, float x, float y, int color,
                                         CallbackInfoReturnable<Font.PreparedText> cir) {
        Font.PreparedText animated = neontext$wrap(cir.getReturnValue(), text);
        if (animated != null) {
            cir.setReturnValue(animated);
        }
    }

    private static Font.PreparedText neontext$wrap(Font.PreparedText prepared, FormattedCharSequence text) {
        if (prepared == null || text == null) {
            return null;
        }
        AnimStyle style = NeonContext.resolve(text);
        if (style == null) {
            return null;
        }
        Hologram holo = NeonContext.hologramFor(text);
        int seed = holo != null ? holo.seed() : TextAnimator.seedOf(text);
        NeonDebug.fontWraps++;
        return TextAnimator.animate(prepared, text, style, seed);
    }
}
