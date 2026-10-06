package dev.localcapes.mixin;

import dev.localcapes.client.CapeManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void localcapes$skin(CallbackInfoReturnable<PlayerSkin> cir) {
        ResourceLocation cape = CapeManager.getCape((AbstractClientPlayer) (Object) this);
        if (cape == null) {
            return;
        }
        PlayerSkin skin = cir.getReturnValue();
        cir.setReturnValue(new PlayerSkin(skin.texture(), skin.textureUrl(), cape, CapeManager.elytraTexture(cape), skin.model(), skin.secure()));
    }
}
