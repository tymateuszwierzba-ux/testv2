package pl.neontext.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.neontext.client.anim.AnimTarget;
import pl.neontext.client.core.NeonContext;
import pl.neontext.client.core.NeonRuntime;

/** Tags tab list text runs as TAB, and resolves the "only me" option per row. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("HEAD"))
    private void neontext$enter(GuiGraphicsExtractor extractor, int height,
                                net.minecraft.world.scores.Scoreboard scoreboard,
                                net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        NeonContext.push(AnimTarget.TAB);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("RETURN"))
    private void neontext$leave(GuiGraphicsExtractor extractor, int height,
                                net.minecraft.world.scores.Scoreboard scoreboard,
                                net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        NeonContext.setLocalPlayerOnly(false);
        NeonContext.pop();
    }

    /**
     * Called once per row while extracting. We use it to flag whether the row being drawn is the
     * local player, which is what the "only me" switch filters on.
     */
    @Inject(method = "getNameForDisplay", at = @At("RETURN"))
    private void neontext$trackRow(PlayerInfo info, CallbackInfoReturnable<net.minecraft.network.chat.Component> cir) {
        String me = NeonRuntime.localPlayerName();
        String name = cir.getReturnValue() == null ? "" : cir.getReturnValue().getString();
        NeonContext.setLocalPlayerOnly(me != null && !me.isEmpty() && name.contains(me));
    }
}
