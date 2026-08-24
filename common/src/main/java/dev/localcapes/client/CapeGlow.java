package dev.localcapes.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.Nullable;

public final class CapeGlow {
    private CapeGlow() {
    }

    public static boolean active(@Nullable AbstractClientPlayer player) {
        return player != null && CapeManager.shouldNightGlow(player);
    }

    public static RenderType overlay(ResourceLocation texture) {
        return RenderType.entityTranslucentEmissive(texture);
    }

    static boolean isDarkEnough(Level level, AbstractClientPlayer player) {
        if (level.isNight()) {
            return true;
        }
        long dayTime = level.getDayTime() % 24000L;
        if (dayTime >= 12000L && dayTime < 23500L) {
            return true;
        }
        int block = level.getBrightness(LightLayer.BLOCK, player.blockPosition());
        int sky = level.getBrightness(LightLayer.SKY, player.blockPosition());
        return Math.max(block, sky) <= 7;
    }
}
