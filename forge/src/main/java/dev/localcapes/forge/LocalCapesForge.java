package dev.localcapes.forge;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.localcapes.LocalCapes;
import dev.localcapes.client.LocalCapesClient;
import net.minecraftforge.fml.common.Mod;

@Mod(LocalCapes.MOD_ID)
public final class LocalCapesForge {
    public LocalCapesForge() {
        LocalCapes.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> LocalCapesClient::init);
    }
}
