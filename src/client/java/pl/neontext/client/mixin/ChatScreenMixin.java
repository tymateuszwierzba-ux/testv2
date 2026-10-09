package pl.neontext.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.neontext.client.core.ChatHitCollector;
import pl.neontext.client.core.NeonClick;
import pl.neontext.client.core.NeonRuntime;

/**
 * Clicking a chat message (left or right button) selects the chat target and swaps its preset -
 * the chat half of "klikam na tekst na chacie i to mi się zamienia". Clicks that vanilla already
 * consumed (links, insertions, suggestion popups, the input field) pass through untouched.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    private ChatComponent.DisplayMode displayMode;

    @Inject(method = "mouseClicked", at = @At("RETURN"), cancellable = true)
    private void neontext$clickChat(MouseButtonEvent event, boolean doubleClick,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !NeonRuntime.clickSelect || !NeonRuntime.masterEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui == null || mc.getWindow() == null) {
            return;
        }
        ChatHitCollector hit = new ChatHitCollector(mc.font, (int) event.x(), (int) event.y());
        mc.gui.hud.getChat().captureClickableText(hit, mc.getWindow().getGuiScaledHeight(),
                mc.gui.hud.getGuiTicks(), this.displayMode);
        if (hit.isHit()) {
            NeonClick.clickChat();
            cir.setReturnValue(true);
        }
    }
}
