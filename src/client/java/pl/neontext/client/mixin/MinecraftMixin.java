package pl.neontext.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.neontext.client.core.NeonClick;

/**
 * Left-clicking in the world (the attack key) while the crosshair is over a hologram selects that
 * hologram and swaps its preset instead of swinging at it - the "klikam i to mi się zamienia" UX.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void neontext$clickHologram(CallbackInfoReturnable<Boolean> cir) {
        if (NeonClick.clickHologram()) {
            cir.setReturnValue(true);
        }
    }
}
