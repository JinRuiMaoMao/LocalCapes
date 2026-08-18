package dev.localcapes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.Command;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.localcapes.LocalCapes;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;

public final class LocalCapesClient {
    private static KeyMapping openMenu;

    private LocalCapesClient() {
    }

    public static void init() {
        openMenu = new KeyMapping(
                "key.localcapes.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.localcapes"
        );
        KeyMappingRegistry.register(openMenu);
        ClientTickEvent.CLIENT_POST.register(client -> {
            while (openMenu.consumeClick()) {
                openScreen(client);
            }
        });
        ReloadListenerRegistry.register(
                PackType.CLIENT_RESOURCES,
                (ResourceManagerReloadListener) CapeManager::reload
        );
        ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommandRegistrationEvent.literal("localcapes")
                        .executes(ctx -> {
                            openScreen(Minecraft.getInstance());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(ClientCommandRegistrationEvent.literal("reload")
                                .executes(ctx -> {
                                    Minecraft minecraft = Minecraft.getInstance();
                                    int loaded = CapeManager.reload(minecraft.getResourceManager());
                                    ctx.getSource().arch$sendSuccess(
                                            () -> Component.literal("LocalCapes: loaded " + loaded + " cape(s)"),
                                            false
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(ClientCommandRegistrationEvent.literal("none")
                                .executes(ctx -> {
                                    CapeManager.unequip();
                                    ctx.getSource().arch$sendSuccess(
                                            () -> Component.translatable("gui.localcapes.none"),
                                            false
                                    );
                                    return Command.SINGLE_SUCCESS;
                                }))
        ));
        LocalCapes.LOGGER.info("LocalCapes client ready. Press H or open inventory to pick a cape.");
    }

    public static void openScreen(Minecraft client) {
        if (client == null) {
            return;
        }
        client.setScreen(new CapeSelectScreen(client.screen));
    }
}
