package dev.localcapes.neoforge;

import dev.localcapes.LocalCapes;
import dev.localcapes.client.LocalCapesClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = LocalCapes.MOD_ID, dist = Dist.CLIENT)
public final class LocalCapesNeoForge {
    public LocalCapesNeoForge() {
        LocalCapes.init();
        LocalCapesClient.init();
    }
}
