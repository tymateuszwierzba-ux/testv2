package pl.neontext.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.core.NeonContext;

/** Tags chat text runs as CHAT so the chat tab in the GUI controls them. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("HEAD"))
    private void neontext$enter(GuiGraphicsExtractor extractor, net.minecraft.client.gui.Font font,
                                int a, int b, int c, ChatComponent.DisplayMode mode, boolean d, CallbackInfo ci) {
        NeonContext.push(AnimTarget.CHAT);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("RETURN"))
    private void neontext$leave(GuiGraphicsExtractor extractor, net.minecraft.client.gui.Font font,
                                int a, int b, int c, ChatComponent.DisplayMode mode, boolean d, CallbackInfo ci) {
        NeonContext.pop();
    }
}
