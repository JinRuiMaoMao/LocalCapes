package dev.localcapes.mixin;

import dev.localcapes.client.CapeManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
    private void localcapes$cloak(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation cape = CapeManager.getCape((AbstractClientPlayer) (Object) this);
        if (cape != null) {
            cir.setReturnValue(cape);
        }
    }

    @Inject(method = "getElytraTextureLocation", at = @At("HEAD"), cancellable = true)
    private void localcapes$elytra(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation cape = CapeManager.getCape((AbstractClientPlayer) (Object) this);
        if (cape != null) {
            cir.setReturnValue(CapeManager.elytraTexture(cape));
        }
    }

    @Inject(method = "isElytraLoaded", at = @At("HEAD"), cancellable = true)
    private void localcapes$elytraLoaded(CallbackInfoReturnable<Boolean> cir) {
        if (CapeManager.hasCape((AbstractClientPlayer) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isCapeLoaded", at = @At("HEAD"), cancellable = true)
    private void localcapes$capeLoaded(CallbackInfoReturnable<Boolean> cir) {
        if (CapeManager.hasCape((AbstractClientPlayer) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
