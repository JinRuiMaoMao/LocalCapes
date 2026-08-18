package dev.localcapes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocalCapes {
    public static final String MOD_ID = "localcapes";
    public static final Logger LOGGER = LoggerFactory.getLogger("LocalCapes");

    public static void init() {
        LOGGER.info("LocalCapes initialized");
    }
}
