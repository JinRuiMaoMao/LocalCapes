package dev.localcapes.mixin;

import dev.localcapes.client.CapeSelectScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void localcapes$addButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.translatable("gui.localcapes.button"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CapeSelectScreen(this));
            }
        }).bounds(this.leftPos + this.imageWidth + 2, this.topPos, 48, 20).build());
    }
}
