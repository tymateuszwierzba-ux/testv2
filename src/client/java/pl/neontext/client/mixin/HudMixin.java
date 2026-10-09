package pl.neontext.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.neontext.client.holo.HologramRenderer;

/**
 * Draws the holograms as part of the HUD, right after vanilla has extracted its own elements, so
 * they sit above the world but below chat and any open screen.
 */
@Mixin(Hud.class)
public abstract class HudMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void neontext$holograms(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        // 26.x replaced options.hideGui with the HUD's own hidden flag (F1 / hud.toggle())
        if (mc.gui == null || mc.gui.hud.isHidden()) {
            return;
        }
        HologramRenderer.render(extractor, mc.font);
    }
}
