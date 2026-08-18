package dev.localcapes.mixin;

import dev.localcapes.client.CapeManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;isModelPartShown(Lnet/minecraft/world/entity/player/PlayerModelPart;)Z"
            )
    )
    private boolean localcapes$alwaysShowLocalCape(AbstractClientPlayer player, PlayerModelPart part) {
        return player.isModelPartShown(part)
                || (part == PlayerModelPart.CAPE && CapeManager.hasCape(player));
    }
}
