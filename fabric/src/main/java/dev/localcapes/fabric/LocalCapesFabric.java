package dev.localcapes.fabric;

import dev.localcapes.LocalCapes;
import dev.localcapes.client.LocalCapesClient;
import net.fabricmc.api.ClientModInitializer;

public final class LocalCapesFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LocalCapes.init();
        LocalCapesClient.init();
    }
}
