package dev.localcapes.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.localcapes.client.CapeGlow;
import dev.localcapes.client.CapeManager;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ElytraLayer.class)
public abstract class ElytraLayerMixin {
    @Shadow
    @Final
    private ElytraModel<LivingEntity> elytraModel;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/ElytraModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void localcapes$emissiveElytra(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof AbstractClientPlayer player) || !CapeGlow.active(player)) {
            return;
        }
        PlayerSkin skin = player.getSkin();
        ResourceLocation texture = skin.elytraTexture() != null ? skin.elytraTexture() : skin.capeTexture();
        if (texture == null || texture.equals(CapeManager.VANILLA_ELYTRA)) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(CapeGlow.overlay(texture));
        this.elytraModel.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
